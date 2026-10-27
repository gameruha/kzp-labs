package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/**
 * Shared sealed base type of the nursery plants: stores the common fields, validates the common
 * invariants and declares the polymorphic {@link #careNeed()} operation.
 */
public abstract sealed class Plant permits Tree, Flower {

    private final PlantKind kind;
    private final String species;
    private final String name;
    private final double heightCm;
    private final double price;
    private final int wateringDays;

    /**
     * Creates a plant with valid common field values.
     *
     * @param kind plant category
     * @param species plant species
     * @param name plant name
     * @param heightCm plant height in centimeters
     * @param price plant price
     * @param wateringDays watering interval in days
     */
    protected Plant(PlantKind kind, String species, String name, double heightCm, double price,
            int wateringDays) {
        this(requireKind(kind), validateText(species, "species"), validateText(name, "name"),
                validateHeight(heightCm), validatePrice(price), validateWateringDays(wateringDays), true);
    }

    /* Присвоює вже перевірений стан; делегування не залишає частково створений об'єкт. */
    private Plant(PlantKind kind, String species, String name, double heightCm, double price,
            int wateringDays, boolean validated) {
        this.kind = kind;
        this.species = species;
        this.name = name;
        this.heightCm = heightCm;
        this.price = price;
        this.wateringDays = wateringDays;
    }

    /**
     * Creates a plant of the matching subtype from a semicolon-separated CSV row.
     *
     * @param line row in species;name;heightCm;price;wateringDays format
     * @return validated tree or flower
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
        validateText(species, "species");
        validateText(name, "name");
        try {
            double heightCm = Double.parseDouble(fields[2].trim());
            double price = Double.parseDouble(fields[3].trim());
            int wateringDays = Integer.parseInt(fields[4].trim());
            // Підтип обирається один раз під час створення; далі код працює лише зі спільним типом.
            return switch (PlantKind.fromSpecies(species)) {
                case TREE -> new Tree(species, name, heightCm, price, wateringDays);
                case FLOWER -> new Flower(species, name, heightCm, price, wateringDays);
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("неправильний числовий формат", exception);
        }
    }

    private static PlantKind requireKind(PlantKind kind) {
        return Objects.requireNonNull(kind, "kind не може бути null");
    }

    private static String validateText(String text, String field) {
        Objects.requireNonNull(text, "%s не може бути null".formatted(field));
        if (text.isBlank()) {
            throw new IllegalArgumentException("species і name не можуть бути порожніми");
        }
        return text.trim();
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
     * Calculates how much care the plant needs; every subtype provides its own formula.
     *
     * @return care need index, higher means more care
     */
    public abstract double careNeed();

    /**
     * Returns the plant category.
     *
     * @return plant category
     */
    public final PlantKind kind() {
        return kind;
    }

    /**
     * Returns the plant species.
     *
     * @return plant species
     */
    public final String species() {
        return species;
    }

    /**
     * Returns the plant name.
     *
     * @return plant name
     */
    public final String name() {
        return name;
    }

    /**
     * Returns the plant height in centimeters.
     *
     * @return plant height in centimeters
     */
    public final double heightCm() {
        return heightCm;
    }

    /**
     * Returns the plant price.
     *
     * @return plant price
     */
    public final double price() {
        return price;
    }

    /**
     * Returns the watering interval in days.
     *
     * @return watering interval in days
     */
    public final int wateringDays() {
        return wateringDays;
    }

    /**
     * Compares plants by concrete subtype, species and name.
     *
     * @param other object to compare with
     * @return true if both plants have the same subtype, species and name
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Plant plant = (Plant) other;
        // getClass не допускає рівність дерева та квітки з однаковими видом і назвою.
        return species.equals(plant.species) && name.equals(plant.name);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return hash code
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), species, name);
    }

    /** Returns a formatted plant description for the report. */
    @Override
    public final String toString() {
        return String.format(Locale.ROOT, "%s %s (%.2f грн)", species, name, price);
    }
}
