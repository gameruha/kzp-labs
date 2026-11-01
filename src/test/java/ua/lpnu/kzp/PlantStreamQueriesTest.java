package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlantStreamQueriesTest {

    private static final double DELTA = 1e-9;

    private List<Plant> plants;

    @BeforeEach
    void loadControlDataset() throws IOException {
        plants = PlantAnalyzer.analyze(
                Files.readAllLines(Path.of("data", "lab04-input.csv"), StandardCharsets.UTF_8)).plants();
    }

    private static List<String> names(List<Plant> list) {
        return list.stream().map(Plant::name).toList();
    }

    /* Контрольний набір містить щонайменше шість коректних записів. */
    @Test
    void controlDatasetHasSevenValidPlants() {
        assertEquals(7, plants.size());
    }

    /* Запит 1: відбір на межі фільтра (1, 2 дні — так; 3 дні — ні) зберігає порядок. */
    @Test
    void filterKeepsPlantsUpToTheBoundaryInOriginalOrder() {
        assertEquals(List.of("Ялина", "Троянда", "Алое"), names(PlantStreamQueries.frequentlyWatered(plants)));
    }

    /* Запит 2: map повертає унікальні відсортовані види. */
    @Test
    void mapReturnsDistinctSortedSpecies() {
        assertEquals(List.of("Квіти", "Листяні", "Орхідеї", "Сукуленти", "Хвойні"),
                PlantStreamQueries.speciesNames(plants));
    }

    /* Запит 3: групування за enum-ключем зі стабільним порядком і середньою ціною. */
    @Test
    void groupingAveragesPriceByKindInStableOrder() {
        Map<PlantKind, Double> averages = PlantStreamQueries.averagePriceByKind(plants);

        assertEquals(List.of(PlantKind.TREE, PlantKind.FLOWER), List.copyOf(averages.keySet()));
        assertEquals((450.0 + 800.0 + 1200.0) / 3, averages.get(PlantKind.TREE), DELTA);
        assertEquals(152.5, averages.get(PlantKind.FLOWER), DELTA);
    }

    /* Запит 4: зведена статистика висоти. */
    @Test
    void statisticsSummarizeHeights() {
        DoubleSummaryStatistics statistics = PlantStreamQueries.heightStatistics(plants);

        assertEquals(7, statistics.getCount());
        assertEquals(865.5, statistics.getSum(), DELTA);
        assertEquals(30.0, statistics.getMin(), DELTA);
        assertEquals(300.0, statistics.getMax(), DELTA);
        assertEquals(865.5 / 7, statistics.getAverage(), DELTA);
    }

    /* Запит 5: top-5 — висота за спаданням, рівна висота розв'язується видом, шостий усічено. */
    @Test
    void topFiveOrdersByHeightThenSpeciesAndTruncatesTheSixth() {
        List<Plant> top = PlantStreamQueries.topByHeight(plants, PlantStreamQueries.TOP_LIMIT);

        assertEquals(List.of("Ялина", "Клен", "Туя", "Троянда", "Алое"), names(top));
        assertEquals(top.get(3).heightCm(), top.get(4).heightCm(), DELTA);
        assertTrue(top.get(3).species().compareTo(top.get(4).species()) < 0);
    }

    /* Граничні значення N: нуль, завеликий, від'ємний. */
    @Test
    void topHandlesZeroOversizedAndNegativeLimit() {
        assertEquals(List.of(), PlantStreamQueries.topByHeight(plants, 0));
        assertEquals(7, PlantStreamQueries.topByHeight(plants, 100).size());
        assertThrows(IllegalArgumentException.class, () -> PlantStreamQueries.topByHeight(plants, -1));
    }

    /* Optional: перший збіг при дублюваному виді, порожній результат для відсутнього. */
    @Test
    void searchReturnsFirstDuplicateOrEmptyOptional() {
        assertEquals("Туя", PlantStreamQueries.findBySpecies(plants, "Хвойні").orElseThrow().name());
        assertEquals("Троянда", PlantStreamQueries.findBySpecies(plants, "Квіти").orElseThrow().name());
        Optional<Plant> missing = PlantStreamQueries.findBySpecies(plants, "Папороті");
        assertTrue(missing.isEmpty());
        assertThrows(NullPointerException.class, () -> PlantStreamQueries.findBySpecies(plants, null));
    }

    /* Завдання із зірочкою: дворівневе групування категорія → вид → кількість. */
    @Test
    void twoLevelGroupingCountsSpeciesInsideKinds() {
        Map<PlantKind, Map<String, Long>> grouped = PlantStreamQueries.countByKindAndSpecies(plants);

        assertEquals(Map.of("Листяні", 1L, "Хвойні", 2L), grouped.get(PlantKind.TREE));
        assertEquals(Map.of("Квіти", 2L, "Орхідеї", 1L, "Сукуленти", 1L), grouped.get(PlantKind.FLOWER));
    }

    /* Порожній вхід: жодних винятків і зрозумілі значення. */
    @Test
    void emptyInputGivesEmptyResults() {
        List<Plant> empty = List.of();

        assertTrue(PlantStreamQueries.frequentlyWatered(empty).isEmpty());
        assertTrue(PlantStreamQueries.speciesNames(empty).isEmpty());
        assertTrue(PlantStreamQueries.averagePriceByKind(empty).isEmpty());
        assertEquals(0, PlantStreamQueries.heightStatistics(empty).getCount());
        assertTrue(PlantStreamQueries.topByHeight(empty, 5).isEmpty());
        assertTrue(PlantStreamQueries.findBySpecies(empty, "Хвойні").isEmpty());
        assertTrue(PlantStreamQueries.countByKindAndSpecies(empty).isEmpty());
    }

    /* Запити відхиляють null-список. */
    @Test
    void queriesRejectNullList() {
        assertThrows(NullPointerException.class, () -> PlantStreamQueries.frequentlyWatered(null));
        assertThrows(NullPointerException.class, () -> PlantStreamQueries.heightStatistics(null));
    }

    /* Доводить відсутність мутації вхідної колекції та незмінність результатів toList(). */
    @Test
    void queriesDoNotMutateTheInputList() {
        List<Plant> source = new ArrayList<>(plants);
        List<Plant> before = List.copyOf(source);

        PlantStreamQueries.frequentlyWatered(source);
        PlantStreamQueries.speciesNames(source);
        PlantStreamQueries.averagePriceByKind(source);
        PlantStreamQueries.heightStatistics(source);
        List<Plant> top = PlantStreamQueries.topByHeight(source, 3);
        PlantStreamQueries.findBySpecies(source, "Хвойні");
        PlantStreamQueries.countByKindAndSpecies(source);

        assertEquals(before, source);
        assertThrows(UnsupportedOperationException.class, () -> top.add(source.get(0)));
    }

    /* Еквівалентність: потокові запити дають ті самі результати, що й еталонні цикли. */
    @Test
    void streamQueriesMatchTheLegacyLoops() {
        List<Plant> legacyFrequent = new ArrayList<>();
        TreeSet<String> legacySpecies = new TreeSet<>();
        Map<PlantKind, Double> priceSum = new EnumMap<>(PlantKind.class);
        Map<PlantKind, Integer> priceCount = new EnumMap<>(PlantKind.class);
        double sum = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        Plant legacyFirst = null;
        for (Plant plant : plants) {
            if (plant.wateringDays() <= PlantStreamQueries.FREQUENT_WATERING_DAYS) {
                legacyFrequent.add(plant);
            }
            legacySpecies.add(plant.species());
            priceSum.merge(plant.kind(), plant.price(), Double::sum);
            priceCount.merge(plant.kind(), 1, Integer::sum);
            sum += plant.heightCm();
            min = Math.min(min, plant.heightCm());
            max = Math.max(max, plant.heightCm());
            if (legacyFirst == null && plant.species().equals("Хвойні")) {
                legacyFirst = plant;
            }
        }
        List<Plant> legacySorted = new ArrayList<>(plants);
        legacySorted.sort((first, second) -> {
            int byHeight = Double.compare(second.heightCm(), first.heightCm());
            if (byHeight != 0) {
                return byHeight;
            }
            int bySpecies = first.species().compareTo(second.species());
            return bySpecies != 0 ? bySpecies : first.name().compareTo(second.name());
        });
        List<Plant> legacyTop = new ArrayList<>();
        for (int index = 0; index < Math.min(5, legacySorted.size()); index++) {
            legacyTop.add(legacySorted.get(index));
        }

        assertEquals(legacyFrequent, PlantStreamQueries.frequentlyWatered(plants));
        assertEquals(List.copyOf(legacySpecies), PlantStreamQueries.speciesNames(plants));
        Map<PlantKind, Double> averages = PlantStreamQueries.averagePriceByKind(plants);
        for (PlantKind kind : priceSum.keySet()) {
            assertEquals(priceSum.get(kind) / priceCount.get(kind), averages.get(kind), DELTA);
        }
        DoubleSummaryStatistics statistics = PlantStreamQueries.heightStatistics(plants);
        assertEquals(sum, statistics.getSum(), DELTA);
        assertEquals(min, statistics.getMin(), DELTA);
        assertEquals(max, statistics.getMax(), DELTA);
        assertEquals(legacyTop, PlantStreamQueries.topByHeight(plants, 5));
        assertEquals(legacyFirst, PlantStreamQueries.findBySpecies(plants, "Хвойні").orElseThrow());
        Map<PlantKind, Map<String, Long>> legacyGroups = new EnumMap<>(PlantKind.class);
        for (Plant plant : plants) {
            legacyGroups.computeIfAbsent(plant.kind(), kind -> new TreeMap<>()).merge(plant.species(), 1L, Long::sum);
        }
        assertEquals(legacyGroups, PlantStreamQueries.countByKindAndSpecies(plants));
    }
}
