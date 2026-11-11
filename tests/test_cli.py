"""Тести інтерфейсу командного рядка (CLI)."""

from pathlib import Path

import pytest

from kzp_nursery.cli import main


def test_cli_success_and_error_exit_codes(
    tmp_path: Path, capsys: pytest.CaptureFixture[str]
) -> None:
    """Перевіряє код завершення 0 для коректного CSV та код 2 для помилки даних або файлу."""
    output_file = tmp_path / "report.txt"
    code = main(["--input", "data/input.csv", "--output", str(output_file)])
    captured = capsys.readouterr()

    assert code == 0
    assert output_file.exists()
    assert "Звіт записано до" in captured.out

    missing_code = main(["--input", str(tmp_path / "missing.csv"), "--output", str(output_file)])
    err_captured = capsys.readouterr()
    assert missing_code == 2
    assert "Помилка даних:" in err_captured.err
