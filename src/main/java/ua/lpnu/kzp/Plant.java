package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/** An immutable, validated plant record from the nursery CSV input. */
public final class Plant {

    private final String species;
    private final String name;
    private final double heightCm;
    private final double price;
    private final int wateringDays;

    /**
     * Creates a plant with valid field values.
     *
     * @param species plant species
     * @param name plant name
     * @param heightCm plant height in centimeters
     * @param price plant price
     * @param wateringDays watering interval in days
     */
    public Plant(String species, String name, double heightCm, double price, int wateringDays) {
        validateNames(species, name);
        if (!Double.isFinite(heightCm) || !Double.isFinite(price)
                || heightCm <= 0 || price < 0 || wateringDays <= 0) {
            throw new IllegalArgumentException(
                    "heightCm має бути скінченною і додатною, price — скінченною і невід'ємною, wateringDays має бути додатним");
        }
        this.species = species.trim();
        this.name = name.trim();
        this.heightCm = heightCm;
        this.price = price;
        this.wateringDays = wateringDays;
    }

    /**
     * Creates a plant from a semicolon-separated CSV row.
     *
     * @param line row in species;name;heightCm;price;wateringDays format
     * @return validated plant
     * @throws IllegalArgumentException if the row is malformed or violates a plant invariant
     */
    public static Plant fromCsv(String line) {
        Objects.requireNonNull(line, "CSV-рядок не може бути null");
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("очікується 5 полів");
        }

        String species = fields[0].trim();
        String name = fields[1].trim();
        validateNames(species, name);
        try {
            return new Plant(species, name,
                    Double.parseDouble(fields[2].trim()),
                    Double.parseDouble(fields[3].trim()),
                    Integer.parseInt(fields[4].trim()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("неправильний числовий формат", exception);
        }
    }

    private static void validateNames(String species, String name) {
        Objects.requireNonNull(species, "species не може бути null");
        Objects.requireNonNull(name, "name не може бути null");
        if (species.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("species і name не можуть бути порожніми");
        }
    }

    /** Returns the plant species. */
    public String species() {
        return species;
    }

    /** Returns the plant name. */
    public String name() {
        return name;
    }

    /** Returns the plant height in centimeters. */
    public double heightCm() {
        return heightCm;
    }

    /** Returns the plant price. */
    public double price() {
        return price;
    }

    /** Returns the watering interval in days. */
    public int wateringDays() {
        return wateringDays;
    }

    /** Returns a formatted plant description for the report. */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s (%.2f грн)", species, name, price);
    }
}