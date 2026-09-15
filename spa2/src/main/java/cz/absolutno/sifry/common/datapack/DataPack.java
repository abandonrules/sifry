package cz.absolutno.sifry.common.datapack;

/**
 * A deterministic lookup dataset. The pack describes its own fields/filters so
 * the Lookup UI can be rendered from metadata; records are searchable through
 * {@link #search(FilterSpec)}. Matching is mechanical — no ranking, no
 * inference beyond what the pack's data literally states.
 */
public interface DataPack {

    String id();

    String displayName();

    PackManifest manifest();

    java.util.List<FieldDefinition> fields();

    /**
     * Distinct non-empty values a field actually holds across the pack,
     * sorted (numbers numerically, text case-insensitively). Empty when the
     * field has no values — e.g. a numeric field that is a formula result.
     */
    java.util.List<String> distinctValues(String field);

    SearchResult search(FilterSpec spec);
}