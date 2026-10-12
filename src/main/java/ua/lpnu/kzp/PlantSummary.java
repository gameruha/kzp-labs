package ua.lpnu.kzp;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable summary of the nursery plant records.
 *
 * @param count number of valid plants
 * @param averageHeightCm average plant height in centimeters
 * @param mostExpensive most expensive plant, or null when there are no valid plants
 * @param minimumWateringDays shortest watering interval, or zero when there are no valid plants
 */
public record PlantSummary(int count, double averageHeightCm,
                           Plant mostExpensive, int minimumWateringDays) {

        public PlantSummary {
                if (count < 0 || !Double.isFinite(averageHeightCm) || averageHeightCm < 0
                                || minimumWateringDays < 0
                                || (count == 0 && (averageHeightCm != 0 || mostExpensive != null || minimumWateringDays != 0))
                                || (count > 0 && (averageHeightCm == 0 || mostExpensive == null || minimumWateringDays == 0))) {
                        throw new IllegalArgumentException("Підсумкові показники некоректні");
                }
        }

         /**
          * Calculates summary metrics for the supplied plants.
          *
          * @param plants valid plant records
          * @return summary metrics
          */
    public static PlantSummary from(List<Plant> plants) {
        Objects.requireNonNull(plants, "plants не може бути null");
        int count = plants.size();
        double averageHeightCm = plants.stream()
                .mapToDouble(Plant::heightCm)
                .average()
                .orElse(0.0);
        Plant mostExpensive = plants.stream()
                .max(Comparator.comparingDouble(Plant::price))
                .orElse(null);
        int minimumWateringDays = plants.stream()
                .mapToInt(Plant::wateringDays)
                .min()
                .orElse(0);
        return new PlantSummary(count, averageHeightCm, mostExpensive, minimumWateringDays);
    }
}