"""Тести функцій аналізу даних розсадника рослин (варіант 12)."""

from pathlib import Path

import pytest

from kzp_nursery.analysis import (
    average_price,
    average_price_by_species,
    compare_memory_usage,
    frequent_watering,
    make_summary,
    streaming_summary,
    tallest_plant,
    unique_species,
)
from kzp_nursery.errors import DataError
from kzp_nursery.loader import iter_records, load_records


def test_frequent_watering_filter_boundary_and_immutability() -> None:
    """Перевіряє межу відбору watering_days <= 2 та незмінність вхідного списку."""
    records = load_records(Path("data/input.csv"))
    original_copy = list(records)

    selected = frequent_watering(records, max_days=2)
    assert len(selected) == 3
    assert [r["watering_days"] for r in selected] == [2, 1, 2]
    assert records == original_copy

    with pytest.raises(DataError):
        frequent_watering(records, max_days=0)


def test_unique_species_and_average_price_by_species() -> None:
    """Перевіряє множину видів на дублікатах і словник середньої ціни за видом."""
    records = load_records(Path("data/input.csv"))

    species = unique_species(records)
    assert species == {"Хвойні", "Листяні", "Квіти", "Сукуленти", "Хвойні, декоративні"}

    averages = average_price_by_species(records)
    assert list(averages.keys()) == sorted(averages.keys())
    assert averages["Хвойні"] == pytest.approx(625.0)
    assert averages["Квіти"] == pytest.approx(120.0)
    assert averages["Листяні"] == pytest.approx(1200.0)


def test_tallest_plant_and_average_price_including_empty() -> None:
    """Перевіряє найвищу рослину та середню ціну на непорожньому й порожньому входах."""
    records = load_records(Path("data/input.csv"))

    tallest = tallest_plant(records)
    assert tallest is not None
    assert tallest["species"] == "Хвойні"
    assert tallest["height"] == pytest.approx(300.0)
    assert average_price(records) == pytest.approx(530.0)

    assert tallest_plant([]) is None
    assert average_price([]) == 0.0
    assert unique_species([]) == set()
    assert average_price_by_species([]) == {}


def test_make_summary_supports_default_args_and_kwargs() -> None:
    """Перевіряє *metrics, параметр за замовчуванням max_watering_days і **labels."""
    records = load_records(Path("data/input.csv"))

    subset = make_summary(
        records,
        "count",
        "average_price",
        max_watering_days=1,
        title="Короткий звіт",
        owner="КІ-302",
    )
    assert subset["count"] == 7
    assert subset["average_price"] == pytest.approx(530.0)
    assert "species" not in subset
    assert subset["labels"] == {"title": "Короткий звіт", "owner": "КІ-302"}

    with pytest.raises(DataError, match="Невідомі метрики"):
        make_summary(records, "unknown_metric")


def test_streaming_summary_and_memory_comparison() -> None:
    """Перевіряє потоковий підсумок через генератор + itertools та вимірювання пам'яті."""
    csv_path = Path("data/input.csv")
    records = load_records(csv_path)

    expected = make_summary(records, max_watering_days=2, title="Потоковий звіт")
    actual = streaming_summary(iter_records(csv_path), max_watering_days=2, title="Потоковий звіт")

    assert actual["count"] == expected["count"]
    assert actual["frequent_watering_count"] == expected["frequent_watering_count"]
    assert actual["species"] == expected["species"]
    assert actual["average_price_by_species"] == pytest.approx(expected["average_price_by_species"])
    assert actual["tallest_plant"] == expected["tallest_plant"]
    assert actual["average_price"] == pytest.approx(expected["average_price"])

    mem = compare_memory_usage(csv_path)
    assert mem["records"] == 7
    assert mem["frequent_count"] == 3
    assert mem["list_peak_bytes"] > 0
    assert mem["stream_peak_bytes"] > 0
