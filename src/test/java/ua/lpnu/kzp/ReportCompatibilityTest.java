package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReportCompatibilityTest {

    private static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }

    /* Звіт лаби 2 для контрольного файлу має бути початком нового звіту без змін. */
    @Test
    void newReportStartsWithTheLab2ReportForTheControlFile() throws IOException {
        String expected = normalize(Files.readString(Path.of("data", "lab02-expected-report.txt"),
                StandardCharsets.UTF_8));
        Analysis analysis = PlantAnalyzer.analyze(
                Files.readAllLines(Path.of("data", "input.csv"), StandardCharsets.UTF_8));

        String actual = normalize(ReportFormatter.format(analysis));

        assertTrue(actual.startsWith(expected));
    }

    /* Нові показники рахуються поліморфно та через enum. */
    @Test
    void newIndicatorsAreAppendedAfterTheOldOnes() throws IOException {
        Analysis analysis = PlantAnalyzer.analyze(
                Files.readAllLines(Path.of("data", "input.csv"), StandardCharsets.UTF_8));

        String report = normalize(ReportFormatter.format(analysis));

        assertTrue(report.contains("Категорія «дерево»: 2"));
        assertTrue(report.contains("Категорія «квітка»: 1"));
        assertTrue(report.contains("Сумарна потреба в догляді: "));
        assertTrue(report.contains("Унікальних рослин: 3"));
        assertEquals(3, analysis.plants().size());
    }
}
