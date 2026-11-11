"""Читання CSV та перетворення рядків на об'єкти підтипів Plant (варіант 12)."""

from collections.abc import Iterator, Mapping
import csv
from pathlib import Path
from typing import Literal, TextIO

from kzp_nursery.errors import DataError
from kzp_nursery.models import Flower, Plant, Tree

REQUIRED_FIELDS = ("species", "age", "height", "price", "watering_days")
FIELD_ALIASES = {
    "вид": "species",
    "вік": "age",
    "висота": "height",
    "ціна": "price",
    "полив": "watering_days",
    "тип": "kind",
}

FLOWER_KEYWORDS = (
    "квіт",
    "сукулент",
    "троянд",
    "орхіде",
    "тюльпан",
    "лаванд",
    "трав",
    "flower",
)
TREE_KEYWORDS = (
    "хвойн",
    "листян",
    "дерев",
    "сосн",
    "дуб",
    "ялин",
    "клен",
    "берез",
    "туя",
    "tree",
)


class SafeCsvReader:
    """Власний контекстний менеджер для безпечного читання CSV-файлу у кодуванні UTF-8."""

    def __init__(self, path: Path) -> None:
        """Зберігає шлях до CSV-файлу та ініціалізує стан дескриптора."""
        self.path = path
        self._file: TextIO | None = None

    @property
    def closed(self) -> bool:
        """Повертає True, якщо файловий потік ще не відкритий або вже закритий."""
        return self._file is None or self._file.closed

    def __enter__(self) -> csv.DictReader:
        """Відкриває CSV-файл у UTF-8, перевіряє заголовок і повертає DictReader."""
        self._file = self.path.open("r", encoding="utf-8", newline="")
        try:
            reader = csv.DictReader(self._file)
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
            return reader
        except Exception:
            self._file.close()
            raise

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_val: BaseException | None,
        exc_tb: object | None,
    ) -> Literal[False]:
        """Гарантовано закриває файловий потік навіть у разі виникнення винятку."""
        if self._file is not None and not self._file.closed:
            self._file.close()
        return False


def load_records(path: Path) -> list[Plant]:
    """Читає CSV-файл у UTF-8 і повертає список перевірених об'єктів підтипів Plant."""
    return list(iter_records(path))


def iter_records(path: Path) -> Iterator[Plant]:
    """Потоково читає CSV-файл і перетворює кожен рядок на об'єкт Tree або Flower."""
    with SafeCsvReader(path) as reader:
        for index, row in enumerate(reader, start=2):
            yield build_plant(row, line_number=index)


def load_dict_records(path: Path) -> list[dict[str, object]]:
    """Читає CSV у словникову модель (ЛР №6) для порівняння пам'яті та еквівалентності звіту."""
    return [
        {
            "species": plant.species,
            "age": plant.age,
            "height": plant.height,
            "price": plant.price,
            "watering_days": plant.watering_days,
        }
        for plant in iter_records(path)
    ]


def build_plant(row: Mapping[str, str | None], line_number: int = 1) -> Plant:
    """Створює екземпляр Tree або Flower з одного рядка CSV."""
    species = (row.get("species") or "").strip()
    if not species:
        raise DataError(f"Рядок {line_number}: порожнє поле species")

    age = _parse_int(row.get("age"), line_number, "age")
    height = _parse_float(row.get("height"), line_number, "height")
    price = _parse_float(row.get("price"), line_number, "price")
    watering_days = _parse_int(row.get("watering_days"), line_number, "watering_days")
    kind = (row.get("kind") or "").strip().lower()

    cls = _resolve_subtype(species, kind, line_number)
    try:
        return cls(
            species=species,
            age=age,
            height=height,
            price=price,
            watering_days=watering_days,
        )
    except ValueError as error:
        raise DataError(f"Рядок {line_number}: {error}") from error


def _resolve_subtype(species: str, kind: str, line_number: int) -> type[Plant]:
    """Визначає конкретний підтип (Tree або Flower) на межі завантаження даних."""
    if kind:
        if kind in ("tree", "дерево"):
            return Tree
        if kind in ("flower", "квітка"):
            return Flower
        raise DataError(f"Рядок {line_number}: невідомий підтип рослини: {kind!r}")

    lowered = species.lower()
    if any(keyword in lowered for keyword in FLOWER_KEYWORDS):
        return Flower
    if any(keyword in lowered for keyword in TREE_KEYWORDS):
        return Tree
    return Tree


def _parse_int(raw_value: str | None, line_number: int, field: str) -> int:
    text = (raw_value or "").strip()
    try:
        return int(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {line_number}: некоректне ціле число у полі {field}: {raw_value!r}"
        ) from error


def _parse_float(raw_value: str | None, line_number: int, field: str) -> float:
    text = (raw_value or "").strip()
    try:
        return float(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {line_number}: некоректне число у полі {field}: {raw_value!r}"
        ) from error


load_plants = load_records
iter_plants = iter_records
