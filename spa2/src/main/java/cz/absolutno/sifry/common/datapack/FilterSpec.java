package cz.absolutno.sifry.common.datapack;

/**
 * A deterministic filter over one field of a pack. {@code field} selects the
 * record property to match (null/empty = identity lookup over display name +
 * aliases + symbols); {@code op} is the comparison; {@code value} is the
 * search text and {@code value2} the optional upper bound for between.
 */
public final class FilterSpec {

    public enum Op { EQ, LT, GT, LE, GE, BETWEEN, CONTAINS }

    private final String field;
    private final Op op;
    private final String value;
    private final String value2;

    private FilterSpec(String field, Op op, String value, String value2) {
        this.field = field;
        this.op = op;
        this.value = value;
        this.value2 = value2;
    }

    /** Identity lookup: match display name / aliases / symbols. */
    public static FilterSpec identity(String value) {
        if (value == null)
            value = "";
        return new FilterSpec(null, Op.EQ, value.trim(), null);
    }

    /** Number/text filter against the named record property. */
    public static FilterSpec on(String field, Op op, String value, String value2) {
        if (field == null)
            throw new IllegalArgumentException("field must not be null");
        return new FilterSpec(field, op, value, value2);
    }

    public String field() {
        return field;
    }

    public Op op() {
        return op;
    }

    public String value() {
        return value;
    }

    public String value2() {
        return value2;
    }

    public boolean isIdentity() {
        return field == null || field.isEmpty();
    }

    @Override
    public String toString() {
        return isIdentity() ? value : field + ' ' + op + ' ' + value;
    }
}