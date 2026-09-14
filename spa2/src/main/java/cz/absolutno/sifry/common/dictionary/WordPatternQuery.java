package cz.absolutno.sifry.common.dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Structured word constraint: positions are 1-based over the lowercase canon
 * key. A {@code length} is required whenever any position-based constraint
 * (fixed letters, equality/inequality groups, all-different flags) is used.
 */
public final class WordPatternQuery {

    private final Integer length;
    private final Map<Integer, Character> fixedLetters;
    private final List<TreeSet<Integer>> equalityGroups;
    private final List<TreeSet<Integer>> inequalityGroups;
    private final boolean allLettersDifferent;
    private final boolean allUnmatchedLettersDifferent;
    private final String prefix;
    private final String suffix;
    private final String contains;

    private WordPatternQuery(Builder b) {
        this.length = b.length;
        this.fixedLetters = Collections.unmodifiableMap(new TreeMap<Integer, Character>(b.fixedLetters));
        this.equalityGroups = copyGroups(b.equalityGroups);
        this.inequalityGroups = copyGroups(b.inequalityGroups);
        this.allLettersDifferent = b.allLettersDifferent;
        this.allUnmatchedLettersDifferent = b.allUnmatchedLettersDifferent;
        this.prefix = b.prefix;
        this.suffix = b.suffix;
        this.contains = b.contains;
    }

    private static List<TreeSet<Integer>> copyGroups(List<TreeSet<Integer>> groups) {
        List<TreeSet<Integer>> copy = new ArrayList<TreeSet<Integer>>();
        for (TreeSet<Integer> g : groups)
            copy.add(new TreeSet<Integer>(g));
        return Collections.unmodifiableList(copy);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getLength() {
        return length;
    }

    public Map<Integer, Character> getFixedLetters() {
        return fixedLetters;
    }

    public List<TreeSet<Integer>> getEqualityGroups() {
        return equalityGroups;
    }

    public List<TreeSet<Integer>> getInequalityGroups() {
        return inequalityGroups;
    }

    public boolean isAllLettersDifferent() {
        return allLettersDifferent;
    }

    public boolean isAllUnmatchedLettersDifferent() {
        return allUnmatchedLettersDifferent;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getContains() {
        return contains;
    }

    public boolean hasAnyPositionalConstraint() {
        return !fixedLetters.isEmpty() || !equalityGroups.isEmpty() || !inequalityGroups.isEmpty()
                || allLettersDifferent || allUnmatchedLettersDifferent;
    }

    public boolean isEmpty() {
        return length == null && !hasAnyPositionalConstraint()
                && prefix == null && suffix == null && contains == null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WordPatternQuery{");
        if (length != null)
            sb.append("len=").append(length).append(' ');
        for (Map.Entry<Integer, Character> e : fixedLetters.entrySet())
            sb.append("p").append(e.getKey()).append("='").append(e.getValue()).append("' ");
        if (allLettersDifferent)
            sb.append("allDiff ");
        if (allUnmatchedLettersDifferent)
            sb.append("unmatchedDiff ");
        if (prefix != null)
            sb.append("prefix='").append(prefix).append("' ");
        if (suffix != null)
            sb.append("suffix='").append(suffix).append("' ");
        if (contains != null)
            sb.append("contains='").append(contains).append("' ");
        return sb.append('}').toString();
    }

    public static final class Builder {

        private Integer length;
        private final Map<Integer, Character> fixedLetters = new TreeMap<Integer, Character>();
        private final List<TreeSet<Integer>> equalityGroups = new ArrayList<TreeSet<Integer>>();
        private final List<TreeSet<Integer>> inequalityGroups = new ArrayList<TreeSet<Integer>>();
        private boolean allLettersDifferent;
        private boolean allUnmatchedLettersDifferent;
        private String prefix;
        private String suffix;
        private String contains;

        public Builder length(int n) {
            this.length = Integer.valueOf(n);
            return this;
        }

        public Builder fixedLetter(int position, char letter) {
            fixedLetters.put(Integer.valueOf(position), Character.toLowerCase(letter));
            return this;
        }

        public Builder equalityGroup(int... positions) {
            TreeSet<Integer> group = new TreeSet<Integer>();
            for (int p : positions)
                group.add(Integer.valueOf(p));
            equalityGroups.add(group);
            return this;
        }

        public Builder inequalityGroup(int... positions) {
            TreeSet<Integer> group = new TreeSet<Integer>();
            for (int p : positions)
                group.add(Integer.valueOf(p));
            inequalityGroups.add(group);
            return this;
        }

        public Builder allLettersDifferent(boolean b) {
            this.allLettersDifferent = b;
            return this;
        }

        public Builder allUnmatchedLettersDifferent(boolean b) {
            this.allUnmatchedLettersDifferent = b;
            return this;
        }

        public Builder prefix(String s) {
            this.prefix = s;
            return this;
        }

        public Builder suffix(String s) {
            this.suffix = s;
            return this;
        }

        public Builder contains(String s) {
            this.contains = s;
            return this;
        }

        public WordPatternQuery build() {
            boolean positional = !fixedLetters.isEmpty() || !equalityGroups.isEmpty()
                    || !inequalityGroups.isEmpty() || allLettersDifferent || allUnmatchedLettersDifferent;
            if (positional && length == null)
                throw new IllegalStateException("length is required for position-based constraints");
            return new WordPatternQuery(this);
        }
    }

}