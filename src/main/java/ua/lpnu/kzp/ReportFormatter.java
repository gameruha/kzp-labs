package ua.lpnu.kzp;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Formats the analysis result as one report for both console and file output. */
public final class ReportFormatter {

    private ReportFormatter() {
    }

    /**
     * Formats one report.
     *
     * @param analysis calculated records and errors
     * @return formatted report
     */
    public static String format(Analysis analysis) {
        PlantSummary summary = PlantSummary.from(analysis.plants());
        String mostExpensive = summary.mostExpensive() == null
                ? "немає коректних записів"
                : summary.mostExpensive().toString();

        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "=== Звіт розсадника рослин (варіант 12) ===%n"));
        report.append(String.format(Locale.ROOT, "Коректних записів: %d%n", summary.count()));
        report.append(String.format(Locale.ROOT, "Середня висота: %.2f см%n", summary.averageHeightCm()));
        report.append(String.format(Locale.ROOT, "Найдорожча рослина: %s%n", mostExpensive));
        report.append(String.format(Locale.ROOT, "Найменший інтервал поливу: %d днів%n",
                summary.minimumWateringDays()));
        report.append(String.format(Locale.ROOT, "Помилок: %d%n", analysis.errors().size()));
        analysis.errors().forEach(error -> report.append(error).append(System.lineSeparator()));
        if (summary.count() == 0) {
            report.append("Помилка: немає жодного коректного запису").append(System.lineSeparator());
        }
        appendPolymorphicSection(report, analysis.plants());
        return report.toString();
    }

    /** Adds the lab 3 indicators; every plant is processed through the shared {@link Plant} type. */
    private static void appendPolymorphicSection(StringBuilder report, List<Plant> plants) {
        for (PlantKind kind : PlantKind.values()) {
            long count = plants.stream().filter(plant -> plant.kind() == kind).count();
            report.append(String.format(Locale.ROOT, "Категорія «%s»: %d%n", kind.label(), count));
        }
        double totalCareNeed = plants.stream().mapToDouble(Plant::careNeed).sum();
        report.append(String.format(Locale.ROOT, "Сумарна потреба в догляді: %.2f%n", totalCareNeed));
        report.append(String.format(Locale.ROOT, "Унікальних рослин: %d%n", new HashSet<>(plants).size()));
    }
}
