package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/** Parses CSV lines into plants and collects validation errors. */
public final class PlantAnalyzer {

    private static final String HEADER_5 = "species;name;heightCm;price;wateringDays";
    private static final String HEADER_6 = "species;name;ageYears;heightCm;price;wateringDays";

    private PlantAnalyzer() {
    }

    /**
     * Parses input lines and collects valid plants and line-numbered errors.
     *
     * @param lines input lines in UTF-8
     * @return valid records and validation messages
     */
    public static Analysis analyze(List<String> lines) {
        Repository<Plant> repository = new Repository<>();
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int lineNumber = index + 1;
            String trimmed = line.trim();
            if (lineNumber == 1
                    && (HEADER_5.equalsIgnoreCase(trimmed) || HEADER_6.equalsIgnoreCase(trimmed))) {
                continue;
            }
            if (line.isBlank()) {
                errors.add("Рядок %d: порожній рядок".formatted(lineNumber));
                continue;
            }
            try {
                repository.add(Plant.fromCsv(line));
            } catch (IllegalArgumentException exception) {
                errors.add("Рядок %d: %s".formatted(lineNumber, exception.getMessage()));
            }
        }
        return new Analysis(repository.all(), errors);
    }
}
