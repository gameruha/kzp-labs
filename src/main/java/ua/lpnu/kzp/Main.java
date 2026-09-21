package ua.lpnu.kzp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {

    // Клас для зберігання даних про рослину
    public static class Plant {
        String species;
        String name;
        double heightCm;
        double price;
        int wateringDays;

        public Plant(String species, String name, double heightCm, double price, int wateringDays) {
            this.species = species;
            this.name = name;
            this.heightCm = heightCm;
            this.price = price;
            this.wateringDays = wateringDays;
        }
    }

    public static void main(String[] args) {
        String filePath = "data/input.csv";
        List<Plant> plants = new ArrayList<>();
        int validRows = 0;
        int invalidRows = 0;

        System.out.println("=== Обробка даних розсадника рослин ===");

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                // Пропускаємо шапку таблиці
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(";");
                
                // Перевірка на кількість полів (має бути рівно 5)
                if (parts.length != 5) {
                    invalidRows++;
                    continue;
                }

                try {
                    String species = parts[0].trim();
                    String name = parts[1].trim();
                    // Замінюємо кома на крапку на випадок введення з комами
                    double heightCm = Double.parseDouble(parts[2].trim().replace(",", "."));
                    double price = Double.parseDouble(parts[3].trim().replace(",", "."));
                    int wateringDays = Integer.parseInt(parts[4].trim());

                    // Валідація: поля не пусті, числові значення коректні
                    if (species.isEmpty() || name.isEmpty() || heightCm <= 0 || price < 0 || wateringDays <= 0) {
                        invalidRows++;
                        continue;
                    }

                    plants.add(new Plant(species, name, heightCm, price, wateringDays));
                    validRows++;

                } catch (NumberFormatException e) {
                    // Якщо не вдалося спарсити числа
                    invalidRows++;
                }
            }

        } catch (IOException e) {
            System.out.println("Помилка читання файлу: " + e.getMessage());
        }

        // Обчислення показників для 12 варіанта
        int totalPlants = plants.size();
        double avgHeight = totalPlants > 0 ? plants.stream().mapToDouble(p -> p.heightCm).average().orElse(0.0) : 0;
        
        Plant mostExpensive = plants.stream().max(Comparator.comparingDouble(p -> p.price)).orElse(null);
        int minWateringDays = plants.stream().mapToInt(p -> p.wateringDays).min().orElse(0);

        // Виведення результатів
        System.out.println("Оброблено коректних рядків: " + validRows);
        System.out.println("Пропущено рядків з помилками: " + invalidRows);
        System.out.println("----------------------------------------");
        System.out.println("1. Загальна кількість рослин: " + totalPlants);
        System.out.println("2. Середня висота рослин: " + String.format("%.2f", avgHeight) + " см");
        System.out.println("3. Найдорожча рослина: " + (mostExpensive != null ? mostExpensive.name + " (" + mostExpensive.price + " грн)" : "немає"));
        System.out.println("4. Найменший інтервал поливу: " + minWateringDays + " днів");
    }
}