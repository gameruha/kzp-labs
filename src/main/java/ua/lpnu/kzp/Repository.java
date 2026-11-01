package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Type-safe in-memory storage of domain records without raw types.
 *
 * @param <T> type of elements stored in the repository
 */
public final class Repository<T> {

    private final List<T> items = new ArrayList<>();

    /** Creates an empty repository. */
    public Repository() {
    }

    /**
     * Adds a non-null element to the repository.
     *
     * @param item element to add
     * @throws NullPointerException if {@code item} is null
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "Запис не може бути null"));
    }

    /**
     * Returns an immutable snapshot of all stored elements.
     *
     * @return unmodifiable copy of the repository contents
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Returns an immutable list of elements matching the given condition.
     *
     * @param condition filter predicate accepting {@code T} or its supertype
     * @return matching elements in insertion order
     * @throws NullPointerException if {@code condition} is null
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "Умова не може бути null");
        return items.stream()
                .filter(condition)
                .toList();
    }
}
