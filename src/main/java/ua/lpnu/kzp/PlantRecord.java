package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Domain record of Variant 12 ("Розсадник рослин") for reflective CSV/JSON export and round-trip
 * restoration.
 *
 * <p>Annotated fields in Variant 12 column order: {@code species} («вид»), {@code ageYears}
 * («вік»), {@code heightCm} («висота»), {@code price} («ціна»), {@code wateringDays} («полив»).
 * Round-trip key: {@code species + ageYears}.
 */
public final class PlantRecord {

    @CsvColumn("вид")
    private final String species;

    @CsvColumn("вік")
    private final int ageYears;

    @CsvColumn("висота")
    private final double heightCm;

    @CsvColumn("ціна")
    private final double price;

    @CsvColumn("полив")
    private final int wateringDays;

    /**
     * Creates a validated nursery plant record.
     *
     * @param species plant species (non-blank)
     * @param ageYears plant age in years (positive)
     * @param heightCm plant height in centimeters (finite, positive)
     * @param price plant price in UAH (finite, non-negative)
     * @param wateringDays watering interval in days (positive)
     * @throws IllegalArgumentException if any invariant is violated
     */
    public PlantRecord(String species, int ageYears, double heightCm, double price, int wateringDays) {
        this(validateSpecies(species), validateAgeYears(ageYears), validateHeight(heightCm),
                validatePrice(price), validateWateringDays(wateringDays), true);
    }

    private PlantRecord(String species, int ageYears, double heightCm, double price,
            int wateringDays, boolean validated) {
        this.species = species;
        this.ageYears = ageYears;
        this.heightCm = heightCm;
        this.price = price;
        this.wateringDays = wateringDays;
    }

    /**
     * Creates a {@link PlantRecord} from a polymorphic {@link Plant} using its {@link Plant#ageYears()}.
     *
     * @param plant domain plant entity
     * @return corresponding {@link PlantRecord}
     */
    public static PlantRecord fromPlant(Plant plant) {
        Objects.requireNonNull(plant, "Рослина не може бути null");
        return fromPlant(plant, plant.ageYears());
    }

    /**
     * Creates a {@link PlantRecord} from a polymorphic {@link Plant} and an explicit age in years.
     *
     * @param plant domain plant entity
     * @param ageYears plant age in years
     * @return corresponding {@link PlantRecord}
     */
    public static PlantRecord fromPlant(Plant plant, int ageYears) {
        Objects.requireNonNull(plant, "Рослина не може бути null");
        return new PlantRecord(plant.species(), ageYears, plant.heightCm(), plant.price(),
                plant.wateringDays());
    }

    /**
     * Parses a single CSV line in the Variant 12 export column order
     * ({@code вид,вік,висота,ціна,полив}).
     *
     * @param line CSV record line
     * @return restored {@link PlantRecord}
     * @throws DataStorageException if the CSV syntax, field count or field values are invalid
     */
    public static PlantRecord fromCsv(String line) throws DataStorageException {
        List<String> fields = CsvParser.parse(line);
        return fromFields(fields);
    }

    /**
     * Restores a {@link PlantRecord} from five parsed CSV fields in Variant 12 order:
     * {@code species}, {@code ageYears}, {@code heightCm}, {@code price}, {@code wateringDays}.
     *
     * @param fields parsed CSV fields
     * @return restored {@link PlantRecord}
     * @throws DataStorageException if the field count is not 5 or a value is invalid
     */
    public static PlantRecord fromFields(List<String> fields) throws DataStorageException {
        Objects.requireNonNull(fields, "Поля CSV не можуть бути null");
        if (fields.size() != 5) {
            throw new DataStorageException(
                    "Очікується 5 полів CSV, отримано: %d".formatted(fields.size()));
        }
        try {
            String species = fields.get(0);
            int ageYears = Integer.parseInt(fields.get(1).trim());
            double heightCm = Double.parseDouble(fields.get(2).trim());
            double price = Double.parseDouble(fields.get(3).trim());
            int wateringDays = Integer.parseInt(fields.get(4).trim());
            return new PlantRecord(species, ageYears, heightCm, price, wateringDays);
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException("Некоректний рядок рослини у CSV", exception);
        }
    }

    /**
     * Reads a UTF-8 CSV document from {@code path}, skips the header row and restores all
     * {@link PlantRecord} items.
     *
     * @param path path to the UTF-8 CSV file
     * @return immutable list of restored records (empty if the file is empty or contains only a header)
     * @throws DataStorageException if reading fails or any row is malformed
     */
    public static List<PlantRecord> readCsv(Path path) throws DataStorageException {
        Objects.requireNonNull(path, "Шлях не може бути null");
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: %s".formatted(path), exception);
        }
        List<List<String>> rows = CsvParser.parseDocument(content);
        if (rows.size() <= 1) {
            return List.of();
        }
        List<PlantRecord> restored = new ArrayList<>(rows.size() - 1);
        for (int index = 1; index < rows.size(); index++) {
            restored.add(fromFields(rows.get(index)));
        }
        return List.copyOf(restored);
    }

    /**
     * Parses an explicit {@link PlantKind} enum constant from a CSV field, wrapping unknown values
     * in {@link DataStorageException} with the original {@link IllegalArgumentException} cause.
     *
     * @param rawKind enum name (e.g. {@code TREE} or {@code FLOWER})
     * @return matching {@link PlantKind}
     * @throws DataStorageException if the enum constant is unknown
     */
    public static PlantKind parseKind(String rawKind) throws DataStorageException {
        if (rawKind == null || rawKind.isBlank()) {
            throw new DataStorageException("Порожнє значення категорії PlantKind");
        }
        try {
            return PlantKind.valueOf(rawKind.trim());
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException(
                    "Невідома категорія рослини: %s".formatted(rawKind), exception);
        }
    }

    private static String validateSpecies(String species) {
        if (species == null || species.isBlank()) {
            throw new IllegalArgumentException("species не може бути порожнім");
        }
        return species.trim();
    }

    private static int validateAgeYears(int ageYears) {
        if (ageYears <= 0) {
            throw new IllegalArgumentException("ageYears має бути додатним");
        }
        return ageYears;
    }

    private static double validateHeight(double heightCm) {
        if (!Double.isFinite(heightCm) || heightCm <= 0) {
            throw new IllegalArgumentException("heightCm має бути скінченною і додатною");
        }
        return heightCm;
    }

    private static double validatePrice(double price) {
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("price має бути скінченною і невід'ємною");
        }
        return price;
    }

    private static int validateWateringDays(int wateringDays) {
        if (wateringDays <= 0) {
            throw new IllegalArgumentException("wateringDays має бути додатним");
        }
        return wateringDays;
    }

    /**
     * Returns the plant species.
     *
     * @return plant species
     */
    public String species() {
        return species;
    }

    /**
     * Returns the plant age in years.
     *
     * @return plant age in years
     */
    public int ageYears() {
        return ageYears;
    }

    /**
     * Returns the plant height in centimeters.
     *
     * @return plant height in centimeters
     */
    public double heightCm() {
        return heightCm;
    }

    /**
     * Returns the plant price in UAH.
     *
     * @return plant price in UAH
     */
    public double price() {
        return price;
    }

    /**
     * Returns the watering interval in days.
     *
     * @return watering interval in days
     */
    public int wateringDays() {
        return wateringDays;
    }

    /**
     * Returns the plant category derived from {@link #species()}.
     *
     * @return plant category
     */
    public PlantKind kind() {
        return PlantKind.fromSpecies(species);
    }

    /**
     * Returns the Variant 12 composite round-trip key ({@code species + ageYears}).
     *
     * @return composite key string
     */
    public String roundTripKey() {
        return "%s:%d".formatted(species, ageYears);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlantRecord that)) {
            return false;
        }
        return ageYears == that.ageYears
                && Double.compare(heightCm, that.heightCm) == 0
                && Double.compare(price, that.price) == 0
                && wateringDays == that.wateringDays
                && species.equals(that.species);
    }

    @Override
    public int hashCode() {
        return Objects.hash(species, ageYears, heightCm, price, wateringDays);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%d р., %.2f см, %.2f грн, полив %d дн.)",
                species, ageYears, heightCm, price, wateringDays);
    }
}
