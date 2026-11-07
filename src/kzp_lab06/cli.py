"""Інтерфейс командного рядка для обробки даних розсадника рослин (варіант 12)."""

import argparse
import sys
from pathlib import Path

from kzp_lab06.analysis import make_summary
from kzp_lab06.errors import DataError
from kzp_lab06.loader import load_records
from kzp_lab06.reporting import format_report, write_report


def build_parser() -> argparse.ArgumentParser:
    """Створює парсер аргументів командного рядка."""
    parser = argparse.ArgumentParser(
        prog="kzp-lab06",
        description="Обробка даних розсадника рослин (варіант 12) та побудова звіту.",
    )
    parser.add_argument("--input", required=True, type=Path, help="Шлях до CSV-файлу.")
    parser.add_argument("--output", required=True, type=Path, help="Шлях до звіту.")
    parser.add_argument(
        "--max-watering-days",
        type=int,
        default=2,
        help="Максимальний інтервал поливу в днях для відбору (за замовчуванням: 2).",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    """Запускає CLI та повертає код завершення процесу (0 — успіх, 2 — помилка даних/файлу)."""
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8")

    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        records = load_records(args.input)
        summary = make_summary(
            records,
            max_watering_days=args.max_watering_days,
            title="Звіт розсадника рослин (варіант 12)",
        )
        report = format_report(summary)
        write_report(args.output, report)
    except (DataError, OSError) as error:
        print(f"Помилка даних: {error}", file=sys.stderr)
        return 2

    print(f"Звіт записано до {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
