package ua.lpnu.kzp;

import java.util.List;

/**
 * Parsed records and validation errors.
 *
 * @param plants validated plant records
 * @param errors validation errors with source line numbers
 */
public record Analysis(List<Plant> plants, List<String> errors) {

    /** Copies both lists and prevents later external mutation. */
    public Analysis {
        plants = List.copyOf(plants);
        errors = List.copyOf(errors);
    }
}
