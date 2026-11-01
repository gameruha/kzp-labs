package ua.lpnu.kzp;

import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        appendStreamSection(report, analysis.plants());
        appendStorageSection(report, analysis.plants());
        return report.toString();
    }

    /** Adds the lab 3 indicators; every plant is processed through the shared {@link Plant} type. */
    private static void appendPolymorphicSection(StringBuilder report, List<Plant> plants) {
        Stream.of(PlantKind.values()).forEach(kind -> appendLine(report, "Категорія «%s»: %d",
                kind.label(), plants.stream().filter(plant -> plant.kind() == kind).count()));
        double totalCareNeed = plants.stream().mapToDouble(Plant::careNeed).sum();
        appendLine(report, "Сумарна потреба в догляді: %.2f", totalCareNeed);
        appendLine(report, "Унікальних рослин: %d", new HashSet<>(plants).size());
    }

    /** Adds the lab 4 section; all values come from {@link PlantStreamQueries}. */
    private static void appendStreamSection(StringBuilder report, List<Plant> plants) {
        appendLine(report, "Рослин із поливом не рідше ніж раз на %d дн.: %d",
                PlantStreamQueries.FREQUENT_WATERING_DAYS,
                PlantStreamQueries.frequentlyWatered(plants).size());
        appendLine(report, "Види: %s", PlantStreamQueries.speciesNames(plants).stream()
                .collect(Collectors.joining(", ", "[", "]")));
        PlantStreamQueries.averagePriceByKind(plants).forEach((kind, average) ->
                appendLine(report, "Середня ціна, категорія «%s»: %.2f грн", kind.label(), average));
        DoubleSummaryStatistics height = PlantStreamQueries.heightStatistics(plants);
        // Для порожнього потоку min і max дорівнюють нескінченностям, тому випадок обробляється явно.
        Optional.of(height).filter(statistics -> statistics.getCount() > 0).ifPresentOrElse(
                statistics -> appendLine(report, "Висота: мін. %.2f см, макс. %.2f см, середня %.2f см",
                        statistics.getMin(), statistics.getMax(), statistics.getAverage()),
                () -> appendLine(report, "Висота: немає даних"));
        String top = PlantStreamQueries.topByHeight(plants, PlantStreamQueries.TOP_LIMIT).stream()
                .map(Plant::toString)
                .collect(Collectors.joining("; "));
        appendLine(report, "Найвищі рослини (до %d): %s", PlantStreamQueries.TOP_LIMIT,
                top.isEmpty() ? "немає даних" : top);
    }

    /** Adds the lab 5 storage and reflective CSV metadata section. */
    private static void appendStorageSection(StringBuilder report, List<Plant> plants) {
        Repository<PlantRecord> repository = new Repository<>();
        for (int index = 0; index < plants.size(); index++) {
            repository.add(PlantRecord.fromPlant(plants.get(index), index + 1));
        }
        appendLine(report, "Записів у Repository<PlantRecord>: %d", repository.all().size());
        try {
            appendLine(report, "CSV-заголовок PlantRecord: %s",
                    CsvExporter.headerFor(PlantRecord.class));
        } catch (DataStorageException exception) {
            throw new IllegalStateException("Не вдалося побудувати CSV-заголовок", exception);
        }
    }

    private static void appendLine(StringBuilder report, String format, Object... arguments) {
        report.append(String.format(Locale.ROOT, format, arguments)).append(System.lineSeparator());
    }
}
