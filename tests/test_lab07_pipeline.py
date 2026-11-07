"""Тести завантажувача, контекстних менеджерів, еквівалентності звіту та CLI (ЛР №7)."""

from pathlib import Path

import pytest

from kzp_lab06.analysis import make_summary as make_lab06_summary
from kzp_lab06.loader import load_records as load_lab06_records
from kzp_lab06.reporting import format_report as format_lab06_report
from kzp_lab07.analysis import (
    average_care_score,
    care_summary,
    compare_dict_vs_object_memory,
    frequent_watering,
    highest_care_plant,
    make_summary,
    tallest_plant,
    total_care_score,
)
from kzp_lab07.cli import main
from kzp_lab07.errors import DataError
from kzp_lab07.loader import SafeCsvReader, build_plant, load_plants
from kzp_lab07.models import Flower, Tree
from kzp_lab07.reporting import SafeReportWriter, format_report, write_report


def test_loader_creates_both_subtypes_and_preserves_duplicates() -> None:
    """Перевіряє створення Tree і Flower, дубліката для рівності та граничних значень із CSV."""
    csv_path = Path("data/lab07-input.csv")
    plants = load_plants(csv_path)

    assert len(plants) == 8
    assert any(isinstance(plant, Tree) for plant in plants)
    assert any(isinstance(plant, Flower) for plant in plants)

    # Рядки 4 та 5 у data/lab07-input.csv є однаковими об'єктами Flower.
    assert plants[3] == plants[4]
    assert isinstance(plants[3], Flower)
    # Рядок 6 має граничні значення age=1 та price=0.0.
    assert plants[5].age == 1
    assert plants[5].price == 0.0


def test_loader_supports_explicit_kind_and_rejects_invalid_rows(tmp_path: Path) -> None:
    """Перевіряє фабрику build_plant з явним полем kind і повідомлення номера рядка помилки."""
    tree = build_plant(
        {
            "species": " Рідкісна сосна ",
            "age": "5",
            "height": "150.0",
            "price": "600.0",
            "watering_days": "4",
            "kind": "tree",
        },
        line_number=2,
    )
    flower = build_plant(
        {
            "species": "Лаванда",
            "age": "2",
            "height": "40.0",
            "price": "180.0",
            "watering_days": "2",
            "kind": "квітка",
        },
        line_number=3,
    )
    assert isinstance(tree, Tree)
    assert isinstance(flower, Flower)

    invalid_csv = tmp_path / "invalid.csv"
    invalid_csv.write_text(
        "species,age,height,price,watering_days\nХвойні,4,120.5,-10.0,7\n",
        encoding="utf-8",
    )
    with pytest.raises(DataError, match="Рядок 2"):
        load_plants(invalid_csv)

    unknown_kind_csv = tmp_path / "unknown_kind.csv"
    unknown_kind_csv.write_text(
        "species,age,height,price,watering_days,kind\nМох,1,5.0,20.0,1,algae\n",
        encoding="utf-8",
    )
    with pytest.raises(DataError, match="Рядок 2: невідомий підтип рослини"):
        load_plants(unknown_kind_csv)


def test_empty_input_and_missing_columns(tmp_path: Path) -> None:
    """Перевіряє поведінку на порожньому CSV (лише заголовок) та при відсутності стовпців."""
    empty_csv = tmp_path / "empty.csv"
    empty_csv.write_text("species,age,height,price,watering_days\n", encoding="utf-8")

    plants = load_plants(empty_csv)
    assert plants == []
    assert highest_care_plant(plants) is None
    assert tallest_plant(plants) is None
    assert total_care_score(plants) == 0.0
    assert average_care_score(plants) == 0.0
    assert care_summary(plants).count == 0

    summary = make_summary(plants)
    report = format_report(summary, include_care=True)
    assert "Кількість записів: 0" in report
    assert "Найвища рослина: немає даних" in report
    assert "Найбільша потреба в догляді: немає даних" in report

    bad_headers = tmp_path / "bad_headers.csv"
    bad_headers.write_text("species,height,price\nХвойні,100,200\n", encoding="utf-8")
    with pytest.raises(DataError, match="Відсутні обов'язкові стовпці"):
        load_plants(bad_headers)


def test_old_and_new_reports_are_equivalent_on_same_csv() -> None:
    """Доводить повну сумісність зовнішнього звіту ЛР №6 (словники) і ЛР №7 (об'єкти)."""
    for csv_file in (Path("data/lab06-input.csv"), Path("data/lab07-input.csv")):
        old_records = load_lab06_records(csv_file)
        old_summary = make_lab06_summary(
            old_records,
            title="Звіт розсадника рослин (варіант 12)",
        )
        old_report = format_lab06_report(old_summary)

        new_plants = load_plants(csv_file)
        new_summary = make_summary(new_plants)
        new_report = format_report(new_summary)
        new_report_with_care = format_report(new_summary, include_care=True)

        assert new_report == old_report
        assert new_report_with_care.startswith(old_report)
        assert "Потреба в догляді (середня):" in new_report_with_care


def test_context_managers_close_files_on_exception(tmp_path: Path) -> None:
    """Перевіряє закриття SafeReportWriter і SafeCsvReader під час винятку."""
    report_path = tmp_path / "out" / "report.txt"
    writer_manager = SafeReportWriter(report_path)

    with pytest.raises(RuntimeError, match="штучний збій запису"):
        with writer_manager as writer:
            assert writer_manager.closed is False
            writer.write("Частина звіту\n")
            raise RuntimeError("штучний збій запису")

    assert writer_manager.closed is True

    csv_path = tmp_path / "sample.csv"
    csv_path.write_text(
        "species,age,height,price,watering_days\nХвойні,4,120.5,450.0,7\n",
        encoding="utf-8",
    )
    reader_manager = SafeCsvReader(csv_path)

    with pytest.raises(RuntimeError, match="штучний збій читання"):
        with reader_manager as reader:
            assert reader_manager.closed is False
            _ = next(reader)
            raise RuntimeError("штучний збій читання")

    assert reader_manager.closed is True


def test_memory_comparison_and_cli_scenarios(
    tmp_path: Path, capsys: pytest.CaptureFixture[str]
) -> None:
    """Перевіряє порівняння пам'яті моделей та успішний/помилковий сценарії CLI."""
    stats = compare_dict_vs_object_memory(Path("data/lab07-input.csv"))
    assert stats["records"] == 8
    assert stats["dict_peak_bytes"] > 0
    assert stats["object_peak_bytes"] > 0
    assert stats["dict_shallow_bytes"] > 0
    assert stats["object_shallow_bytes"] > 0

    output_file = tmp_path / "report.txt"
    exit_code = main(
        [
            "--input",
            "data/lab07-input.csv",
            "--output",
            str(output_file),
            "--include-care",
        ]
    )
    captured = capsys.readouterr()
    assert exit_code == 0
    assert "Звіт записано до" in captured.out
    text = output_file.read_text(encoding="utf-8")
    assert "Кількість записів: 8" in text
    assert "Потреба в догляді (сумарна):" in text

    missing_code = main(
        [
            "--input",
            str(tmp_path / "missing.csv"),
            "--output",
            str(output_file),
        ]
    )
    err_captured = capsys.readouterr()
    assert missing_code == 2
    assert "Помилка даних:" in err_captured.err

    # Додаткова перевірка write_report та frequent_watering на помилковий аргумент.
    write_report(output_file, "Тест\n")
    assert output_file.read_text(encoding="utf-8") == "Тест\n"
    with pytest.raises(DataError):
        frequent_watering([], max_days=0)
