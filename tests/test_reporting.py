"""Тести форматування звіту, сумісності зі старим звітом та SafeReportWriter (ЛР №7)."""

from pathlib import Path

import pytest

from kzp_nursery.analysis import make_dict_summary, make_summary
from kzp_nursery.loader import load_dict_records, load_records
from kzp_nursery.reporting import SafeReportWriter, format_report, write_report


def test_old_and_new_reports_are_equivalent_on_same_csv(tmp_path: Path) -> None:
    """Доводить повну сумісність старого словникового (ЛР №6) і нового об'єктного (ЛР №7) звітів."""
    csv_path = Path("data/input.csv")

    old_summary = make_dict_summary(load_dict_records(csv_path))
    old_report = format_report(old_summary)

    new_summary = make_summary(load_records(csv_path))
    new_report = format_report(new_summary)
    new_report_with_care = format_report(new_summary, include_care=True)

    assert new_report == old_report
    assert new_report_with_care.startswith(old_report)
    assert "Потреба в догляді (сумарна): 767.68 бала" in new_report_with_care
    assert "Потреба в догляді (середня): 109.67 бала" in new_report_with_care
    assert "Найбільша потреба в догляді: Хвойні (181.00 бала)" in new_report_with_care

    output_file = tmp_path / "reports" / "report.txt"
    write_report(output_file, new_report)
    assert output_file.read_text(encoding="utf-8") == old_report


def test_format_report_empty_data_and_safe_writer_closes_on_exception(tmp_path: Path) -> None:
    """Перевіряє порожній звіт і закриття SafeReportWriter під час винятку."""
    summary = make_summary([])
    report = format_report(summary, include_care=True)

    assert "Кількість записів: 0" in report
    assert "Найвища рослина: немає даних" in report
    assert "Найбільша потреба в догляді: немає даних" in report

    report_path = tmp_path / "out" / "report.txt"
    writer_manager = SafeReportWriter(report_path)

    with pytest.raises(RuntimeError, match="штучний збій запису"):
        with writer_manager as writer:
            assert writer_manager.closed is False
            writer.write("Частина звіту\n")
            raise RuntimeError("штучний збій запису")

    assert writer_manager.closed is True
