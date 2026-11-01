package ua.lpnu.kzp;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pure Stream API queries over a polymorphic list of plants. The methods never modify the input,
 * contain no loops and no branching by subtype, and perform no console formatting.
 */
public final class PlantStreamQueries {

    /** Threshold for the filter query: plants watered at most every this many days. */
    public static final int FREQUENT_WATERING_DAYS = 2;

    /** Number of plants returned by the top-N query in the report. */
    public static final int TOP_LIMIT = 5;

    /** Primary criterion descending, then species and name ascending. */
    private static final Comparator<Plant> BY_HEIGHT_DESCENDING = Comparator
            .comparingDouble(Plant::heightCm)
            .reversed()
            .thenComparing(Plant::species)
            .thenComparing(Plant::name);

    private PlantStreamQueries() {
    }

    /**
     * Selects plants that must be watered at most every {@link #FREQUENT_WATERING_DAYS} days.
     *
     * @param plants source plants
     * @return new immutable list in the original order
     */
    public static List<Plant> frequentlyWatered(List<Plant> plants) {
        return streamOf(plants)
                .filter(plant -> plant.wateringDays() <= FREQUENT_WATERING_DAYS)
                .toList();
    }

    /**
     * Maps plants to their distinct species names in alphabetical order.
     *
     * @param plants source plants
     * @return new immutable list of species
     */
    public static List<String> speciesNames(List<Plant> plants) {
        return streamOf(plants)
                .map(Plant::species)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Groups plants by category and averages the price in each group.
     *
     * @param plants source plants
     * @return map in the stable {@link PlantKind} order; categories without plants are absent
     */
    public static Map<PlantKind, Double> averagePriceByKind(List<Plant> plants) {
        return streamOf(plants)
                .collect(Collectors.groupingBy(
                        Plant::kind,
                        () -> new EnumMap<>(PlantKind.class),
                        Collectors.averagingDouble(Plant::price)));
    }

    /**
     * Summarizes plant heights.
     *
     * @param plants source plants
     * @return count, sum, minimum, maximum and average height; for an empty list the count is zero
     */
    public static DoubleSummaryStatistics heightStatistics(List<Plant> plants) {
        return streamOf(plants)
                .collect(Collectors.summarizingDouble(Plant::heightCm));
    }

    /**
     * Selects the tallest plants; ties are ordered by species and then by name.
     *
     * @param plants source plants
     * @param limit maximum number of plants, zero returns an empty list
     * @return new immutable list, at most {@code limit} elements
     * @throws IllegalArgumentException if the limit is negative
     */
    public static List<Plant> topByHeight(List<Plant> plants, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Ліміт не може бути від'ємним");
        }
        // Спочатку формується повний порядок, а limit залишає лише потрібний префікс.
        return streamOf(plants)
                .sorted(BY_HEIGHT_DESCENDING)
                .limit(limit)
                .toList();
    }

    /**
     * Finds the first plant of the given species in the original order.
     *
     * @param plants source plants
     * @param species requested species
     * @return the first match or an empty result
     */
    public static Optional<Plant> findBySpecies(List<Plant> plants, String species) {
        Objects.requireNonNull(species, "Вид не може бути null");
        return streamOf(plants)
                .filter(plant -> plant.species().equals(species))
                .findFirst();
    }

    /**
     * Counts plants by category and, inside each category, by species (two-level grouping).
     *
     * @param plants source plants
     * @return categories in {@link PlantKind} order, species in alphabetical order
     */
    public static Map<PlantKind, Map<String, Long>> countByKindAndSpecies(List<Plant> plants) {
        return streamOf(plants)
                .collect(Collectors.groupingBy(
                        Plant::kind,
                        () -> new EnumMap<>(PlantKind.class),
                        Collectors.groupingBy(
                                Plant::species,
                                TreeMap::new,
                                Collectors.counting())));
    }

    /* Перевіряє список і повертає його як новий потік для окремого запиту. */
    private static Stream<Plant> streamOf(List<Plant> plants) {
        return Objects.requireNonNull(plants, "Список не може бути null").stream();
    }
}
