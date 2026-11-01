package ua.lpnu.kzp;

/**
 * Checked exception indicating an error during data storage, CSV/JSON parsing, file I/O or
 * reflective field access.
 */
public class DataStorageException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with an explanatory message.
     *
     * @param message human-readable description of the failed operation
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Creates an exception with an explanatory message and the underlying root cause.
     *
     * @param message human-readable description of the failed operation
     * @param cause original exception that caused the failure
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
