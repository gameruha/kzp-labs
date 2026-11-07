"""Поліморфний аналіз об'єктів рослин через спільний протокол без перевірок конкретного підтипу."""

from collections.abc import Sequence
from pathlib import Path
import sys
import tracemalloc

from kzp_lab06.loader import load_records as load_dict_records
from kzp_lab07.errors import DataError
from kzp_lab07.loader import load_plants
from kzp_lab07.models import CareScored, CareSummary, Plant, ReportSummary


def highest_care_plant(plants: Sequence[CareScored]) -> CareScored | None:
    """Повертає рослину з найбільшою оцінкою догляду care_score() або None для порожнього входу."""
    # Функція працює через протокол CareScored і не перевіряє конкретний клас (Tree чи Flower).
    return max(
        plants,
        key=lambda plant: (plant.care_score(), plant.height, plant.species),
        default=None,
    )


def total_care_score(plants: Sequence[CareScored]) -> float:
    """Обчислює сумарну потребу в догляді всіх рослин через спільний метод care_score()."""
    return round(sum(plant.care_score() for plant in plants), 2)


def average_care_score(plants: Sequence[CareScored]) -> float:
    """Обчислює середній показник потреби в догляді або 0.0 для порожнього списку."""
    if not plants:
        return 0.0
    return round(total_care_score(plants) / len(plants), 2)


def sort_by_care_score(plants: Sequence[CareScored]) -> list[CareScored]:
    """Впорядковує рослини за спаданням потреби в догляді та назвою виду."""
    return sorted(
        plants,
        key=lambda plant: (-plant.care_score(), plant.species, -plant.height),
    )


def care_summary(plants: Sequence[CareScored]) -> CareSummary:
    """Формує незмінний @dataclass CareSummary на основі поліморфного виклику care_score()."""
    if not plants:
        return CareSummary(
            count=0,
            total_care_score=0.0,
            average_care_score=0.0,
            highest_care_species=None,
            highest_care_score=0.0,
        )

    best = highest_care_plant(plants)
    total = total_care_score(plants)
    avg = round(total / len(plants), 2)
    return CareSummary(
        count=len(plants),
        total_care_score=total,
        average_care_score=avg,
        highest_care_species=best.species if best is not None else None,
        highest_care_score=best.care_score() if best is not None else 0.0,
    )


def frequent_watering(plants: Sequence[Plant], max_days: int = 2) -> list[Plant]:
    """Відбирає рослини, що потребують частого поливу, за допомогою методу об'єкта."""
    if isinstance(max_days, bool) or not isinstance(max_days, int) or max_days <= 0:
        raise DataError("Максимальний інтервал поливу має бути додатним")
    return [plant for plant in plants if plant.needs_frequent_watering(max_days)]


def unique_species(plants: Sequence[CareScored]) -> set[str]:
    """Повертає множину унікальних видів рослин."""
    return {plant.species for plant in plants}


def average_price_by_species(plants: Sequence[CareScored]) -> dict[str, float]:
    """Обчислює словник середньої ціни за видом рослини у стабільному порядку ключів."""
    grouped: dict[str, list[float]] = {}
    for plant in plants:
        grouped.setdefault(plant.species, []).append(plant.price)

    return {species: sum(prices) / len(prices) for species, prices in sorted(grouped.items())}


def tallest_plant(plants: Sequence[Plant]) -> Plant | None:
    """Повертає найвищу рослину або None для порожнього списку."""
    if not plants:
        return None
    return max(plants, key=lambda plant: (plant.height, plant.species))


def average_price(plants: Sequence[CareScored]) -> float:
    """Обчислює середню ціну всіх рослин або 0.0 для порожнього списку."""
    if not plants:
        return 0.0
    return sum(plant.price for plant in plants) / len(plants)


def make_summary(
    plants: Sequence[Plant],
    max_watering_days: int = 2,
    title: str = "Звіт розсадника рослин (варіант 12)",
) -> ReportSummary:
    """Формує незмінний підсумковий об'єкт ReportSummary для списку рослин."""
    frequent = frequent_watering(plants, max_days=max_watering_days)
    avg_by_species = average_price_by_species(plants)
    return ReportSummary(
        count=len(plants),
        frequent_watering_count=len(frequent),
        average_price=average_price(plants),
        tallest_plant=tallest_plant(plants),
        species=tuple(sorted(unique_species(plants))),
        average_price_by_species=tuple(avg_by_species.items()),
        care=care_summary(plants),
        title=title,
    )


def compare_dict_vs_object_memory(path: Path) -> dict[str, int]:
    """Порівнює споживання пам'яті словникової моделі (ЛР №6) та об'єктної моделі (ЛР №7)."""
    tracemalloc.start()
    dict_records = load_dict_records(path)
    _, dict_peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()

    tracemalloc.start()
    obj_records = load_plants(path)
    _, obj_peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()

    dict_shallow = sys.getsizeof(dict_records) + sum(sys.getsizeof(item) for item in dict_records)
    obj_shallow = sys.getsizeof(obj_records) + sum(
        sys.getsizeof(item) + sys.getsizeof(item.__dict__) for item in obj_records
    )

    return {
        "records": len(obj_records),
        "dict_peak_bytes": dict_peak,
        "object_peak_bytes": obj_peak,
        "dict_shallow_bytes": dict_shallow,
        "object_shallow_bytes": obj_shallow,
    }
