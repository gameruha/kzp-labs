"""Інтерфейс командного рядка для об'єктно-орієнтованої обробки даних розсадника рослин."""

import argparse
from pathlib import Path
import sys

from kzp_lab07.analysis import make_summary
from kzp_lab07.errors import DataError
from kzp_lab07.loader import load_plants
from kzp_lab07.reporting import format_report, write_report


def build_parser() -> argparse.ArgumentParser:
    """Створює парсер аргументів командного рядка для ЛР №7."""
    parser = argparse.ArgumentParser(
        prog="kzp-lab07",
        description="Об'єктно-орієнтована обробка даних розсадника рослин (варіант 12).",
    )
    parser.add_argument("--input", required=True, type=Path, help="Шлях до вхідного CSV-файлу.")
    parser.add_argument("--output", required=True, type=Path, help="Шлях до вихідного звіту.")
    parser.add_argument(
        "--max-watering-days",
        type=int,
        default=2,
        help="Максимальний інтервал поливу в днях для відбору (за замовчуванням: 2).",
    )
    parser.add_argument(
        "--include-care",
        action="store_true",
        help="Додати до звіту поліморфні показники потреби в догляді (care_score).",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    """Запускає CLI та повертає код завершення (0 — успіх, 2 — помилка даних або файлу)."""
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8")

    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        plants = load_plants(args.input)
        summary = make_summary(plants, max_watering_days=args.max_watering_days)
        report = format_report(summary, include_care=args.include_care)
        write_report(args.output, report)
    except (DataError, OSError) as error:
        print(f"Помилка даних: {error}", file=sys.stderr)
        return 2

    print(f"Звіт записано до {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
