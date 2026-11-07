"""Обчислення показників розсадника рослин (варіант 12)."""

import itertools
import tracemalloc
from collections.abc import Iterable
from pathlib import Path

from kzp_lab06.errors import DataError
from kzp_lab06.loader import iter_records, load_records

ALLOWED_METRICS = {
    "count",
    "frequent_watering",
    "species",
    "average_price_by_species",
    "tallest_plant",
    "average_price",
}


def frequent_watering(
    records: list[dict[str, object]], max_days: int = 2
) -> list[dict[str, object]]:
    """Відбирає рослини з періодичністю поливу не більшою за max_days днів."""
    if max_days <= 0:
        raise DataError("Максимальний інтервал поливу має бути додатним")
    # Відбір записів списковим включенням (list comprehension) без зміни вхідного списку.
    return [record for record in records if int(record["watering_days"]) <= max_days]


def unique_species(records: list[dict[str, object]]) -> set[str]:
    """Повертає множину унікальних видів рослин."""
    return {str(record["species"]) for record in records}


def average_price_by_species(records: list[dict[str, object]]) -> dict[str, float]:
    """Обчислює словник середньої ціни за видом рослини (species)."""
    grouped: dict[str, list[float]] = {}
    for record in records:
        species = str(record["species"])
        grouped.setdefault(species, []).append(float(record["price"]))

    # Словникове включення (dict comprehension) формує підсумкову карту у стабільному порядку.
    return {species: sum(prices) / len(prices) for species, prices in sorted(grouped.items())}


def tallest_plant(records: list[dict[str, object]]) -> dict[str, object] | None:
    """Повертає запис найвищої рослини або None для порожнього списку."""
    if not records:
        return None
    return max(
        records,
        key=lambda record: (float(record["height"]), str(record["species"])),
    )


def average_price(records: list[dict[str, object]]) -> float:
    """Обчислює середню ціну всіх рослин або 0.0 для порожнього списку."""
    if not records:
        return 0.0
    prices = [float(record["price"]) for record in records]
    return sum(prices) / len(prices)


def make_summary(
    records: list[dict[str, object]],
    *metrics: str,
    max_watering_days: int = 2,
    **labels: str,
) -> dict[str, object]:
    """Формує підсумковий словник метрик варіанта 12 із підтримкою *args та **kwargs."""
    requested = metrics or (
        "count",
        "frequent_watering",
        "species",
        "average_price_by_species",
        "tallest_plant",
        "average_price",
    )
    unknown = set(requested) - ALLOWED_METRICS
    if unknown:
        unknown_list = ", ".join(sorted(unknown))
        raise DataError(f"Невідомі метрики: {unknown_list}")

    frequent = frequent_watering(records, max_days=max_watering_days)
    summary: dict[str, object] = {"labels": dict(labels)}

    if "count" in requested:
        summary["count"] = len(records)
    if "frequent_watering" in requested:
        summary["frequent_watering"] = frequent
        summary["frequent_watering_count"] = len(frequent)
    if "species" in requested:
        summary["species"] = unique_species(records)
    if "average_price_by_species" in requested:
        summary["average_price_by_species"] = average_price_by_species(records)
    if "tallest_plant" in requested:
        summary["tallest_plant"] = tallest_plant(records)
    if "average_price" in requested:
        summary["average_price"] = average_price(records)
    return summary


def streaming_summary(
    records_iter: Iterable[dict[str, object]],
    max_watering_days: int = 2,
    **labels: str,
) -> dict[str, object]:
    """Потоково обчислює підсумок через генератор та itertools без повного списку в пам'яті."""
    if max_watering_days <= 0:
        raise DataError("Максимальний інтервал поливу має бути додатним")

    main_stream, filter_stream = itertools.tee(records_iter, 2)
    frequent_iter = (
        record for record in filter_stream if int(record["watering_days"]) <= max_watering_days
    )
    frequent_count = sum(1 for _ in frequent_iter)

    count = 0
    total_price = 0.0
    species_set: set[str] = set()
    sums_by_species: dict[str, float] = {}
    counts_by_species: dict[str, int] = {}
    tallest: dict[str, object] | None = None

    for record in main_stream:
        count += 1
        species = str(record["species"])
        price = float(record["price"])
        height = float(record["height"])

        total_price += price
        species_set.add(species)
        sums_by_species[species] = sums_by_species.get(species, 0.0) + price
        counts_by_species[species] = counts_by_species.get(species, 0) + 1

        if tallest is None or (height, species) > (
            float(tallest["height"]),
            str(tallest["species"]),
        ):
            tallest = record

    avg_by_species = {
        sp: sums_by_species[sp] / counts_by_species[sp] for sp in sorted(sums_by_species)
    }

    return {
        "labels": dict(labels),
        "count": count,
        "frequent_watering_count": frequent_count,
        "species": species_set,
        "average_price_by_species": avg_by_species,
        "tallest_plant": tallest,
        "average_price": total_price / count if count > 0 else 0.0,
    }


def compare_memory_usage(path: Path, max_watering_days: int = 2) -> dict[str, int]:
    """Порівнює пікове споживання пам'яті спискового та генераторного конвеєрів."""
    tracemalloc.start()
    records = load_records(path)
    make_summary(records, max_watering_days=max_watering_days)
    _, list_peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()

    tracemalloc.start()
    # Однопрохідний генератор без tee для мінімального споживання пам'яті.
    count = 0
    frequent_count = 0
    total_price = 0.0
    for record in iter_records(path):
        count += 1
        total_price += float(record["price"])
        if int(record["watering_days"]) <= max_watering_days:
            frequent_count += 1
    _, stream_peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()

    return {
        "records": count,
        "frequent_count": frequent_count,
        "list_peak_bytes": list_peak,
        "stream_peak_bytes": stream_peak,
    }
