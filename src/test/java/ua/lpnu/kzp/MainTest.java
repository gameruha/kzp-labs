package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void parsesValidUkrainianRecordsAndCalculatesVariantMetrics() {
        Main.Analysis analysis = Main.analyze(List.of(
                "species;name;heightCm;price;wateringDays",
                "Хвойні;Туя;120.5;450.0;7",
                "Листяні;Клен;250.0;1200.0;14"));

        assertEquals(2, analysis.plants().size());
        String report = Main.formatReport(analysis);
        assertTrue(report.contains("Середня висота: 185.25 см"));
        assertTrue(report.contains("Листяні Клен (1200.00 грн)"));
        assertTrue(report.contains("Найменший інтервал поливу: 7 днів"));
    }

    @Test
    void reportsLineNumbersForInvalidRowsAndKeepsValidRows() {
        Main.Analysis analysis = Main.analyze(List.of(
                "species;name;heightCm;price;wateringDays",
                "Квіти;Троянда;60.0;150.0;5",
                "Сукуленти;;15.0;80.0;3",
                "Декоративні;Барбарис;текст;300.0;10",
                "неправильний;рядок"));

        assertEquals(1, analysis.plants().size());
        assertEquals(3, analysis.errors().size());
        assertTrue(analysis.errors().get(0).startsWith("Рядок 3:"));
        assertTrue(analysis.errors().get(1).startsWith("Рядок 4:"));
        assertTrue(analysis.errors().get(2).startsWith("Рядок 5:"));
    }

    @Test
    void handlesEmptyInputWithoutDivisionByZero() {
        Main.Analysis analysis = Main.analyze(List.of("species;name;heightCm;price;wateringDays", ""));

        String report = Main.formatReport(analysis);
        assertTrue(report.contains("Коректних записів: 0"));
        assertTrue(report.contains("немає жодного коректного запису"));
    }

    @Test
    void preservesAnEmptyLastFieldDuringValidation() {
        Main.Analysis analysis = Main.analyze(List.of("A;B;10.0;20.0;"));

        assertEquals(1, analysis.errors().size());
        assertTrue(analysis.errors().get(0).contains("Рядок 1"));
    }
}
