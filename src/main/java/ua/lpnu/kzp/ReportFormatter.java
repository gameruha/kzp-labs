package ua.lpnu.kzp;

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
        return report.toString();
    }
}
