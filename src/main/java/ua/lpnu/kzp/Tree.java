package ua.lpnu.kzp;

/** A tree: tall plant that is watered rarely, but needs more care the taller it grows. */
public final class Tree extends Plant {

    private static final double MAX_HEIGHT_CM = 10_000.0;
    private static final int MAX_WATERING_DAYS = 60;
    private static final double DAYS_IN_WEEK = 7.0;

    /**
     * Creates a tree whose height does not exceed 100 m and watering interval is at most 60 days.
     *
     * @param species tree species
     * @param name tree name
     * @param heightCm tree height in centimeters
     * @param price tree price
     * @param wateringDays watering interval in days
     */
    public Tree(String species, String name, double heightCm, double price, int wateringDays) {
        // Спочатку базовий клас перевіряє спільні поля, потім підтип — власні правила.
        super(PlantKind.TREE, species, name, heightCm, price, wateringDays);
        if (heightCm > MAX_HEIGHT_CM || wateringDays > MAX_WATERING_DAYS) {
            throw new IllegalArgumentException(
                    "дерево: висота має бути не більшою за 10000 см, інтервал поливу — не більшим за 60 днів");
        }
    }

    /**
     * Calculates the weekly care need, which grows with the tree height.
     *
     * @return care need index
     */
    @Override
    public double careNeed() {
        return DAYS_IN_WEEK / wateringDays() * (1.0 + heightCm() / 100.0);
    }
}
