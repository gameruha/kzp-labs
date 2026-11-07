"""Читання та перевірка CSV-записів розсадника рослин (варіант 12)."""

import csv
import math
from collections.abc import Iterator
from pathlib import Path

from kzp_lab06.errors import DataError

REQUIRED_FIELDS = ("species", "age", "height", "price", "watering_days")
FIELD_ALIASES = {
    "вид": "species",
    "вік": "age",
    "висота": "height",
    "ціна": "price",
    "полив": "watering_days",
}


def load_records(path: Path) -> list[dict[str, object]]:
    """Читає CSV-файл у UTF-8 і повертає перевірені записи розсадника рослин."""
    return list(iter_records(path))


def iter_records(path: Path) -> Iterator[dict[str, object]]:
    """Потоково читає та перевіряє CSV-записи без завантаження всього файлу у список."""
    with path.open("r", encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        if reader.fieldnames is None:
            raise DataError("CSV-файл не містить рядка заголовка")

        normalized_headers = [
            FIELD_ALIASES.get(name.strip(), name.strip()) for name in reader.fieldnames
        ]
        reader.fieldnames = normalized_headers
        missing = [field for field in REQUIRED_FIELDS if field not in normalized_headers]
        if missing:
            missing_list = ", ".join(missing)
            raise DataError(f"Відсутні обов'язкові стовпці: {missing_list}")

        for index, row in enumerate(reader, start=2):
            yield _parse_row(row, index)


def _parse_row(row: dict[str, str | None], line_number: int) -> dict[str, object]:
    """Перетворює та перевіряє один рядок CSV."""
    species = (row.get("species") or "").strip()
    if not species:
        raise DataError(f"Рядок {line_number}: порожнє поле species")

    age = _parse_positive_int(row.get("age"), line_number, "age")
    height = _parse_positive_float(row.get("height"), line_number, "height")
    price = _parse_non_negative_float(row.get("price"), line_number, "price")
    watering_days = _parse_positive_int(row.get("watering_days"), line_number, "watering_days")

    return {
        "species": species,
        "age": age,
        "height": height,
        "price": price,
        "watering_days": watering_days,
    }


def _parse_positive_int(raw_value: str | None, line_number: int, field: str) -> int:
    text = (raw_value or "").strip()
    try:
        value = int(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {line_number}: некоректне ціле число у полі {field}: {raw_value!r}"
        ) from error

    if value <= 0:
        raise DataError(f"Рядок {line_number}: поле {field} має бути додатним")
    return value


def _parse_positive_float(raw_value: str | None, line_number: int, field: str) -> float:
    value = _parse_float(raw_value, line_number, field)
    if value <= 0:
        raise DataError(f"Рядок {line_number}: поле {field} має бути додатним")
    return value


def _parse_non_negative_float(raw_value: str | None, line_number: int, field: str) -> float:
    value = _parse_float(raw_value, line_number, field)
    if value < 0:
        raise DataError(f"Рядок {line_number}: поле {field} не може бути від'ємним")
    return value


def _parse_float(raw_value: str | None, line_number: int, field: str) -> float:
    text = (raw_value or "").strip()
    try:
        value = float(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {line_number}: некоректне число у полі {field}: {raw_value!r}"
        ) from error

    if not math.isfinite(value):
        raise DataError(f"Рядок {line_number}: поле {field} має бути скінченним числом")
    return value
