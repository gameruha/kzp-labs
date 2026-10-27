package ua.lpnu.kzp;

import java.io.IOException;

/** Entry point of the console application for processing plant nursery records, variant 12. */
public final class Main {

    private static final int EXIT_OK = 0;
    private static final int EXIT_IO_ERROR = 1;
    private static final int EXIT_USAGE_ERROR = 2;

    private Main() {
    }

    /**
     * Runs the application and terminates the process with its exit code.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Validates arguments, starts the controller and converts failures to an exit code.
     *
     * @param args command-line arguments
     * @return process exit code: 0 on success, 1 on file errors, 2 on invalid arguments
     */
    static int run(String[] args) {
        try {
            CommandLineOptions options = ArgumentParser.parse(args);
            new Controller().execute(options);
            return EXIT_OK;
        } catch (IllegalArgumentException exception) {
            System.err.println("Помилка аргументів: %s".formatted(exception.getMessage()));
            System.err.print(ArgumentParser.helpText());
            return EXIT_USAGE_ERROR;
        } catch (IOException exception) {
            System.err.println("Помилка роботи з файлами: %s".formatted(exception.getMessage()));
            return EXIT_IO_ERROR;
        }
    }
}
