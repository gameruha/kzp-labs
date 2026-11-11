"""Тести завантажувача об'єктів Plant та контекстного менеджера SafeCsvReader (ЛР №7)."""

from pathlib import Path

import pytest

from kzp_nursery.errors import DataError
from kzp_nursery.loader import SafeCsvReader, build_plant, iter_records, load_records
from kzp_nursery.models import Flower, Tree


def test_load_records_reads_valid_csv_with_both_subtypes_and_quoted_comma() -> None:
    """Перевіряє читання контрольного CSV у підтипи Tree та Flower."""
    plants = load_records(Path("data/input.csv"))

    assert len(plants) == 7
    assert any(isinstance(plant, Tree) for plant in plants)
    assert any(isinstance(plant, Flower) for plant in plants)
    assert plants[0] == Tree(
        species="Хвойні",
        age=4,
        height=120.5,
        price=450.0,
        watering_days=7,
    )
    assert plants[-1].species == "Хвойні, декоративні"


def test_iter_records_supports_ukrainian_headers_and_duplicates(tmp_path: Path) -> None:
    """Перевіряє підтримку україномовних заголовків та рівність дубльованих об'єктів."""
    csv_file = tmp_path / "ukr.csv"
    csv_file.write_text(
        "вид,вік,висота,ціна,полив\nКвіти,2,60.0,150.0,1\nКвіти,2,60.0,150.0,1\n",
        encoding="utf-8",
    )

    plants = list(iter_records(csv_file))
    assert len(plants) == 2
    assert plants[0] == plants[1]
    assert isinstance(plants[0], Flower)


def test_build_plant_with_explicit_kind_and_unknown_kind(tmp_path: Path) -> None:
    """Перевіряє фабрику build_plant з явним полем kind та відхилення невідомого підтипу."""
    tree = build_plant(
        {
            "species": "Рідкісна сосна",
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
            "price": "0.0",
            "watering_days": "2",
            "kind": "квітка",
        },
        line_number=3,
    )
    assert isinstance(tree, Tree)
    assert isinstance(flower, Flower)
    assert flower.price == 0.0

    unknown_kind_csv = tmp_path / "unknown_kind.csv"
    unknown_kind_csv.write_text(
        "species,age,height,price,watering_days,kind\nМох,1,5.0,20.0,1,algae\n",
        encoding="utf-8",
    )
    with pytest.raises(DataError, match="Рядок 2: невідомий підтип рослини"):
        load_records(unknown_kind_csv)


def test_load_records_empty_and_missing_headers(tmp_path: Path) -> None:
    """Перевіряє порожній файл без заголовка, лише заголовок та відсутній стовпець."""
    completely_empty = tmp_path / "empty_file.csv"
    completely_empty.write_text("", encoding="utf-8")
    with pytest.raises(DataError, match="заголовка"):
        load_records(completely_empty)

    header_only = tmp_path / "header_only.csv"
    header_only.write_text("species,age,height,price,watering_days\n", encoding="utf-8")
    assert load_records(header_only) == []

    missing_col = tmp_path / "missing.csv"
    missing_col.write_text("species,age,height,price\nХвойні,4,120.5,450.0\n", encoding="utf-8")
    with pytest.raises(DataError, match="watering_days"):
        load_records(missing_col)


def test_load_records_reports_line_number_and_closes_reader_on_error(tmp_path: Path) -> None:
    """Перевіряє номер рядка, першопричину та закриття SafeCsvReader при винятку."""
    broken = tmp_path / "broken.csv"
    broken.write_text(
        "species,age,height,price,watering_days\nХвойні,чотири,120.5,450.0,7\n",
        encoding="utf-8",
    )

    with pytest.raises(DataError, match=r"Рядок 2.*age") as exc_info:
        load_records(broken)
    assert isinstance(exc_info.value.__cause__, ValueError)

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


@pytest.mark.parametrize(
    ("row_text", "expected_match"),
    [
        ("   ,4,120.5,450.0,7", "species"),
        ("Хвойні,0,120.5,450.0,7", "Вік"),
        ("Хвойні,4,0,450.0,7", "Висота"),
        ("Хвойні,4,nan,450.0,7", "Висота"),
        ("Хвойні,4,120.5,-1.0,7", "Ціна"),
        ("Хвойні,4,120.5,450.0,0", "Інтервал поливу"),
    ],
)
def test_load_records_rejects_invalid_domain_values(
    tmp_path: Path, row_text: str, expected_match: str
) -> None:
    """Перевіряє відхилення граничних невалідних значень полів через setter-и Plant."""
    csv_file = tmp_path / "invalid.csv"
    csv_file.write_text(
        f"species,age,height,price,watering_days\n{row_text}\n",
        encoding="utf-8",
    )
    with pytest.raises(DataError, match=expected_match):
        load_records(csv_file)
