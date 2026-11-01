package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreamReportTest {

    private static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }

    /* Звіт лабораторної № 3 для контрольного файлу є початком нового звіту без змін. */
    @Test
    void newReportStartsWithTheLab3ReportForTheControlFile() throws IOException {
        String expected = normalize(Files.readString(Path.of("data", "lab03-expected-report.txt"),
                StandardCharsets.UTF_8));
        Analysis analysis = PlantAnalyzer.analyze(
                Files.readAllLines(Path.of("data", "input.csv"), StandardCharsets.UTF_8));

        assertTrue(normalize(ReportFormatter.format(analysis)).startsWith(expected));
    }

    /* Потокова секція показує результати запитів для набору із шести і більше записів. */
    @Test
    void streamSectionShowsAllQueryResults() throws IOException {
        Analysis analysis = PlantAnalyzer.analyze(
                Files.readAllLines(Path.of("data", "lab04-input.csv"), StandardCharsets.UTF_8));

        String report = normalize(ReportFormatter.format(analysis));

        assertTrue(report.contains("Рослин із поливом не рідше ніж раз на 2 дн.: 3\n"));
        assertTrue(report.contains("Види: [Квіти, Листяні, Орхідеї, Сукуленти, Хвойні]\n"));
        assertTrue(report.contains("Середня ціна, категорія «дерево»: 816.67 грн\n"));
        assertTrue(report.contains("Середня ціна, категорія «квітка»: 152.50 грн\n"));
        assertTrue(report.contains("Висота: мін. 30.00 см, макс. 300.00 см, середня 123.64 см\n"));
        String top = String.join("; ", "Хвойні Ялина (800.00 грн)", "Листяні Клен (1200.00 грн)",
                "Хвойні Туя (450.00 грн)", "Квіти Троянда (150.00 грн)", "Сукуленти Алое (70.00 грн)");
        assertTrue(report.contains("Найвищі рослини (до 5): %s\n".formatted(top)));
    }

    /* Порожній набір: статистика та топ пояснюються явно, без Infinity. */
    @Test
    void emptyInputIsReportedExplicitly() {
        String report = normalize(ReportFormatter.format(PlantAnalyzer.analyze(List.of())));

        assertTrue(report.contains("Висота: немає даних\n"));
        assertTrue(report.contains("Найвищі рослини (до 5): немає даних\n"));
        assertTrue(report.contains("Види: []\n"));
        assertTrue(!report.contains("Infinity"));
    }
}
