"""Пакет обробки даних розсадника рослин (варіант 12)."""

__all__ = [
    "DataError",
    "average_price",
    "average_price_by_species",
    "compare_memory_usage",
    "format_report",
    "frequent_watering",
    "iter_records",
    "load_records",
    "make_summary",
    "streaming_summary",
    "tallest_plant",
    "unique_species",
    "write_report",
]

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
from kzp_nursery.reporting import format_report, write_report
