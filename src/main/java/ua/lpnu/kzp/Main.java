package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Console application for processing plant nursery records, variant 12. */
public final class Main {

    private static final String VERSION = "1.0.0";
    private static final Path DEFAULT_INPUT = Path.of("data", "input.csv");
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
        try {
            Arguments arguments = Arguments.parse(args);
            if (arguments.help()) {
                System.out.print(helpText());
                return;
            }
            if (arguments.version()) {
                System.out.println(VERSION);
                return;
            }
            Analysis analysis = analyze(Files.readAllLines(arguments.input(), StandardCharsets.UTF_8));
            String report = formatReport(analysis);
            System.out.print(report);
            writeReport(arguments.output(), report);
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка аргументів: " + exception.getMessage());
            System.err.print(helpText());
        } catch (IOException exception) {
            System.err.println("Помилка роботи з файлами: " + exception.getMessage());
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
            String[] fields = line.split(";", -1);
            if (fields.length != 5) {
                errors.add("Рядок %d: очікується 5 полів".formatted(lineNumber));
                continue;
            }
            if (fields[0].isBlank() || fields[1].isBlank()) {
                errors.add("Рядок %d: species і name не можуть бути порожніми".formatted(lineNumber));
                continue;
            }
            try {
                double heightCm = Double.parseDouble(fields[2].trim());
                double price = Double.parseDouble(fields[3].trim());
                int wateringDays = Integer.parseInt(fields[4].trim());
                if (!Double.isFinite(heightCm) || !Double.isFinite(price)
                        || heightCm <= 0 || price < 0 || wateringDays <= 0) {
                    errors.add("Рядок %d: heightCm і wateringDays мають бути додатними, price не може бути від'ємним"
                            .formatted(lineNumber));
                    continue;
                }
                plants.add(new Plant(fields[0].trim(), fields[1].trim(), heightCm, price, wateringDays));
            } catch (NumberFormatException exception) {
                errors.add("Рядок %d: неправильний числовий формат".formatted(lineNumber));
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
        int count = analysis.plants().size();
        double averageHeight = count == 0 ? 0.0
                : analysis.plants().stream().mapToDouble(Plant::heightCm).average().orElse(0.0);
        Plant mostExpensive = analysis.plants().stream()
                .max(Comparator.comparingDouble(Plant::price)).orElse(null);
        int minimumWateringDays = analysis.plants().stream()
                .mapToInt(Plant::wateringDays).min().orElse(0);

        StringBuilder report = new StringBuilder();
        report.append("=== Звіт розсадника рослин (варіант 12) ===%n".formatted());
        report.append("Коректних записів: %d%n".formatted(count));
        report.append(String.format(Locale.ROOT, "Середня висота: %.2f см%n", averageHeight));
        report.append("Найдорожча рослина: %s%n".formatted(mostExpensive == null
                ? "немає коректних записів"
                : String.format(Locale.ROOT, "%s %s (%.2f грн)", mostExpensive.species(),
                    mostExpensive.name(), mostExpensive.price())));
        report.append("Найменший інтервал поливу: %d днів%n".formatted(minimumWateringDays));
        report.append("Помилок: %d%n".formatted(analysis.errors().size()));
        analysis.errors().forEach(error -> report.append(error).append(System.lineSeparator()));
        if (count == 0) {
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
        return "Використання: java -jar lab01-1.0.0.jar [--help] [--version] "
                + "[--input <файл>] [--output <файл>]%n".formatted();
    }

    /** A validated plant record from the input file. */
    public record Plant(String species, String name, double heightCm, double price, int wateringDays) {
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
            Path input = DEFAULT_INPUT;
            Path output = DEFAULT_OUTPUT;
            boolean help = false;
            boolean version = false;
            for (int index = 0; index < args.length; index++) {
                switch (args[index]) {
                    case "--help" -> help = true;
                    case "--version" -> version = true;
                    case "--input" -> input = value(args, ++index, "--input");
                    case "--output" -> output = value(args, ++index, "--output");
                    default -> throw new IllegalArgumentException("невідомий аргумент: " + args[index]);
                }
            }
            return new Arguments(input, output, help, version);
        }

        private static Path value(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("після " + option + " потрібно вказати шлях");
            }
            return Path.of(args[index]);
        }
    }
}
