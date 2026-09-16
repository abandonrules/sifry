package cz.absolutno.sifry.common.datapack;

/**
 * One queryable field of a data pack, e.g. {@code orderFromSun} (numeric) or
 * {@code parentBody} (text). Describes the field to the Lookup UI so filters
 * can be rendered from pack metadata instead of per-source UI branching.
 */
public final class FieldDefinition {

    public enum Kind { TEXT, NUMERIC, DATE }

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_NUMBER = "numeric";
    public static final String TYPE_DATE = "date";

    private final String id;
    private final String displayName;
    private final Kind kind;

    public FieldDefinition(String id, String displayName, Kind kind) {
        this.id = id;
        this.displayName = displayName;
        this.kind = kind;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public String toString() {
        return id + " (" + kind + ')';
    }
}