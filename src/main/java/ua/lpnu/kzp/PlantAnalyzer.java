package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/** Parses CSV lines into plants and collects validation errors. */
public final class PlantAnalyzer {

    private static final String HEADER = "species;name;heightCm;price;wateringDays";

    private PlantAnalyzer() {
    }

    /**
     * Parses input lines and collects valid plants and line-numbered errors.
     *
     * @param lines input lines in UTF-8
     * @return valid records and validation messages
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
}
