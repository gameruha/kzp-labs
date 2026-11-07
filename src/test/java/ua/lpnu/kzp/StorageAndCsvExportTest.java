package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageAndCsvExportTest {

    @TempDir
    Path temporaryDirectory;

    private static final class UnannotatedItem {
        @SuppressWarnings("unused")
        private final String label = "none";
    }

    private static final class CustomCatalogItem {
        @CsvColumn("Код")
        private final String code;

        @CsvColumn("Кількість")
        private final int count;

        private CustomCatalogItem(String code, int count) {
            this.code = code;
            this.count = count;
        }
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }

    @Test
    void repositoryStoresItemsReturnsImmutableSnapshotAndFiltersWithSupertypePredicate() {
        Repository<Tree> treeRepository = new Repository<>();
        Tree thuja = new Tree("Хвойні", "Туя", 120.5, 450.0, 7);
        Tree maple = new Tree("Листяні", "Клен", 250.0, 1200.0, 14);
        treeRepository.add(thuja);
        treeRepository.add(maple);

        List<Tree> snapshot = treeRepository.all();
        assertEquals(2, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(thuja));

        Predicate<Plant> tallPlant = plant -> plant.heightCm() >= 200.0;
        assertEquals(List.of(maple), treeRepository.find(tallPlant));
        assertThrows(NullPointerException.class, () -> treeRepository.add(null));
        assertThrows(NullPointerException.class, () -> treeRepository.find(null));
    }

    @Test
    void csvExporterDeterministicallySortsColumnsEscapesSpecialCharactersAndCompletesRoundTrip()
            throws Exception {
        List<PlantRecord> original = List.of(
                new PlantRecord("Хвойні", 4, 120.5, 450.0, 7),
                new PlantRecord("Листяні", 8, 250.0, 1200.0, 14),
                new PlantRecord("Квіти", 2, 60.0, 150.0, 2),
                new PlantRecord("Сукуленти", 3, 60.0, 70.0, 1),
                new PlantRecord("Хвойні, декоративні \"Альпійські\"", 6, 180.0, 950.0, 5));
        Path csvPath = temporaryDirectory.resolve("plants.csv");

        CsvExporter.write(csvPath, PlantRecord.class, original);

        String csvText = Files.readString(csvPath, StandardCharsets.UTF_8);
        List<List<String>> rows = CsvParser.parseDocument(csvText);
        List<PlantRecord> restored = PlantRecord.readCsv(csvPath);

        assertEquals(6, rows.size());
        assertEquals("вид,вік,висота,ціна,полив", String.join(",", rows.get(0)));
        assertTrue(csvText.contains("\"Хвойні, декоративні \"\"Альпійські\"\"\""));
        assertEquals(original, restored);
        assertEquals("Хвойні:4", restored.get(0).roundTripKey());
        assertEquals(PlantKind.TREE, restored.get(0).kind());
    }

    @Test
    void roundTripPreservesMultilineQuotedFieldAndCountsPhysicalLinesVsRecords() throws Exception {
        List<PlantRecord> original = List.of(
                new PlantRecord("Квіти\nОранжерейні, \"Екзотика\"", 2, 55.0, 320.0, 3),
                new PlantRecord("Хвойні", 5, 150.0, 600.0, 10));
        Path csvPath = temporaryDirectory.resolve("multiline.csv");

        CsvExporter.write(csvPath, PlantRecord.class, original);

        String rawCsv = Files.readString(csvPath, StandardCharsets.UTF_8);
        long physicalLines = normalize(rawCsv).lines().count();
        List<List<String>> parsedRecords = CsvParser.parseDocument(rawCsv);
        List<PlantRecord> restored = PlantRecord.readCsv(csvPath);

        assertEquals(4L, physicalLines);
        assertEquals(3, parsedRecords.size());
        assertEquals(original, restored);
    }

    @Test
    void exporterWorksWithArbitraryAnnotatedClassAndInheritedPlantFields() throws Exception {
        Path customPath = temporaryDirectory.resolve("custom.csv");
        CsvExporter.write(customPath, CustomCatalogItem.class,
                List.of(new CustomCatalogItem("A,01", 12)));
        List<List<String>> customRows = CsvParser.parseDocument(
                Files.readString(customPath, StandardCharsets.UTF_8));
        assertEquals(List.of("Код", "Кількість"), customRows.get(0));
        assertEquals(List.of("A,01", "12"), customRows.get(1));

        Path treePath = temporaryDirectory.resolve("trees.csv");
        CsvExporter.write(treePath, Tree.class,
                List.of(new Tree("Хвойні", "Туя", 4, 120.5, 450.0, 7)));
        assertEquals("вид,вік,висота,ціна,полив", CsvExporter.headerFor(Tree.class));
    }

    @Test
    void readsControlDataFilesForValidEmptyAndInvalidCases() throws Exception {
        List<PlantRecord> validRecords = PlantRecord.readCsv(Path.of("data", "lab05-records.csv"));
        assertEquals(5, validRecords.size());
        assertEquals("Хвойні, декоративні \"Альпійські\"", validRecords.get(4).species());

        List<PlantRecord> emptyRecords = PlantRecord.readCsv(Path.of("data", "lab05-empty.csv"));
        assertTrue(emptyRecords.isEmpty());

        Path exportedEmpty = temporaryDirectory.resolve("empty.csv");
        CsvExporter.write(exportedEmpty, PlantRecord.class, List.of());
        assertTrue(PlantRecord.readCsv(exportedEmpty).isEmpty());

        DataStorageException invalidException = assertThrows(DataStorageException.class,
                () -> PlantRecord.readCsv(Path.of("data", "lab05-invalid.csv")));
        assertTrue(invalidException.getCause() instanceof NumberFormatException);
    }

    @Test
    void distinguishesModelFormatValueFileAndReflectionErrorLevelsWithCause() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new PlantRecord(" ", 3, 100.0, 200.0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new PlantRecord("Хвойні", 0, 100.0, 200.0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new PlantRecord("Хвойні", 3, -10.0, 200.0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new PlantRecord("Хвойні", 3, 100.0, -1.0, 5));
        assertThrows(IllegalArgumentException.class,
                () -> new PlantRecord("Хвойні", 3, 100.0, 200.0, 0));

        assertThrows(DataStorageException.class,
                () -> PlantRecord.fromCsv("4,120.5,450.0"));
        assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("\"Незакриті лапки"));
        assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument("\"Закриті\"хвіст,10"));
        assertThrows(DataStorageException.class,
                () -> CsvParser.parseDocument(null));

        DataStorageException numberException = assertThrows(DataStorageException.class,
                () -> PlantRecord.fromCsv("Хвойні,не_число,120.5,450.0,7"));
        assertTrue(numberException.getCause() instanceof NumberFormatException);

        assertEquals(PlantKind.TREE, PlantRecord.parseKind("TREE"));
        DataStorageException enumException = assertThrows(DataStorageException.class,
                () -> PlantRecord.parseKind("UNKNOWN_KIND"));
        assertTrue(enumException.getCause() instanceof IllegalArgumentException);

        Path directoryTarget = temporaryDirectory.resolve("directory-not-file");
        Files.createDirectory(directoryTarget);
        DataStorageException writeFileException = assertThrows(DataStorageException.class,
                () -> CsvExporter.write(directoryTarget, PlantRecord.class, List.of()));
        assertTrue(writeFileException.getCause() instanceof IOException);

        DataStorageException readFileException = assertThrows(DataStorageException.class,
                () -> PlantRecord.readCsv(temporaryDirectory.resolve("missing.csv")));
        assertTrue(readFileException.getCause() instanceof IOException);

        assertThrows(DataStorageException.class,
                () -> CsvExporter.write(temporaryDirectory.resolve("unannotated.csv"),
                        UnannotatedItem.class, List.of(new UnannotatedItem())));
    }

    @Test
    void supportsJsonExporterThroughSharedInterfaceAndCachesReflectiveFields() throws Exception {
        List<PlantRecord> records = List.of(
                new PlantRecord("Хвойні, \"Туя\"", 4, 120.5, 450.0, 7),
                new PlantRecord("Квіти", 2, 60.0, 150.0, 2));
        Path csvPath = temporaryDirectory.resolve("via-interface.csv");
        Path jsonPath = temporaryDirectory.resolve("via-interface.json");

        Exporter<PlantRecord> csvExporter = new CsvExporter<>();
        Exporter<PlantRecord> jsonExporter = new JsonExporter<>();
        csvExporter.export(csvPath, PlantRecord.class, records);
        jsonExporter.export(jsonPath, PlantRecord.class, records);

        String json = Files.readString(jsonPath, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"вік\": 4"));
        assertTrue(json.contains("\"вид\": \"Хвойні, \\\"Туя\\\"\""));
        assertEquals(records, PlantRecord.readCsv(csvPath));

        CsvExporter.clearCache();
        List<Field> firstLookup = CsvExporter.annotatedFields(PlantRecord.class);
        List<Field> secondLookup = CsvExporter.annotatedFields(PlantRecord.class);
        assertSame(firstLookup, secondLookup);

        Path cachedBenchPath = temporaryDirectory.resolve("cached-bench.csv");
        Path uncachedBenchPath = temporaryDirectory.resolve("uncached-bench.csv");
        long uncachedStart = System.nanoTime();
        for (int index = 0; index < 200; index++) {
            CsvExporter.writeUncached(uncachedBenchPath, PlantRecord.class, records);
        }
        long uncachedDuration = System.nanoTime() - uncachedStart;

        long cachedStart = System.nanoTime();
        for (int index = 0; index < 200; index++) {
            CsvExporter.write(cachedBenchPath, PlantRecord.class, records);
        }
        long cachedDuration = System.nanoTime() - cachedStart;

        assertTrue(uncachedDuration > 0 && cachedDuration > 0);
        assertNotNull(Files.readString(cachedBenchPath, StandardCharsets.UTF_8));
    }

    @Test
    void newReportStartsWithTheLab4ReportForTheControlFile() throws IOException {
        String expected = normalize(Files.readString(Path.of("data", "lab04-expected-report.txt"),
                StandardCharsets.UTF_8));
        Analysis analysis = PlantAnalyzer.analyze(
                Files.readAllLines(Path.of("data", "input.csv"), StandardCharsets.UTF_8));

        String actual = normalize(ReportFormatter.format(analysis));

        assertTrue(actual.startsWith(expected));
        assertTrue(actual.contains("Записів у Repository<PlantRecord>: 3\n"));
        assertTrue(actual.contains("CSV-заголовок PlantRecord: вид,вік,висота,ціна,полив\n"));
    }
}
