package ua.lpnu.kzp;

import java.nio.file.Path;
import java.util.List;

/**
 * Common contract for exporting objects whose fields are annotated with {@link CsvColumn}.
 *
 * @param <T> type of exported records
 */
public interface Exporter<T> {

    /**
     * Writes the annotated fields of {@code records} to {@code path} in UTF-8.
     *
     * @param path destination file path
     * @param type class of exported records
     * @param records records to export
     * @throws DataStorageException if reflection fails or the file cannot be written
     */
    void export(Path path, Class<T> type, List<? extends T> records) throws DataStorageException;
}
