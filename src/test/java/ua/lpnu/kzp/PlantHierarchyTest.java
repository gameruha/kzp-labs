package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlantHierarchyTest {

    private static final double DELTA = 1e-9;

    /* Перевіряє, що фабрика обирає підтип за видом рослини. */
    @Test
    void factoryCreatesSubtypeMatchingTheSpecies() {
        Plant tree = Plant.fromCsv("Хвойні;Туя;120.5;450.0;7");
        Plant flower = Plant.fromCsv("Квіти;Троянда;60.0;150.0;5");

        assertInstanceOf(Tree.class, tree);
        assertInstanceOf(Flower.class, flower);
        assertEquals(PlantKind.TREE, tree.kind());
        assertEquals(PlantKind.FLOWER, flower.kind());
    }

    /* Перевіряє різні результати поліморфного методу для різних підтипів. */
    @Test
    void subtypesCalculateCareNeedDifferently() {
        Plant tree = new Tree("Листяні", "Клен", 200.0, 100.0, 14);
        Plant flower = new Flower("Квіти", "Троянда", 60.0, 150.0, 14);

        assertEquals(1.5, tree.careNeed(), DELTA);
        assertEquals(1.0, flower.careNeed(), DELTA);
        assertNotEquals(tree.careNeed(), flower.careNeed());
    }

    /* Перевіряє обробку різнорідних об'єктів через колекцію спільного типу. */
    @Test
    void polymorphicCollectionUsesTheSubtypeImplementation() {
        List<Plant> plants = List.of(
                new Tree("Листяні", "Клен", 200.0, 100.0, 14),
                new Flower("Квіти", "Троянда", 60.0, 150.0, 14));

        double total = plants.stream().mapToDouble(Plant::careNeed).sum();

        assertEquals(2.5, total, DELTA);
    }

    /* Базовий тип відхиляє неправильний спільний стан через конструктор будь-якого підтипу. */
    @Test
    void baseTypeRejectsInvalidCommonState() {
        assertThrows(NullPointerException.class, () -> new Tree(null, "Клен", 100.0, 1.0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Tree("Листяні", " ", 100.0, 1.0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Tree("Листяні", "Клен", -1.0, 1.0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Flower("Квіти", "Роза", 10.0, -1.0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Flower("Квіти", "Роза", 10.0, 1.0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Flower("Квіти", "Роза", Double.POSITIVE_INFINITY, 1.0, 5));
    }

    /* Кожен підтип приймає граничні коректні значення. */
    @Test
    void subtypesAcceptTheirBoundaryValues() {
        assertEquals(10_000.0, new Tree("Листяні", "Секвоя", 10_000.0, 0.0, 60).heightCm(), DELTA);
        assertEquals(200.0, new Flower("Квіти", "Соняшник", 200.0, 0.0, 30).heightCm(), DELTA);
    }

    /* Кожен підтип відхиляє власні хибні значення. */
    @Test
    void subtypesRejectTheirOwnInvalidFields() {
        assertThrows(IllegalArgumentException.class, () -> new Tree("Листяні", "Клен", 10_000.1, 1.0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Tree("Листяні", "Клен", 100.0, 1.0, 61));
        assertThrows(IllegalArgumentException.class, () -> new Flower("Квіти", "Роза", 200.1, 1.0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Flower("Квіти", "Роза", 10.0, 1.0, 31));
        assertThrows(IllegalArgumentException.class, () -> Plant.fromCsv("Квіти;Гігант;500;1;5"));
    }

    /* Рівні об'єкти мають однаковий hashCode. */
    @Test
    void equalPlantsHaveEqualHashCodes() {
        Plant first = new Tree("Хвойні", "Туя", 120.0, 450.0, 7);
        Plant second = new Tree("Хвойні", "Туя", 150.0, 500.0, 10);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    /* HashSet не дублює логічно рівні об'єкти, HashMap знаходить їх за ключем. */
    @Test
    void hashSetAndHashMapTreatEqualPlantsAsOne() {
        Set<Plant> plants = new HashSet<>();
        plants.add(new Tree("Хвойні", "Туя", 120.0, 450.0, 7));
        plants.add(new Tree("Хвойні", "Туя", 120.0, 450.0, 7));
        Map<Plant, Integer> stock = new HashMap<>();
        stock.put(new Flower("Квіти", "Троянда", 60.0, 150.0, 5), 10);

        assertEquals(1, plants.size());
        assertEquals(10, stock.get(new Flower("Квіти", "Троянда", 60.0, 150.0, 5)));
    }

    /* Різні підтипи не стають рівними лише через однакові вид і назву. */
    @Test
    void differentSubtypesWithSameSpeciesAndNameAreNotEqual() {
        Plant tree = new Tree("Мікс", "Зразок", 100.0, 10.0, 5);
        Plant flower = new Flower("Мікс", "Зразок", 100.0, 10.0, 5);

        assertNotEquals(tree, flower);
        assertEquals(2, new HashSet<>(List.of(tree, flower)).size());
    }

    /* Switch зі зіставленням зі зразком охоплює обидва підтипи без default. */
    @Test
    void placementSwitchHandlesEverySealedSubtype() {
        assertTrue(PlantPlacement.recommend(new Tree("Листяні", "Клен", 250.0, 1.0, 14))
                .contains("відкритий ґрунт"));
        assertTrue(PlantPlacement.recommend(new Flower("Квіти", "Троянда", 60.0, 1.0, 5))
                .contains("клумба"));
    }

    /* Перевіряє enum: підписи та визначення категорії за видом. */
    @Test
    void plantKindHasLabelsAndResolvesCategoryFromSpecies() {
        assertEquals("дерево", PlantKind.TREE.label());
        assertEquals("квітка", PlantKind.FLOWER.label());
        assertEquals(PlantKind.TREE, PlantKind.fromSpecies(" ХВОЙНІ "));
        assertEquals(PlantKind.TREE, PlantKind.fromSpecies("Листяні"));
        assertEquals(PlantKind.FLOWER, PlantKind.fromSpecies("Сукуленти"));
        assertEquals(PlantKind.FLOWER, PlantKind.fromSpecies("A"));
    }
}
