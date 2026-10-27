package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Set;

/** Category of a nursery plant with a label for the report. */
public enum PlantKind {
    /** Tree: conifers, deciduous and fruit trees. */
    TREE("дерево"),
    /** Flower and any other decorative plant. */
    FLOWER("квітка");

    private static final Set<String> TREE_SPECIES = Set.of("хвойні", "листяні", "плодові", "дерева");

    private final String label;

    PlantKind(String label) {
        this.label = label;
    }

    /**
     * Returns the text representation of the category.
     *
     * @return category label for the report
     */
    public String label() {
        return label;
    }

    /**
     * Determines the category from the species column of the CSV file, which keeps the lab 1 format.
     *
     * @param species species text from the input data
     * @return {@link #TREE} for tree species, otherwise {@link #FLOWER}
     */
    public static PlantKind fromSpecies(String species) {
        return TREE_SPECIES.contains(species.trim().toLowerCase(Locale.ROOT)) ? TREE : FLOWER;
    }
}
