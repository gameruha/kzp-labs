package ua.lpnu.kzp;

/** A flower: small plant that needs frequent attention regardless of its height. */
public final class Flower extends Plant {

    private static final double MAX_HEIGHT_CM = 200.0;
    private static final int MAX_WATERING_DAYS = 30;
    private static final double FLOWER_CARE_FACTOR = 14.0;

    /**
     * Creates a flower whose height does not exceed 200 cm and watering interval is at most 30 days.
     *
     * @param species flower species
     * @param name flower name
     * @param heightCm flower height in centimeters
     * @param price flower price
     * @param wateringDays watering interval in days
     */
    public Flower(String species, String name, double heightCm, double price, int wateringDays) {
        // Спочатку базовий клас перевіряє спільні поля, потім підтип — власні правила.
        super(PlantKind.FLOWER, species, name, heightCm, price, wateringDays);
        if (heightCm > MAX_HEIGHT_CM || wateringDays > MAX_WATERING_DAYS) {
            throw new IllegalArgumentException(
                    "квітка: висота має бути не більшою за 200 см, інтервал поливу — не більшим за 30 днів");
        }
    }

    /**
     * Calculates the care need, which depends only on how often the flower must be watered.
     *
     * @return care need index
     */
    @Override
    public double careNeed() {
        return FLOWER_CARE_FACTOR / wateringDays();
    }
}
