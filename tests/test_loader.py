"""Тести читання та перевірки CSV-записів розсадника рослин."""

from pathlib import Path

import pytest

from kzp_nursery.errors import DataError
from kzp_nursery.loader import iter_records, load_records


def test_load_records_reads_valid_csv_with_quoted_comma() -> None:
    """Перевіряє читання контрольного CSV варіанта 12 із комою в лапках."""
    records = load_records(Path("data/input.csv"))

    assert len(records) == 7
    assert records[0] == {
        "species": "Хвойні",
        "age": 4,
        "height": 120.5,
        "price": 450.0,
        "watering_days": 7,
    }
    assert records[-1]["species"] == "Хвойні, декоративні"


def test_iter_records_supports_ukrainian_header_aliases(tmp_path: Path) -> None:
    """Перевіряє підтримку україномовних заголовків (вид,вік,висота,ціна,полив)."""
    csv_file = tmp_path / "ukr.csv"
    csv_file.write_text(
        "вид,вік,висота,ціна,полив\nХвойні,4,120.5,450.0,7\nКвіти,2,60.0,150.0,2\n",
        encoding="utf-8",
    )

    records = list(iter_records(csv_file))
    assert len(records) == 2
    assert records[0]["species"] == "Хвойні"
    assert records[1]["watering_days"] == 2


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


def test_load_records_reports_line_number_field_and_cause(tmp_path: Path) -> None:
    """Перевіряє номер рядка, назву поля та збереження першопричини (__cause__)."""
    broken = tmp_path / "broken.csv"
    broken.write_text(
        "species,age,height,price,watering_days\nХвойні,чотири,120.5,450.0,7\n",
        encoding="utf-8",
    )

    with pytest.raises(DataError, match=r"Рядок 2.*age") as exc_info:
        load_records(broken)
    assert isinstance(exc_info.value.__cause__, ValueError)


@pytest.mark.parametrize(
    ("row_text", "expected_match"),
    [
        ("   ,4,120.5,450.0,7", "species"),
        ("Хвойні,0,120.5,450.0,7", "age"),
        ("Хвойні,4,0,450.0,7", "height"),
        ("Хвойні,4,nan,450.0,7", "height"),
        ("Хвойні,4,120.5,-1.0,7", "price"),
        ("Хвойні,4,120.5,450.0,0", "watering_days"),
    ],
)
def test_load_records_rejects_invalid_domain_values(
    tmp_path: Path, row_text: str, expected_match: str
) -> None:
    """Перевіряє відхилення граничних невалідних значень полів."""
    csv_file = tmp_path / "invalid.csv"
    csv_file.write_text(
        f"species,age,height,price,watering_days\n{row_text}\n",
        encoding="utf-8",
    )
    with pytest.raises(DataError, match=expected_match):
        load_records(csv_file)
