# Розсадник рослин (варіант 12) — Python-проєкт (ЛР № 6)

Консольний Python-пакет `kzp-nursery` (`src/kzp_nursery/`) для обробки даних розсадника рослин
(варіант 12) з курсу «Кросплатформні засоби програмування».

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

Також підтримуються україномовні заголовки (`вид,вік,висота,ціна,полив`).

## Структура пакета `src/kzp_nursery/`

- `errors.py` — предметний виняток `DataError(ValueError)`.
- `loader.py` — читання UTF-8 CSV через `csv.DictReader` (`load_records(path)`) та потоковий
  генератор `iter_records(path)` із ланцюгом причин `raise DataError(...) from error`.
- `analysis.py` — функції варіанта 12 над `list`, `dict`, `set` із трьома comprehensions
  (`frequent_watering`, `unique_species`, `average_price_by_species`, `tallest_plant`,
  `average_price`), підсумкова функція `make_summary(records, *metrics, max_watering_days=2, **labels)`
  із параметром за замовчуванням, `*args` і `**kwargs`, потоковий підсумок `streaming_summary`
  (`itertools.tee`) та вимірювання пам'яті `compare_memory_usage` (`tracemalloc`).
- `reporting.py` — детерміноване форматування звіту `format_report(summary)` та запис у UTF-8
  `write_report(path, text)` через `with path.open("w", encoding="utf-8", newline="\n")`.
- `cli.py` — консольна точка входу `kzp-nursery` / `kzp-report` (`--input`, `--output`,
  `--max-watering-days`).

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
```
