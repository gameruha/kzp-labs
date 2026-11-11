"""Тести поліморфного аналізу об'єктів Plant (варіант 12, ЛР №7)."""

from pathlib import Path

import pytest

from kzp_nursery.analysis import (
    average_care_score,
    average_price,
    average_price_by_species,
    care_summary,
    compare_dict_vs_object_memory,
    frequent_watering,
    highest_care_plant,
    make_dict_summary,
    make_summary,
    streaming_summary,
    tallest_plant,
    total_care_score,
    unique_species,
)
from kzp_nursery.errors import DataError
from kzp_nursery.loader import iter_records, load_dict_records, load_records


def test_frequent_watering_filter_boundary_and_immutability() -> None:
    """Перевіряє межу відбору watering_days <= 2 та незмінність вхідного списку."""
    plants = load_records(Path("data/input.csv"))
    original_copy = list(plants)

    selected = frequent_watering(plants, max_days=2)
    assert len(selected) == 3
    assert [p.watering_days for p in selected] == [2, 1, 2]
    assert plants == original_copy

    with pytest.raises(DataError):
        frequent_watering(plants, max_days=0)


def test_unique_species_and_average_price_by_species() -> None:
    """Перевіряє множину видів на дублікатах і словник середньої ціни за видом."""
    plants = load_records(Path("data/input.csv"))

    species = unique_species(plants)
    assert species == {"Хвойні", "Листяні", "Квіти", "Сукуленти", "Хвойні, декоративні"}

    averages = average_price_by_species(plants)
    assert list(averages.keys()) == sorted(averages.keys())
    assert averages["Хвойні"] == pytest.approx(625.0)
    assert averages["Квіти"] == pytest.approx(120.0)
    assert averages["Листяні"] == pytest.approx(1200.0)


def test_tallest_plant_care_metrics_and_empty_inputs() -> None:
    """Перевіряє найвищу рослину, середню ціну й care_score на даних і порожньому вході."""
    plants = load_records(Path("data/input.csv"))

    tallest = tallest_plant(plants)
    assert tallest is not None
    assert tallest.species == "Хвойні"
    assert tallest.height == pytest.approx(300.0)
    assert average_price(plants) == pytest.approx(530.0)
    assert total_care_score(plants) == pytest.approx(767.68)
    assert average_care_score(plants) == pytest.approx(109.67)

    assert tallest_plant([]) is None
    assert highest_care_plant([]) is None
    assert average_price([]) == 0.0
    assert total_care_score([]) == 0.0
    assert average_care_score([]) == 0.0
    assert care_summary([]).count == 0
    assert unique_species([]) == set()
    assert average_price_by_species([]) == {}


def test_streaming_summary_and_memory_comparison() -> None:
    """Перевіряє потоковий підсумок ReportSummary та порівняння пам'яті моделей."""
    csv_path = Path("data/input.csv")
    plants = load_records(csv_path)

    expected = make_summary(plants, max_watering_days=2, title="Потоковий звіт")
    actual = streaming_summary(iter_records(csv_path), max_watering_days=2, title="Потоковий звіт")

    assert actual == expected

    dict_summary = make_dict_summary(load_dict_records(csv_path), title="Потоковий звіт")
    assert dict_summary["count"] == expected.count
    assert dict_summary["frequent_watering_count"] == expected.frequent_watering_count

    mem = compare_dict_vs_object_memory(csv_path)
    assert mem["records"] == 7
    assert mem["dict_peak_bytes"] > 0
    assert mem["object_peak_bytes"] > 0
    assert mem["dict_shallow_bytes"] > 0
    assert mem["object_shallow_bytes"] > 0
