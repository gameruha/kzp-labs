from pathlib import Path

from kzp_lab06.analysis import make_summary
from kzp_lab06.reporting import format_report, write_report


def test_format_report_produces_deterministic_sorted_lines() -> None:
    records = [
        {
            "species": "Хвойні",
            "age": 8,
            "height": 300.0,
            "price": 800.0,
            "watering_days": 2,
        },
        {
            "species": "Квіти",
            "age": 2,
            "height": 60.0,
            "price": 150.0,
            "watering_days": 1,
        },
    ]
    summary = make_summary(records, title="Звіт розсадника рослин (варіант 12)")

    report = format_report(summary)

    assert "Звіт розсадника рослин (варіант 12)\n" in report
    assert "Кількість записів: 2\n" in report
    assert "Частий полив (<= 2 дні): 2\n" in report
    assert "Середня ціна: 475.00 грн\n" in report
    assert "Найвища рослина: Хвойні (300.00 см, вік 8 р., 800.00 грн)\n" in report
    assert "Види: Квіти, Хвойні\n" in report
    assert "  Квіти: 150.00 грн\n  Хвойні: 800.00 грн\n" in report


def test_format_report_handles_empty_records_and_writes_utf8_file(
    tmp_path: Path,
) -> None:
    empty_summary = make_summary([])
    empty_report = format_report(empty_summary)
    assert "Найвища рослина: немає даних\n" in empty_report

    output_path = tmp_path / "nested" / "report.txt"
    write_report(output_path, empty_report)

    assert output_path.read_text(encoding="utf-8") == empty_report
