package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

public class MainTest {

    @Test
    public void testPlantCreation() {
        Main.Plant plant = new Main.Plant("Хвойні", "Туя", 120.0, 450.0, 7);
        assertEquals("Туя", plant.name);
        assertEquals(120.0, plant.heightCm);
        assertEquals(450.0, plant.price);
        assertEquals(7, plant.wateringDays);
    }

    @Test
    public void testCalculationsLogic() {
        List<Main.Plant> plants = Arrays.asList(
            new Main.Plant("А", "Рослина 1", 100.0, 200.0, 5),
            new Main.Plant("Б", "Рослина 2", 200.0, 500.0, 10)
        );

        double avgHeight = plants.stream().mapToDouble(p -> p.heightCm).average().orElse(0.0);
        assertEquals(150.0, avgHeight);

        Main.Plant expensive = plants.stream().max((p1, p2) -> Double.compare(p1.price, p2.price)).orElse(null);
        assertNotNull(expensive);
        assertEquals("Рослина 2", expensive.name);
    }
}