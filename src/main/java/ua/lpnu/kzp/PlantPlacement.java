package ua.lpnu.kzp;

import java.util.Locale;

/**
 * Placement advice built with a pattern-matching {@code switch} over the sealed {@link Plant}
 * hierarchy. The compiler checks that every permitted subtype is handled, so there is no
 * {@code default} branch. The switch contains no care calculations: those stay in
 * {@link Plant#careNeed()}.
 */
public final class PlantPlacement {

    private PlantPlacement() {
    }

    /**
     * Recommends where to plant the given plant.
     *
     * @param plant plant to place
     * @return placement advice
     */
    public static String recommend(Plant plant) {
        return switch (plant) {
            case Tree tree -> String.format(Locale.ROOT, "відкритий ґрунт, потрібен простір для висоти %.0f см",
                    tree.heightCm());
            case Flower flower -> String.format(Locale.ROOT, "клумба або горщик, полив кожні %d дн.",
                    flower.wateringDays());
        };
    }
}
