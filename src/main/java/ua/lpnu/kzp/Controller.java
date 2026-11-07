package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Controls processing: help, version, or reading, storing in {@link Repository} and exporting UTF-8
 * CSV in Variant 12 column order.
 */
final class Controller {

    /**
     * Executes the action requested by the options.
     *
     * @param options validated command-line options
     * @throws IOException if the input cannot be read
     * @throws DataStorageException if CSV parsing or reflective export fails
     */
    void execute(CommandLineOptions options) throws IOException, DataStorageException {
        if (options.help()) {
            System.out.print(ArgumentParser.helpText());
            return;
        }
        if (options.version()) {
            System.out.println(ApplicationInfo.version());
            return;
        }
        List<String> lines = Files.readAllLines(options.input(), StandardCharsets.UTF_8);
        Repository<PlantRecord> repository = new Repository<>();
        if (isSemicolonInput(lines)) {
            Analysis analysis = PlantAnalyzer.analyze(lines);
            for (Plant plant : analysis.plants()) {
                repository.add(PlantRecord.fromPlant(plant));
            }
        } else {
            for (PlantRecord record : PlantRecord.readCsv(options.input())) {
                repository.add(record);
            }
        }
        CsvExporter.write(options.output(), PlantRecord.class, repository.all());
        String csv = Files.readString(options.output(), StandardCharsets.UTF_8);
        System.out.print(csv);
    }

    private static boolean isSemicolonInput(List<String> lines) {
        return lines.stream().anyMatch(line -> line.contains(";"));
    }
}
