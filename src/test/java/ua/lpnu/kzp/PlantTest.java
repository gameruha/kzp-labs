package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlantTest {

    @Test
    void createsPlantFromCsvAndKeepsZeroPriceBoundary() {
        Plant plant = Plant.fromCsv(" Хвойні ; Туя ; 1.5 ; 0 ; 1 ");

        assertEquals("Хвойні", plant.species());
        assertEquals("Туя", plant.name());
        assertEquals(1.5, plant.heightCm());
        assertEquals(0.0, plant.price());
        assertEquals(1, plant.wateringDays());
        assertEquals("Хвойні Туя (0.00 грн)", plant.toString());
    }

    @Test
    void rejectsEmptyNamesAndInvalidMeasurements() {
        assertThrows(NullPointerException.class,
            () -> new Plant(null, "Туя", 1.0, 0.0, 1));
        assertThrows(NullPointerException.class,
            () -> new Plant("Хвойні", null, 1.0, 0.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Plant(" ", "Туя", 1.0, 0.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Plant("Хвойні", "Туя", 0.0, 0.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Plant("Хвойні", "Туя", 1.0, -1.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Plant("Хвойні", "Туя", 1.0, 0.0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Plant("Хвойні", "Туя", Double.NaN, 0.0, 1));
    }

    @Test
    void rejectsMalformedCsvRows() {
        assertThrows(IllegalArgumentException.class, () -> Plant.fromCsv("Хвойні;Туя;1;0"));
        assertThrows(IllegalArgumentException.class, () -> Plant.fromCsv("Хвойні;Туя;помилка;0;1"));
        assertThrows(IllegalArgumentException.class, () -> Plant.fromCsv("Хвойні;;1;0;1"));
    }

    @Test
    void calculatesSummaryAndHandlesNoPlants() {
        Plant first = Plant.fromCsv("Хвойні;Туя;10;50;7");
        Plant second = Plant.fromCsv("Листяні;Клен;20;100;3");

        assertEquals(new PlantSummary(2, 15.0, second, 3), PlantSummary.from(List.of(first, second)));
        assertEquals(new PlantSummary(0, 0.0, null, 0), PlantSummary.from(List.of()));
        assertNull(PlantSummary.from(List.of()).mostExpensive());
        assertThrows(IllegalArgumentException.class,
            () -> new PlantSummary(-1, 0.0, null, 0));
    }
}