"""Форматування та запис звіту розсадника рослин (варіант 12)."""

from pathlib import Path


def format_report(summary: dict[str, object]) -> str:
    """Формує детермінований багаторядковий текстовий звіт."""
    labels = dict(summary.get("labels", {}))
    title = labels.get("title", "Звіт розсадника рослин (варіант 12)")
    lines = [title]

    if "count" in summary:
        lines.append(f"Кількість записів: {summary['count']}")

    if "frequent_watering_count" in summary:
        lines.append(f"Частий полив (<= 2 дні): {summary['frequent_watering_count']}")

    if "average_price" in summary:
        lines.append(f"Середня ціна: {float(summary['average_price']):.2f} грн")

    if "tallest_plant" in summary:
        tallest = summary["tallest_plant"]
        if isinstance(tallest, dict):
            lines.append(
                "Найвища рослина: "
                f"{tallest['species']} ({float(tallest['height']):.2f} см, "
                f"вік {tallest['age']} р., {float(tallest['price']):.2f} грн)"
            )
        else:
            lines.append("Найвища рослина: немає даних")

    if "species" in summary:
        species = sorted(str(item) for item in summary["species"])
        lines.append(f"Види: {', '.join(species)}")

    if "average_price_by_species" in summary:
        lines.append("Середня ціна за видом:")
        avg_map = dict(summary["average_price_by_species"])
        for species, avg_price in sorted(avg_map.items()):
            lines.append(f"  {species}: {float(avg_price):.2f} грн")

    return "\n".join(lines) + "\n"


def write_report(path: Path, text: str) -> None:
    """Записує текст звіту у UTF-8 з єдиним роздільником рядків LF."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as file:
        file.write(text)
