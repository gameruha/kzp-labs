# Лабораторна робота № 1: розсадник рослин

Консольна Java-програма для варіанта 12 з курсу «Кросплатформні засоби програмування».
Програма читає записи розсадника рослин, відкидає некоректні рядки з поясненням,
обчислює показники та формує однаковий звіт у консоль і файл.

## Формат входу

Файл за замовчуванням: `data/input.csv`. Кодування UTF-8, роздільник полів `;`.
Поля запису:

```text
species;name;heightCm;price;wateringDays
```

`heightCm` і `price` є десятковими числами з крапкою. `heightCm` та `wateringDays`
мають бути більшими за нуль, `price` не може бути від'ємним. Порожні текстові поля,
неправильна кількість полів, неправильні числа та порожні рядки відкидаються із
зазначенням номера рядка.

## Показники варіанта 12

1. Кількість коректних записів.
2. Середня висота рослин.
3. Найдорожча рослина.
4. Найменший інтервал поливу.

## Збірка і запуск

Windows:

```text
.\mvnw.cmd clean verify package
java -jar target\lab01-1.0.0.jar
```

macOS/Linux:

```text
./mvnw clean verify package
java -jar target/lab01-1.0.0.jar
```

Доступні режими:

```text
java -jar target/lab01-1.0.0.jar --help
java -jar target/lab01-1.0.0.jar --version
java -jar target/lab01-1.0.0.jar --input data/input.csv --output out/report.txt
```

За замовчуванням звіт записується в `out/report.txt`. Для дробових чисел використовується
`Locale.ROOT`, а для файлів явно задається UTF-8.

## Перевірки

```text
.\mvnw.cmd test
.\mvnw.cmd verify
```

`verify` запускає JUnit 5 та SpotBugs. Maven Shade Plugin створює виконуваний JAR
із головним класом `ua.lpnu.kzp.Main`. GitHub Actions перевіряє `verify` і `package`
на Ubuntu, Windows та macOS і завантажує JAR як артефакт.