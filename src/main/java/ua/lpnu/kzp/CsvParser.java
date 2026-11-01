package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/**
 * State-machine CSV parser that handles commas, escaped double quotes and line breaks inside
 * quoted fields.
 */
public final class CsvParser {

    private CsvParser() {
    }

    /**
     * Parses a single CSV record (which may contain multiline quoted fields).
     *
     * @param line CSV record text
     * @return immutable list of parsed field values
     * @throws DataStorageException if the line is null, malformed or does not contain exactly one record
     */
    public static List<String> parse(String line) throws DataStorageException {
        List<List<String>> records = parseDocument(line);
        if (records.size() != 1) {
            throw new DataStorageException("Очікується один CSV-рядок");
        }
        return records.get(0);
    }

    /**
     * Parses a complete CSV document into records and fields using a state machine.
     *
     * @param text full CSV document text in UTF-8
     * @return immutable list of records, where each record is an immutable list of fields
     * @throws DataStorageException if the CSV syntax is invalid (e.g. unclosed quotes or text after closing quotes)
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        if (text == null) {
            throw new DataStorageException("CSV-документ не може бути null");
        }
        if (text.isEmpty()) {
            return List.of();
        }
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;
        // quoted показує, що поточне поле перебуває всередині лапок,
        // а quoteClosed — що лапки вже закриті й наступним допустимим символом
        // може бути лише кома або кінець запису.
        for (int index = 0; index < text.length(); index++) {
            char symbol = text.charAt(index);
            if (symbol == '"' && !quoteClosed && !quoted && current.length() == 0) {
                quoted = true;
            } else if (symbol == '"' && quoted && index + 1 < text.length()
                    && text.charAt(index + 1) == '"') {
                current.append('"');
                index++;
            } else if (symbol == '"' && quoted) {
                quoted = false;
                quoteClosed = true;
            } else if (symbol == ',' && !quoted) {
                fields.add(current.toString());
                current.setLength(0);
                quoteClosed = false;
            } else if ((symbol == '\r' || symbol == '\n') && !quoted) {
                fields.add(current.toString());
                records.add(List.copyOf(fields));
                fields = new ArrayList<>();
                current.setLength(0);
                quoteClosed = false;
                if (symbol == '\r' && index + 1 < text.length()
                        && text.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                if (quoteClosed) {
                    throw new DataStorageException("Символ після закритих CSV-лапок");
                }
                current.append(symbol);
            }
        }
        if (quoted) {
            throw new DataStorageException("Незакриті CSV-лапки");
        }
        if (!fields.isEmpty() || current.length() > 0 || quoteClosed) {
            fields.add(current.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }
}
