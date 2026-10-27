package ua.lpnu.kzp;

import java.nio.file.Path;

/** Parses and validates command-line arguments. */
final class ArgumentParser {

    private static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    private ArgumentParser() {
    }

    /**
     * Parses command-line arguments.
     *
     * @param args command-line arguments
     * @return validated options
     * @throws IllegalArgumentException if an argument is unknown or a required value is missing
     */
    static CommandLineOptions parse(String[] args) {
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
        return new CommandLineOptions(input, output, help, version);
    }

    /**
     * Returns command-line usage instructions.
     *
     * @return usage text
     */
    static String helpText() {
        return "Використання: java -jar %s.jar [--help] [--version] [--input <файл>] [--output <файл>]%n"
                .formatted(ApplicationInfo.jarName());
    }

    private static Path value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("після %s потрібно вказати шлях".formatted(option));
        }
        return Path.of(args[index]);
    }
}
