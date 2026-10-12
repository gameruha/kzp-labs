package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Console application for processing plant nursery records, variant 12. */
public final class Main {

    private static final String VERSION = "2.0.0";
    private static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");
    private static final String HEADER = "species;name;heightCm;price;wateringDays";

    private Main() {
    }

    /**
     * Runs the application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            Arguments arguments = Arguments.parse(args);
            if (arguments.help()) {
                System.out.print(helpText());
                return 0;
            }
            if (arguments.version()) {
                System.out.println(VERSION);
                return 0;
            }
            Analysis analysis = analyze(Files.readAllLines(arguments.input(), StandardCharsets.UTF_8));
            String report = formatReport(analysis);
            System.out.print(report);
            writeReport(arguments.output(), report);
            return 0;
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка аргументів: %s".formatted(exception.getMessage()));
            System.err.print(helpText());
            return 2;
        } catch (IOException exception) {
            System.err.println("Помилка роботи з файлами: %s".formatted(exception.getMessage()));
            return 1;
        }
    }

    /**
     * Parses input lines and calculates variant 12 metrics.
     *
     * @param lines input lines in UTF-8
     * @return calculated metrics and validation messages
     */
    public static Analysis analyze(List<String> lines) {
        List<Plant> plants = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int lineNumber = index + 1;
            if (lineNumber == 1 && HEADER.equalsIgnoreCase(line.trim())) {
                continue;
            }
            if (line.isBlank()) {
                errors.add("Рядок %d: порожній рядок".formatted(lineNumber));
                continue;
            }
            try {
                plants.add(Plant.fromCsv(line));
            } catch (IllegalArgumentException exception) {
                errors.add("Рядок %d: %s".formatted(lineNumber, exception.getMessage()));
            }
        }
        return new Analysis(plants, errors);
    }

    /**
     * Formats one report for both console and file output.
     *
     * @param analysis calculated records and errors
     * @return formatted report
     */
    public static String formatReport(Analysis analysis) {
        PlantSummary summary = PlantSummary.from(analysis.plants());

        StringBuilder report = new StringBuilder();
        report.append("=== Звіт розсадника рослин (варіант 12) ===%n".formatted());
        report.append("Коректних записів: %d%n".formatted(summary.count()));
        report.append(String.format(Locale.ROOT, "Середня висота: %.2f см%n", summary.averageHeightCm()));
        report.append("Найдорожча рослина: %s%n".formatted(summary.mostExpensive() == null
                ? "немає коректних записів"
            : summary.mostExpensive()));
        report.append("Найменший інтервал поливу: %d днів%n".formatted(summary.minimumWateringDays()));
        report.append("Помилок: %d%n".formatted(analysis.errors().size()));
        analysis.errors().forEach(error -> report.append(error).append(System.lineSeparator()));
        if (summary.count() == 0) {
            report.append("Помилка: немає жодного коректного запису").append(System.lineSeparator());
        }
        return report.toString();
    }

    /**
     * Writes report text as UTF-8 and creates its parent directory.
     *
     * @param output destination path
     * @param report report text
     * @throws IOException if the destination cannot be written
     */
    public static void writeReport(Path output, String report) throws IOException {
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }

    /** Returns command-line usage instructions. */
    public static String helpText() {
        return """
            Використання: java -jar lab01-2.0.0.jar [--help] [--version] [--input <файл>] [--output <файл>]
            """;
    }

    /** Parsed records and validation errors. */
    public record Analysis(List<Plant> plants, List<String> errors) {
        public Analysis {
            plants = List.copyOf(plants);
            errors = List.copyOf(errors);
        }
    }

    private record Arguments(Path input, Path output, boolean help, boolean version) {
        private static Arguments parse(String[] args) {
            Path input = null;
            Path output = DEFAULT_OUTPUT;
            boolean help = false;
            boolean version = false;
            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "--help" -> help = true;
                    case "--version" -> version = true;
                    case "--input" -> input = value(args, ++index, "--input");
                    case "--output" -> output = value(args, ++index, "--output");
                    default -> throw new IllegalArgumentException(
                            "невідомий аргумент: %s".formatted(args[index]));
                }
            }
            if (!help && !version && input == null) {
                throw new IllegalArgumentException("потрібно вказати вхідний файл через --input");
            }
            return new Arguments(input, output, help, version);
        }

        private static Path value(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("після %s потрібно вказати шлях".formatted(option));
            }
            return Path.of(args[index]);
        }
    }
}
