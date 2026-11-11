# Розсадник рослин (варіант 12) — Python-проєкт (ЛР № 6–7)

Консольний Python-пакет `kzp-nursery` (`src/kzp_nursery/`) для обробки даних розсадника рослин
(варіант 12) з курсу «Кросплатформні засоби програмування».

- **Гілка `main-python`** містить виключно Python-проєкт (Java-проєкт розміщено окремо на гілці
  `main-java`).
- **Лабораторна робота № 6** (тег `v6.0.0`, гілка `release/lab06`) реалізує початкову словникову
  обробку CSV-даних у пакеті `src/kzp_nursery/`.
- **Лабораторна робота № 7** (тег `v7.0.0`, гілка `release/lab07`) розвиває цей самий пакет
  `src/kzp_nursery/` до об'єктно-орієнтованої моделі з `@property`, `@dataclass(frozen=True)`,
  підтипами `Tree` і `Flower`, спільним протоколом `CareScored(Protocol)` і власними контекстними
  менеджерами `SafeCsvReader` та `SafeReportWriter`.

## Формат вхідного CSV (`data/input.csv`)

Кодування UTF-8, роздільник `,`:

```csv
species,age,height,price,watering_days
Хвойні,4,120.5,450.0,7
Хвойні,8,300.0,800.0,2
Листяні,10,250.0,1200.0,14
Квіти,2,60.0,150.0,1
Квіти,1,30.0,90.0,3
Сукуленти,3,60.0,70.0,2
"Хвойні, декоративні",6,180.0,950.0,5
```

Також підтримуються україномовні заголовки (`вид,вік,висота,ціна,полив`) та необов'язковий стовпець
підтипу `kind` / `тип` (`tree`/`flower`, `дерево`/`квітка`).

## Структура пакета `src/kzp_nursery/`

- `__init__.py` — публічний API пакета.
- `errors.py` — предметний виняток `DataError(ValueError)`.
- `models.py` — протокол `CareScored(Protocol)` із властивостями й методом `care_score()`, базовий
  клас `Plant` (`__init__`, `@property` + setter-валідація, `needs_frequent_watering`, `__str__`,
  `__repr__`, `__eq__`, `__lt__`), підтипи `Tree(Plant)` і `Flower(Plant)` з різними формулами
  `care_score()`, а також незмінні `@dataclass(frozen=True)` `CareSummary` та `ReportSummary`.
- `loader.py` — перетворення CSV-рядків на об'єкти `Tree` і `Flower` (`build_plant`, `iter_records`,
  `load_records`) із повідомленням номера рядка у `DataError` та власний контекстний менеджер
  `SafeCsvReader`.
- `analysis.py` — поліморфні функції аналізу через протокол `CareScored` без перевірок конкретного
  підтипу (`highest_care_plant`, `total_care_score`, `average_care_score`, `sort_by_care_score`,
  `care_summary`, `frequent_watering`, `unique_species`, `average_price_by_species`,
  `tallest_plant`, `average_price`, `make_summary`, `streaming_summary`,
  `compare_dict_vs_object_memory`).
- `reporting.py` — детерміноване форматування звіту `format_report(summary, include_care=...)`,
  повністю сумісне із звітом лабораторної № 6, та безпечний запис у UTF-8 через власний контекстний
  менеджер `SafeReportWriter`.
- `cli.py` — консольні скрипти `kzp-nursery`, `kzp-report` та `kzp-oop-report` (`--input`,
  `--output`, `--max-watering-days`, `--include-care`).

## Команди встановлення, перевірки та запуску

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[dev]"
python -m pytest
ruff check .
ruff format --check .
python -m build --wheel
kzp-nursery --input data/input.csv --output reports/report.txt
kzp-oop-report --input data/input.csv --output reports/report-care.txt --include-care
```
