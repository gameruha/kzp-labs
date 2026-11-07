from pathlib import Path

import pytest

from kzp_lab06.analysis import (
    average_price,
    average_price_by_species,
    compare_memory_usage,
    frequent_watering,
    make_summary,
    streaming_summary,
    tallest_plant,
    unique_species,
)
from kzp_lab06.errors import DataError
from kzp_lab06.loader import iter_records, load_records


def sample_records() -> list[dict[str, object]]:
    return [
        {
            "species": "Хвойні",
            "age": 4,
            "height": 120.5,
            "price": 450.0,
            "watering_days": 7,
        },
        {
            "species": "Хвойні",
            "age": 8,
            "height": 300.0,
            "price": 850.0,
            "watering_days": 2,
        },
        {
            "species": "Квіти",
            "age": 2,
            "height": 60.0,
            "price": 150.0,
            "watering_days": 1,
        },
        {
            "species": "Квіти",
            "age": 1,
            "height": 30.0,
            "price": 90.0,
            "watering_days": 3,
        },
    ]


def test_frequent_watering_includes_boundary_two_days() -> None:
    records = sample_records()

    filtered = frequent_watering(records)

    assert [item["age"] for item in filtered] == [8, 2]
    assert len(records) == 4
    with pytest.raises(DataError, match="додатним"):
        frequent_watering(records, max_days=0)


def test_unique_species_and_average_price_by_species_handle_duplicates() -> None:
    records = sample_records()

    assert unique_species(records) == {"Хвойні", "Квіти"}
    averages = average_price_by_species(records)
    assert list(averages.keys()) == ["Квіти", "Хвойні"]
    assert averages["Квіти"] == pytest.approx(120.0)
    assert averages["Хвойні"] == pytest.approx(650.0)


def test_tallest_plant_and_average_price_handle_regular_and_empty_inputs() -> None:
    records = sample_records()

    tallest = tallest_plant(records)
    assert tallest is not None
    assert tallest["height"] == pytest.approx(300.0)
    assert average_price(records) == pytest.approx(385.0)

    assert tallest_plant([]) is None
    assert average_price([]) == pytest.approx(0.0)


def test_make_summary_supports_args_default_and_kwargs() -> None:
    records = sample_records()

    summary = make_summary(
        records,
        "count",
        "species",
        "tallest_plant",
        max_watering_days=2,
        title="Власний заголовок",
        owner="КІ-302",
    )

    assert summary["count"] == 4
    assert summary["species"] == {"Хвойні", "Квіти"}
    assert summary["tallest_plant"]["height"] == pytest.approx(300.0)
    assert summary["labels"] == {"title": "Власний заголовок", "owner": "КІ-302"}
    assert "average_price" not in summary

    with pytest.raises(DataError, match="Невідомі метрики"):
        make_summary(records, "unknown_metric")


def test_streaming_summary_matches_list_summary_and_measures_memory() -> None:
    csv_path = Path("data/lab06-input.csv")
    records = load_records(csv_path)

    expected = make_summary(records, max_watering_days=2, title="Потоковий звіт")
    actual = streaming_summary(iter_records(csv_path), max_watering_days=2, title="Потоковий звіт")

    assert actual["count"] == expected["count"]
    assert actual["frequent_watering_count"] == expected["frequent_watering_count"]
    assert actual["species"] == expected["species"]
    assert actual["average_price_by_species"] == pytest.approx(expected["average_price_by_species"])
    assert actual["tallest_plant"] == expected["tallest_plant"]
    assert actual["average_price"] == pytest.approx(expected["average_price"])

    memory = compare_memory_usage(csv_path, max_watering_days=2)
    assert memory["records"] == 7
    assert memory["frequent_count"] == 3
    assert memory["list_peak_bytes"] > 0
    assert memory["stream_peak_bytes"] > 0
