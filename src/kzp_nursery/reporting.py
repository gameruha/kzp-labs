"""Форматування та безпечний запис текстового звіту розсадника рослин (варіант 12)."""

from pathlib import Path
from typing import Literal, TextIO

from kzp_nursery.models import ReportSummary


class SafeReportWriter:
    """Власний контекстний менеджер для безпечного запису текстового звіту у UTF-8."""

    def __init__(self, path: Path) -> None:
        """Зберігає цільовий шлях і готує внутрішній стан файлового дескриптора."""
        self.path = path
        self._file: TextIO | None = None

    @property
    def closed(self) -> bool:
        """Повертає True, якщо файл ще не відкритий або вже закритий."""
        return self._file is None or self._file.closed

    def __enter__(self) -> "SafeReportWriter":
        """Створює батьківські каталоги та відкриває файл для запису у кодуванні UTF-8."""
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self._file = self.path.open("w", encoding="utf-8", newline="\n")
        return self

    def write(self, text: str) -> None:
        """Записує текст звіту у відкритий файловий потік."""
        if self._file is None or self._file.closed:
            raise ValueError("Контекстний менеджер SafeReportWriter не активний")
        self._file.write(text)

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc_val: BaseException | None,
        exc_tb: object | None,
    ) -> Literal[False]:
        """Гарантовано закриває файл навіть під час виникнення винятку."""
        if self._file is not None and not self._file.closed:
            self._file.close()
        return False


def format_report(summary: ReportSummary | dict[str, object], *, include_care: bool = False) -> str:
    """Формує детермінований текстовий звіт, сумісний із форматом попередньої роботи."""
    if isinstance(summary, dict):
        return _format_dict_report(summary)

    lines = [
        summary.title,
        f"Кількість записів: {summary.count}",
        f"Частий полив (<= 2 дні): {summary.frequent_watering_count}",
        f"Середня ціна: {summary.average_price:.2f} грн",
    ]

    if summary.tallest_plant is not None:
        lines.append(f"Найвища рослина: {summary.tallest_plant}")
    else:
        lines.append("Найвища рослина: немає даних")

    lines.append(f"Види: {', '.join(summary.species)}")
    lines.append("Середня ціна за видом:")
    for species, avg_price in summary.average_price_by_species:
        lines.append(f"  {species}: {avg_price:.2f} грн")

    if include_care:
        lines.append(f"Потреба в догляді (сумарна): {summary.care.total_care_score:.2f} бала")
        lines.append(f"Потреба в догляді (середня): {summary.care.average_care_score:.2f} бала")
        if summary.care.highest_care_species is not None:
            lines.append(
                "Найбільша потреба в догляді: "
                f"{summary.care.highest_care_species} ({summary.care.highest_care_score:.2f} бала)"
            )
        else:
            lines.append("Найбільша потреба в догляді: немає даних")

    return "\n".join(lines) + "\n"


def _format_dict_report(summary: dict[str, object]) -> str:
    """Форматує старий словниковий підсумок (ЛР №6) для прямого порівняння звітів."""
    labels = dict(summary.get("labels", {}))
    title = labels.get("title", "Звіт розсадника рослин (варіант 12)")
    lines = [
        title,
        f"Кількість записів: {summary['count']}",
        f"Частий полив (<= 2 дні): {summary['frequent_watering_count']}",
        f"Середня ціна: {float(summary['average_price']):.2f} грн",
    ]
    tallest = summary.get("tallest_plant")
    if isinstance(tallest, dict):
        lines.append(
            "Найвища рослина: "
            f"{tallest['species']} ({float(tallest['height']):.2f} см, "
            f"вік {tallest['age']} р., {float(tallest['price']):.2f} грн)"
        )
    else:
        lines.append("Найвища рослина: немає даних")

    species = sorted(str(item) for item in summary["species"])
    lines.append(f"Види: {', '.join(species)}")
    lines.append("Середня ціна за видом:")
    avg_map = dict(summary["average_price_by_species"])
    for sp, avg_price in sorted(avg_map.items()):
        lines.append(f"  {sp}: {float(avg_price):.2f} грн")
    return "\n".join(lines) + "\n"


def write_report(path: Path, text: str) -> None:
    """Записує текст звіту у UTF-8 через власний контекстний менеджер SafeReportWriter."""
    with SafeReportWriter(path) as writer:
        writer.write(text)
