from pathlib import Path

import pytest

from kzp_lab06.errors import DataError
from kzp_lab06.loader import iter_records, load_records


def test_load_records_reads_utf8_csv_with_quoted_comma(tmp_path: Path) -> None:
    csv_file = tmp_path / "plants.csv"
    csv_file.write_text(
        "species,age,height,price,watering_days\n"
        "Хвойні,4,120.5,450.0,7\n"
        '"Хвойні, декоративні",6,180.0,950.0,2\n',
        encoding="utf-8",
    )

    records = load_records(csv_file)

    assert records == [
        {
            "species": "Хвойні",
            "age": 4,
            "height": 120.5,
            "price": 450.0,
            "watering_days": 7,
        },
        {
            "species": "Хвойні, декоративні",
            "age": 6,
            "height": 180.0,
            "price": 950.0,
            "watering_days": 2,
        },
    ]
    assert list(iter_records(csv_file)) == records


def test_load_records_supports_lab05_ukrainian_headers() -> None:
    records = load_records(Path("data/lab05-records.csv"))

    assert len(records) == 5
    assert records[0]["species"] == "Хвойні"
    assert records[0]["age"] == 4
    assert records[4]["species"] == 'Хвойні, декоративні "Альпійські"'


def test_load_records_rejects_empty_file_and_missing_column(tmp_path: Path) -> None:
    empty_file = tmp_path / "empty.csv"
    empty_file.write_text("", encoding="utf-8")
    with pytest.raises(DataError, match="заголовка"):
        load_records(empty_file)

    missing_col = tmp_path / "missing.csv"
    missing_col.write_text(
        "species,age,height,price\nХвойні,4,120.5,450.0\n", encoding="utf-8"
    )
    with pytest.raises(DataError, match="watering_days"):
        load_records(missing_col)


def test_load_records_reports_line_field_and_preserves_cause(tmp_path: Path) -> None:
    bad_int = tmp_path / "bad_int.csv"
    bad_int.write_text(
        "species,age,height,price,watering_days\nХвойні,чотири,120.5,450.0,7\n",
        encoding="utf-8",
    )
    with pytest.raises(DataError, match="Рядок 2.*age") as exc_info:
        load_records(bad_int)
    assert isinstance(exc_info.value.__cause__, ValueError)

    bad_float = tmp_path / "bad_float.csv"
    bad_float.write_text(
        "species,age,height,price,watering_days\nХвойні,4,висока,450.0,7\n",
        encoding="utf-8",
    )
    with pytest.raises(DataError, match="Рядок 2.*height") as float_exc:
        load_records(bad_float)
    assert isinstance(float_exc.value.__cause__, ValueError)


@pytest.mark.parametrize(
    ("row", "expected_pattern"),
    [
        (",4,120.5,450.0,7", "Рядок 2.*species"),
        ("Хвойні,0,120.5,450.0,7", "Рядок 2.*age"),
        ("Хвойні,4,-10.0,450.0,7", "Рядок 2.*height"),
        ("Хвойні,4,120.5,-1.0,7", "Рядок 2.*price"),
        ("Хвойні,4,120.5,450.0,0", "Рядок 2.*watering_days"),
        ("Хвойні,4,inf,450.0,7", "Рядок 2.*height"),
    ],
)
def test_load_records_rejects_invalid_domain_values(
    tmp_path: Path, row: str, expected_pattern: str
) -> None:
    csv_file = tmp_path / "invalid.csv"
    csv_file.write_text(
        f"species,age,height,price,watering_days\n{row}\n",
        encoding="utf-8",
    )

    with pytest.raises(DataError, match=expected_pattern):
        load_records(csv_file)
