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

    SearchResult search(FilterSpec spec);
}