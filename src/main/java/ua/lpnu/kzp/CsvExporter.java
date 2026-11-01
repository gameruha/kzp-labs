package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Universal reflective CSV exporter that writes fields annotated with {@link CsvColumn} in UTF-8
 * with deterministic column ordering, RFC-4180 escaping, inheritance traversal and field caching.
 *
 * @param <T> type of exported records
 */
public final class CsvExporter<T> implements Exporter<T> {

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /** Creates a CSV exporter instance implementing {@link Exporter}. */
    public CsvExporter() {
    }

    @Override
    public void export(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        write(path, type, records);
    }

    /**
     * Writes the {@link CsvColumn}-annotated fields of {@code records} to {@code path} in UTF-8,
     * using the cached reflective field metadata for {@code type}.
     *
     * @param <T> type of records
     * @param path destination CSV path
     * @param type class of records
     * @param records list of records to export
     * @throws DataStorageException if reflection or file writing fails
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        writeInternal(path, type, records, true);
    }

    /**
     * Writes the {@link CsvColumn}-annotated fields without using the reflective field cache
     * (used to compare cached vs uncached export performance).
     *
     * @param <T> type of records
     * @param path destination CSV path
     * @param type class of records
     * @param records list of records to export
     * @throws DataStorageException if reflection or file writing fails
     */
    public static <T> void writeUncached(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        writeInternal(path, type, records, false);
    }

    private static <T> void writeInternal(
            Path path, Class<T> type, List<? extends T> records, boolean useCache)
            throws DataStorageException {
        Objects.requireNonNull(path, "Шлях не може бути null");
        Objects.requireNonNull(type, "Тип не може бути null");
        Objects.requireNonNull(records, "Записи не можуть бути null");
        List<Field> fields = useCache ? annotatedFields(type) : discoverAnnotatedFields(type);
        // Заголовок і значення будуються з однакового, детерміновано відсортованого набору полів.
        String header = fields.stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(","));
        StringBuilder csv = new StringBuilder(header)
                .append(System.lineSeparator());
        for (T record : records) {
            if (record == null) {
                throw new DataStorageException("Список містить null-запис");
            }
            List<String> values = new ArrayList<>(fields.size());
            for (Field field : fields) {
                try {
                    values.add(escape(field.get(record)));
                } catch (IllegalAccessException exception) {
                    throw new DataStorageException(
                            "Не вдалося прочитати поле %s".formatted(field.getName()), exception);
                }
            }
            csv.append(String.join(",", values))
                    .append(System.lineSeparator());
        }
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, csv.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException(
                    "Не вдалося записати CSV: %s".formatted(path), exception);
        }
    }

    /**
     * Returns the CSV header line for the given type in deterministic field-name order.
     *
     * @param type class containing {@link CsvColumn} fields
     * @return comma-separated header line
     * @throws DataStorageException if the class has no annotated fields or access is denied
     */
    public static String headerFor(Class<?> type) throws DataStorageException {
        Objects.requireNonNull(type, "Тип не може бути null");
        return annotatedFields(type).stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(","));
    }

    /**
     * Returns the accessible {@link CsvColumn}-annotated fields of {@code type} (including
     * superclasses) sorted by Java field name, using an in-memory cache.
     *
     * @param type class to inspect
     * @return immutable list of accessible annotated fields
     * @throws DataStorageException if the class has no annotated fields or access is denied
     */
    static List<Field> annotatedFields(Class<?> type) throws DataStorageException {
        List<Field> cached = FIELD_CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        List<Field> discovered = discoverAnnotatedFields(type);
        FIELD_CACHE.put(type, discovered);
        return discovered;
    }

    /** Discovers and validates annotated fields across the inheritance hierarchy without caching. */
    static List<Field> discoverAnnotatedFields(Class<?> type) throws DataStorageException {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type;
                current != null && current != Object.class;
                current = current.getSuperclass()) {
            fields.addAll(Arrays.stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(CsvColumn.class))
                    .toList());
        }
        if (fields.isEmpty()) {
            throw new DataStorageException("Клас не має CSV-полів: %s".formatted(type.getSimpleName()));
        }
        fields.sort(Comparator.comparing(Field::getName));
        for (Field field : fields) {
            if (!field.trySetAccessible()) {
                throw new DataStorageException(
                        "Немає доступу до поля %s".formatted(field.getName()));
            }
        }
        return List.copyOf(fields);
    }

    /** Clears the reflective field cache. */
    static void clearCache() {
        FIELD_CACHE.clear();
    }

    /**
     * Escapes a field value according to CSV rules: wraps in double quotes if it contains a comma,
     * double quote or line break, and doubles any internal double quotes.
     *
     * @param value field value to escape
     * @return escaped CSV representation
     */
    public static String escape(Object value) {
        String text = Objects.toString(value, "");
        boolean quoted = text.contains(",")
                || text.contains("\"")
                || text.contains("\r")
                || text.contains("\n");
        return quoted
                ? "\"%s\"".formatted(text.replace("\"", "\"\""))
                : text;
    }
}
