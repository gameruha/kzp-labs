package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Second export format (star task) implementing {@link Exporter} to serialize objects with
 * {@link CsvColumn}-annotated fields into a UTF-8 JSON array.
 *
 * @param <T> type of exported records
 */
public final class JsonExporter<T> implements Exporter<T> {

    /** Creates a JSON exporter instance implementing {@link Exporter}. */
    public JsonExporter() {
    }

    @Override
    public void export(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        write(path, type, records);
    }

    /**
     * Writes the {@link CsvColumn}-annotated fields of {@code records} as a UTF-8 JSON array.
     *
     * @param <T> type of records
     * @param path destination JSON path
     * @param type class of records
     * @param records list of records to export
     * @throws DataStorageException if reflection or file writing fails
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(path, "Шлях не може бути null");
        Objects.requireNonNull(type, "Тип не може бути null");
        Objects.requireNonNull(records, "Записи не можуть бути null");
        List<Field> fields = CsvExporter.annotatedFields(type);
        List<String> jsonObjects = new ArrayList<>(records.size());
        for (T record : records) {
            if (record == null) {
                throw new DataStorageException("Список містить null-запис");
            }
            List<String> properties = new ArrayList<>(fields.size());
            for (Field field : fields) {
                try {
                    String columnName = field.getAnnotation(CsvColumn.class).value();
                    Object value = field.get(record);
                    properties.add("\"%s\": %s".formatted(escapeJson(columnName), formatJsonValue(value)));
                } catch (IllegalAccessException exception) {
                    throw new DataStorageException(
                            "Не вдалося прочитати поле %s".formatted(field.getName()), exception);
                }
            }
            jsonObjects.add("  {%s}".formatted(String.join(", ", properties)));
        }
        String json = jsonObjects.isEmpty()
                ? "[]"
                : "[%s%s%s]".formatted(
                        System.lineSeparator(),
                        String.join(",%s".formatted(System.lineSeparator()), jsonObjects),
                        System.lineSeparator());
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException(
                    "Не вдалося записати JSON: %s".formatted(path), exception);
        }
    }

    private static String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"%s\"".formatted(escapeJson(value.toString()));
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
