from pathlib import Path

import pytest

from kzp_lab06.cli import main


def test_cli_writes_report_and_returns_zero(
    tmp_path: Path, capsys: pytest.CaptureFixture[str]
) -> None:
    output_file = tmp_path / "report.txt"

    exit_code = main(["--input", "data/lab06-input.csv", "--output", str(output_file)])

    captured = capsys.readouterr()
    assert exit_code == 0
    assert "Звіт записано до" in captured.out
    report_text = output_file.read_text(encoding="utf-8")
    assert "Кількість записів: 7" in report_text
    assert "Частий полив (<= 2 дні): 3" in report_text
    assert "Найвища рослина: Хвойні (300.00 см, вік 8 р., 800.00 грн)" in report_text


def test_cli_returns_two_and_writes_stderr_on_data_or_file_error(
    tmp_path: Path, capsys: pytest.CaptureFixture[str]
) -> None:
    bad_csv = tmp_path / "bad.csv"
    bad_csv.write_text(
        "species,age,height,price,watering_days\nХвойні,помилка,120.0,450.0,7\n",
        encoding="utf-8",
    )
    output_file = tmp_path / "report.txt"

    exit_code = main(["--input", str(bad_csv), "--output", str(output_file)])

    captured = capsys.readouterr()
    assert exit_code == 2
    assert "Помилка даних:" in captured.err
    assert "Traceback" not in captured.err

    missing_code = main(["--input", str(tmp_path / "missing.csv"), "--output", str(output_file)])
    assert missing_code == 2
