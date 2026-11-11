"""Тести форматування та запису звіту розсадника рослин."""

from pathlib import Path

from kzp_nursery.analysis import make_summary
from kzp_nursery.loader import load_records
from kzp_nursery.reporting import format_report, write_report


def test_format_and_write_report_deterministic(tmp_path: Path) -> None:
    """Перевіряє детермінований формат звіту та запис у файл UTF-8."""
    records = load_records(Path("data/input.csv"))
    summary = make_summary(records, title="Звіт розсадника рослин (варіант 12)")
    report = format_report(summary)

    output_file = tmp_path / "reports" / "report.txt"
    write_report(output_file, report)

    saved = output_file.read_text(encoding="utf-8")
    assert saved == report
    assert "Кількість записів: 7" in saved
    assert "Частий полив (<= 2 дні): 3" in saved
    assert "Середня ціна: 530.00 грн" in saved
    assert "Найвища рослина: Хвойні (300.00 см, вік 8 р., 800.00 грн)" in saved


def test_format_report_empty_data() -> None:
    """Перевіряє форматування звіту для порожнього списку записів."""
    summary = make_summary([])
    report = format_report(summary)

    assert "Кількість записів: 0" in report
    assert "Найвища рослина: немає даних" in report
