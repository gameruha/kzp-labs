package ua.lpnu.kzp;

import java.nio.file.Path;

/**
 * Validated command-line options.
 *
 * @param input input CSV path, or null when only help or version is requested
 * @param output report output path
 * @param help whether usage instructions were requested
 * @param version whether the application version was requested
 */
record CommandLineOptions(Path input, Path output, boolean help, boolean version) {
}
