package cz.absolutno.sifry.substituce.analysis;

/**
 * A half-open range {@code [start, end)} into a string. Pure value; no Android
 * dependencies.
 */
public final class TextRange {

    private final int start;
    private final int end;

    public TextRange(int start, int end) {
        if (start < 0)
            throw new IllegalArgumentException("start must be >= 0");
        if (end < start)
            throw new IllegalArgumentException("end must be >= start");
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int length() {
        return end - start;
    }

    public boolean isEmpty() {
        return start == end;
    }

    public boolean contains(int index) {
        return index >= start && index < end;
    }

    public TextRange shiftedBy(int delta) {
        return new TextRange(start + delta, end + delta);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TextRange))
            return false;
        TextRange r = (TextRange) o;
        return start == r.start && end == r.end;
    }

    @Override
    public int hashCode() {
        return 31 * start + end;
    }

    @Override
    public String toString() {
        return "TextRange[" + start + "," + end + ")";
    }
}
