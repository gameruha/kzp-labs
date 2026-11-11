"""Тести інтерфейсу командного рядка (CLI)."""

from pathlib import Path

import pytest

from kzp_nursery.cli import main


def test_cli_success_and_error_exit_codes(
    tmp_path: Path, capsys: pytest.CaptureFixture[str]
) -> None:
    """Перевіряє код завершення 0 для коректного CSV та код 2 для помилки даних або файлу."""
    output_file = tmp_path / "report.txt"
    code = main(["--input", "data/input.csv", "--output", str(output_file), "--include-care"])
    captured = capsys.readouterr()

    assert code == 0
    assert output_file.exists()
    assert "Звіт записано до" in captured.out
    text = output_file.read_text(encoding="utf-8")
    assert "Кількість записів: 7" in text
    assert "Потреба в догляді (сумарна): 767.68 бала" in text

    missing_code = main(["--input", str(tmp_path / "missing.csv"), "--output", str(output_file)])
    err_captured = capsys.readouterr()
    assert missing_code == 2
    assert "Помилка даних:" in err_captured.err
