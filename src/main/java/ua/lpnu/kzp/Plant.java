package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;

/**
 * Shared sealed base type of the nursery plants: stores the common fields, validates the common
 * invariants and declares the polymorphic {@link #careNeed()} operation.
 */
public abstract sealed class Plant permits Tree, Flower {

    private final PlantKind kind;

    @CsvColumn("вид")
    private final String species;

    private final String name;

    @CsvColumn("вік")
    private final int ageYears;

    @CsvColumn("висота")
    private final double heightCm;

    @CsvColumn("ціна")
    private final double price;

    @CsvColumn("полив")
    private final int wateringDays;

    /**
     * Creates a plant with valid common field values and default age of 1 year.
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
                1, validateHeight(heightCm), validatePrice(price),
                validateWateringDays(wateringDays), true);
    }

    /**
     * Creates a plant with valid common field values including age in years.
     *
     * @param kind plant category
     * @param species plant species
     * @param name plant name
     * @param ageYears plant age in years
     * @param heightCm plant height in centimeters
     * @param price plant price
     * @param wateringDays watering interval in days
     */
    protected Plant(PlantKind kind, String species, String name, int ageYears, double heightCm,
            double price, int wateringDays) {
        this(requireKind(kind), validateText(species, "species"), validateText(name, "name"),
                validateAgeYears(ageYears), validateHeight(heightCm), validatePrice(price),
                validateWateringDays(wateringDays), true);
    }

    /* Присвоює вже перевірений стан; делегування не залишає частково створений об'єкт. */
    private Plant(PlantKind kind, String species, String name, int ageYears, double heightCm,
            double price, int wateringDays, boolean validated) {
        this.kind = kind;
        this.species = species;
        this.name = name;
        this.ageYears = ageYears;
        this.heightCm = heightCm;
        this.price = price;
        this.wateringDays = wateringDays;
    }

    /**
     * Creates a plant of the matching subtype from a semicolon-separated CSV row.
     *
     * @param line row in {@code species;name;ageYears;heightCm;price;wateringDays} or
     *     {@code species;name;heightCm;price;wateringDays} format
     * @return validated tree or flower
     * @throws IllegalArgumentException if the row is malformed or violates a plant invariant
     */
    public static Plant fromCsv(String line) {
        Objects.requireNonNull(line, "CSV-рядок не може бути null");
        String[] fields = line.split(";", -1);
        if (fields.length != 5 && fields.length != 6) {
            throw new IllegalArgumentException("очікується 5 або 6 полів");
        }

        String species = fields[0].trim();
        String name = fields[1].trim();
        validateText(species, "species");
        validateText(name, "name");
        try {
            int ageYears = fields.length == 6 ? Integer.parseInt(fields[2].trim()) : 1;
            int offset = fields.length == 6 ? 1 : 0;
            double heightCm = Double.parseDouble(fields[2 + offset].trim());
            double price = Double.parseDouble(fields[3 + offset].trim());
            int wateringDays = Integer.parseInt(fields[4 + offset].trim());
            // Підтип обирається один раз під час створення; далі код працює лише зі спільним типом.
            return switch (PlantKind.fromSpecies(species)) {
                case TREE -> new Tree(species, name, ageYears, heightCm, price, wateringDays);
                case FLOWER -> new Flower(species, name, ageYears, heightCm, price, wateringDays);
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
     * Returns the plant age in years.
     *
     * @return plant age in years
     */
    public final int ageYears() {
        return ageYears;
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
