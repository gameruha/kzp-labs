# Лабораторні роботи № 1–7: розсадник рослин

Консольна Java- та Python-програма для варіанта 12 з курсу «Кросплатформні засоби програмування».
Програма читає записи розсадника рослин, відкидає некоректні рядки з поясненням,
обчислює показники та формує однаковий звіт у консоль і файл.

## Формат входу

Вхідний файл передається обов'язковим аргументом `--input`. Прикладовий файл:
`data/input.csv`. Кодування UTF-8, роздільник полів `;`. Поля запису:

```text
species;name;heightCm;price;wateringDays
```

`heightCm` і `price` є скінченними десятковими числами з крапкою. `heightCm` та
`wateringDays` мають бути більшими за нуль, `price` не може бути від'ємним. Порожні
текстові поля, неправильна кількість полів, некоректні, нескінченні та нечислові
значення, а також порожні рядки відкидаються із зазначенням номера рядка.

## Показники варіанта 12

1. Кількість коректних записів.
2. Середня висота рослин.
3. Найдорожча рослина.
4. Найменший інтервал поливу.

## Модель після лабораторної роботи № 2

`Plant` — незмінна сутність із приватними полями та перевіркою інваріантів у конструкторі.
Фабрика `Plant.fromCsv` розбирає п'ять CSV-полів; вона відхиляє порожні назви,
неправильний числовий формат, нескінченні значення, висоту й інтервал поливу не більше
нуля, а також від'ємну ціну. `PlantSummary` — record із кількістю записів, середньою
висотою, найдорожчою рослиною та найменшим інтервалом поливу. CLI й формат звіту
збережені сумісними з лабораторною роботою № 1.

## Модель після лабораторної роботи № 3

`Plant` став `sealed abstract` базовим типом: у ньому лежать спільні поля, перевірка
інваріантів, `equals`/`hashCode`, `toString` і абстрактний метод `careNeed()` (потреба в
догляді). Дозволені підтипи: `Tree` (дерево) та `Flower` (квітка). Кожен підтип має власні
правила (`Tree`: висота до 10 000 см, полив не рідше ніж раз на 60 днів; `Flower`: висота до
200 см, полив не рідше ніж раз на 30 днів) і власну формулу `careNeed()`. Enum `PlantKind`
(`TREE`, `FLOWER`) має підпис `label()` для звіту; категорія визначається за колонкою
`species` (хвойні, листяні, плодові, дерева — дерева, решта — квіти), тому формат CSV не змінився.
Звіт і далі містить усі показники лабораторної № 2 без змін; після них додано кількість
рослин за категоріями, сумарну потребу в догляді та кількість унікальних рослин.
`PlantPlacement` показує `switch` зі зіставленням зі зразком над запечатаною ієрархією.
Версія береться лише з `pom.xml`.

## Потокові запити після лабораторної роботи № 4

Усі запити зібрано в чистому класі `PlantStreamQueries` (без циклів, без розгалужень за
підтипом, без форматування виводу; вхідний список не змінюється):

| Запит | Метод | Результат |
|---|---|---|
| Відбір: полив не рідше ніж раз на 2 дні | `frequentlyWatered` | `List<Plant>` |
| Перетворення: види рослин | `speciesNames` | `List<String>` |
| Групування: середня ціна за `PlantKind` | `averagePriceByKind` | `Map<PlantKind, Double>` |
| Статистика висоти | `heightStatistics` | `DoubleSummaryStatistics` |
| Top-5: висота за спаданням, потім вид і назва | `topByHeight` | `List<Plant>` |
| Пошук за видом | `findBySpecies` | `Optional<Plant>` |
| Зірочка: категорія → вид → кількість | `countByKindAndSpecies` | `Map<PlantKind, Map<String, Long>>` |

`ReportFormatter` додає до звіту секцію з результатами запитів; усі рядки звіту лабораторних
№ 2 і № 3 залишаються без змін на початку. У моделі `Plant` (`Tree` і `Flower`) наявне поле
`ageYears` (`вік`, додатне ціле число років). Контрольний набір даних для запитів —
`data/lab04-input.csv` (7 записів формату `species;name;ageYears;heightCm;price;wateringDays`,
дубльований вид, межа фільтра, рівна висота).

## Параметризоване сховище, рефлексивний CSV-експорт і round-trip після лабораторної роботи № 5

- `Repository<T>` — типобезпечне сховище в пам'яті без raw types із методами `add(T)`,
  незмінним знімком `all()` (`List.copyOf`) та пошуком `find(Predicate<? super T>)`.
- `@CsvColumn` — анотація часу виконання (`@Retention(RUNTIME)`, `@Target(FIELD)`) із назвою
  стовпця `value()`.
- `PlantRecord` — клас запису варіанта 12 (`species: String`, `ageYears: int`,
  `heightCm: double`, `price: double`, `wateringDays: int`; стовпці `@CsvColumn`: `вид`, `вік`,
  `висота`, `ціна`, `полив`; складений ключ round-trip: `species + ageYears`). Порядок стовпців
  відповідає варіанту 12 (`species, ageYears, heightCm, price, wateringDays`):
  `вид,вік,висота,ціна,полив`. Анотацією `@CsvColumn` також позначено відповідні п'ять полів
  базового класу `Plant` (`Tree` і `Flower`).
- `Controller` зберігає зчитані записи у `Repository<PlantRecord>`, формує вихідний CSV-файл на
  базі збережених об'єктів через `CsvExporter.write` у порядку колонок варіанта 12
  (`вид,вік,висота,ціна,полив`) і виводить сформований CSV у стандартний потік виводу.
- `CsvExporter<T>` та `JsonExporter<T>` реалізують спільний інтерфейс `Exporter<T>` (завдання із
  зірочкою), обходять ланцюжок успадкування, кешують рефлексивні поля в `ConcurrentHashMap`,
  екранують коми, подвійні лапки й перенесення рядків і записують файли в `UTF-8`.
- `CsvParser` — скінченний автомат над повним текстом документа, який коректно розбирає поля в
  лапках із комами, подвоєними лапками `""` та багаторядковими значеннями.
- `DataStorageException` — власний checked-виняток зі збереженням першопричини (`getCause()`) для
  помилок формату, значення (`NumberFormatException`, `IllegalArgumentException`), файлової
  системи (`IOException`) та рефлексії.
- Контрольні файли: `data/lab05-records.csv` (5 записів, включно з комою й лапками),
  `data/lab05-empty.csv` (лише заголовок), `data/lab05-invalid.csv` (хибний рядок).

## Збірка і запуск

Потрібен JDK 21; для запуску JAR команда `java` має використовувати саме цю версію.

Windows:

```powershell
.\mvnw.cmd clean verify package
chcp 65001 > $null; & "$env:JAVA_HOME\bin\java.exe" '-Dfile.encoding=UTF-8' -jar target\lab01-5.0.0.jar --input data\input.csv
```

Команда запуску вище призначена для PowerShell і вмикає UTF-8, щоб український текст
правильно відображався в консолі Windows.

macOS/Linux:

```text
./mvnw clean verify package
java -jar target/lab01-5.0.0.jar --input data/input.csv
```

Доступні режими:

```text
java -jar target/lab01-5.0.0.jar --help
java -jar target/lab01-5.0.0.jar --version
java -jar target/lab01-5.0.0.jar --input data/input.csv --output out/report.txt
```

`--input <файл>` обов'язковий для звичайного запуску; `--output <файл>` необов'язковий.
За замовчуванням звіт записується в `out/report.txt`. Для дробових чисел використовується
`Locale.ROOT`, а для файлів явно задається UTF-8. Каталоги `target/` (зібрані Maven-файли)
та `out/` (згенерований звіт) ігноруються Git.

## Перевірки

```text
.\mvnw.cmd test
.\mvnw.cmd verify
```

`verify` запускає JUnit 5 та SpotBugs. Maven Shade Plugin створює виконуваний JAR
із головним класом `ua.lpnu.kzp.Main`. GitHub Actions перевіряє `verify` і `package`
на Ubuntu, Windows та macOS і завантажує JAR як артефакт.

## Обробка даних засобами Python після лабораторної роботи № 6

Пакет `kzp_lab06` (`src/kzp_lab06/`, конфігурація в `pyproject.toml`, версія `6.0.0`)
реалізує обробку даних розсадника рослин (варіант 12) у Python 3.11+:

- `errors.py` — предметний виняток `DataError(ValueError)`.
- `loader.py` — читання UTF-8 CSV через `csv.DictReader` (`load_records(path)`) та потоковий
  генератор `iter_records(path)` (завдання із зірочкою) із підтримкою як англомовних заголовків
  варіанта 12 (`species,age,height,price,watering_days`), так і україномовних заголовків із ЛР № 5
  (`вид,вік,висота,ціна,полив`), а також ланцюгом причин `raise DataError(...) from error`.
- `analysis.py` — функції варіанта 12 над `list`, `dict`, `set` із трьома компрехеншнами
  (`frequent_watering`, `unique_species`, `average_price_by_species`, `tallest_plant`,
  `average_price`), підсумкова функція `make_summary(records, *metrics, max_watering_days=2, **labels)`
  із параметром за замовчуванням, `*args` і `**kwargs`, а також потокова обробка `streaming_summary`
  через `itertools.tee` та порівняння пам'яті `compare_memory_usage` через `tracemalloc`.
- `reporting.py` — детерміноване форматування звіту `format_report(summary)` та запис у UTF-8
  через `with path.open("w", encoding="utf-8", newline="\n")` (`write_report`).
- `cli.py` — консольна точка входу `kzp-lab06` (`--input`, `--output`, `--max-watering-days`), яка
  повертає код `0` за успіху та `2` у разі `DataError` або `OSError` із повідомленням у `stderr`.

### Команди Python-пакета (Windows PowerShell)

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -e ".[dev]"
pytest
ruff check .
ruff format --check .
python -m build
kzp-lab06 --input data/lab06-input.csv --output reports/report.txt
```

### Команди Python-пакета (Linux / macOS)

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e ".[dev]"
pytest
ruff check .
ruff format --check .
python -m build
kzp-lab06 --input data/lab06-input.csv --output reports/report.txt
```

## Об'єктно-орієнтована обробка даних у Python після лабораторної роботи № 7

Пакет `kzp_lab07` (`src/kzp_lab07/`, конфігурація в `pyproject.toml`, версія `7.0.0`, точки входу
`kzp-lab07` та `kzp-oop-report`) перетворює словникові записи лабораторної № 6 на об'єкти з
інкапсульованим станом і поліморфною поведінкою:

- `models.py`:
  - `Plant` — базовий клас рослини розсадника з `__init__`, інкапсульованими полями `_species`,
    `_age`, `_height`, `_price`, `_watering_days`, властивостями `@property` і setter-валідацією,
    методом `needs_frequent_watering(max_days=2)`, контрактом `care_score()` та спеціальними
    методами `__str__`, `__repr__`, `__eq__`, `__lt__`;
  - `Tree(Plant)` і `Flower(Plant)` — підтипи з викликом `super().__init__(...)` та різними
    формулами спільної операції `care_score()` (для дерева — за висотою крони та віком, для квітки —
    за частотою поливу);
  - `CareScored(Protocol)` — спільний протокол (`@runtime_checkable`) із властивостями (`species`,
    `age`, `height`, `price`, `watering_days`) і методом `care_score() -> float`;
  - `CareSummary` та `ReportSummary` — незмінні `@dataclass(frozen=True)` для збереження підсумку
    потреби в догляді й повного підсумку звіту.
- `loader.py` — перетворення CSV-рядків безпосередньо на об'єкти `Tree` і `Flower` (`build_plant`,
  `iter_plants`, `load_plants`) із повідомленням номера рядка у `DataError`, а також власний
  контекстний менеджер `SafeCsvReader` (завдання із зірочкою).
- `analysis.py` — поліморфні функції аналізу (`highest_care_plant`, `total_care_score`,
  `average_care_score`, `sort_by_care_score`, `care_summary`, `frequent_watering`,
  `unique_species`, `average_price_by_species`, `tallest_plant`, `average_price`, `make_summary`,
  `compare_dict_vs_object_memory`), що працюють через спільний протокол `CareScored` без перевірок
  конкретного підтипу (`isinstance`).
- `reporting.py` — детерміноване форматування звіту `format_report(summary, include_care=...)`,
  повністю сумісне із зовнішнім звітом лабораторної № 6, та безпечний запис у UTF-8 через власний
  контекстний менеджер `SafeReportWriter` (завдання із зірочкою).
- `cli.py` — консольний інтерфейс `kzp-lab07` / `kzp-oop-report` (`--input`, `--output`,
  `--max-watering-days`, `--include-care`) із захистом `if __name__ == "__main__": raise SystemExit(main())`.

### Команди для лабораторної роботи № 7

```powershell
python -m pytest
ruff check .
ruff format --check .
python -m build --wheel
kzp-lab07 --input data/lab07-input.csv --output reports/lab07-report.txt --include-care
```

