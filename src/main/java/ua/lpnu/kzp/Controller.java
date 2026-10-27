package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Controls processing: help, version, or reading, analyzing and reporting the input file. */
final class Controller {

    /**
     * Executes the action requested by the options.
     *
     * @param options validated command-line options
     * @throws IOException if the input cannot be read or the report cannot be written
     */
    void execute(CommandLineOptions options) throws IOException {
        if (options.help()) {
            System.out.print(ArgumentParser.helpText());
            return;
        }
        if (options.version()) {
            System.out.println(ApplicationInfo.version());
            return;
        }
        Analysis analysis = PlantAnalyzer.analyze(Files.readAllLines(options.input(), StandardCharsets.UTF_8));
        String report = ReportFormatter.format(analysis);
        System.out.print(report);
        writeReport(options.output(), report);
    }

    private static void writeReport(Path output, String report) throws IOException {
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }
}
