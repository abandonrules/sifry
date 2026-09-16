package cz.absolutno.sifry.common.dictionary;

import java.util.Map;
import java.util.Set;

/**
 * Compiles a {@link WordPatternQuery} into the anchored pattern used against
 * the canon key: the pattern is applied to the lowercase part of each
 * {@code key:value} line, and a trailing ':' pins the end of the key. Output is
 * plain PCRE (lookaheads and backreferences) so the existing native engine
 * can run it unchanged.
 *
 * <p>Two modes: with a length (positions pinned into a {@code [a-z]{L}} body
 * plus zero-width constraint lookaheads, all required positional features
 * available) and without (prefix/suffix/contains folded into an unbounded
 * body). Only ASCII word sources are in scope for now; extended alphabets
 * arrive with their own compilers.
 */
public final class DictionaryQueryCompiler {

    private DictionaryQueryCompiler() {
    }

    public static final String EMPTY_PATTERN = "";

    private static String esc(String s) {
        if (s == null || s.isEmpty())
            return s;
        return java.util.regex.Pattern.quote(s);
    }

    private static void checkPosition(int len, int position) {
        if (len > 0 && position > len)
            throw new IllegalArgumentException("position " + position + " exceeds length " + len);
        if (position < 1)
            throw new IllegalArgumentException("position must be >= 1");
    }

    /**
     * Exact word lookup, the direct spelling of the validation contract:
     * {@code ^word:} on WORD sources. Returns {@value #EMPTY_PATTERN} for a
     * blank input so callers can treat "nothing typed" as "no constraint".
     */
    public static String exactKeyPattern(String word) {
        if (word == null || word.isEmpty())
            return EMPTY_PATTERN;
        String w = word.toLowerCase().trim();
        if (w.isEmpty())
            return EMPTY_PATTERN;
        return "^" + esc(w) + ":";
    }

    public static String compile(WordPatternQuery query) {
        if (query == null || query.isEmpty())
            return EMPTY_PATTERN;
        if (query.getLength() == null)
            return compileUnbounded(query);
        return compileBounded(query);
    }

    private static String compileUnbounded(WordPatternQuery query) {
        StringBuilder body = new StringBuilder("^");
        String contains = DictionaryQueryNormalizer.canonicalFragment(query.getContains());
        if (contains != null && !contains.isEmpty())
            body.append("(?=.*").append(esc(contains)).append(')');
        String prefix = DictionaryQueryNormalizer.canonicalFragment(query.getPrefix());
        if (prefix != null && !prefix.isEmpty())
            body.append(esc(prefix));
        body.append("[a-z]*");
        String suffix = DictionaryQueryNormalizer.canonicalFragment(query.getSuffix());
        if (suffix != null && !suffix.isEmpty())
            body.append(esc(suffix));
        return body.append(':').toString();
    }

    private static String compileBounded(WordPatternQuery query) {
        int len = query.getLength().intValue();
        Map<Integer, Character> fixed = query.getFixedLetters();
        for (Integer p : fixed.keySet())
            checkPosition(len, p.intValue());
        for (Set<Integer> group : query.getEqualityGroups())
            checkGroup(len, group);
        for (Set<Integer> group : query.getInequalityGroups())
            checkGroup(len, group);

        StringBuilder lookaheads = new StringBuilder();
        int[] groups = {0};
        String contains = DictionaryQueryNormalizer.canonicalFragment(query.getContains());
        if (contains != null && !contains.isEmpty())
            lookaheads.append("(?=.*").append(esc(contains)).append(')');
        for (Set<Integer> group : query.getEqualityGroups()) {
            Integer[] ps = group.toArray(new Integer[0]);
            for (int k = 1; k < ps.length; k++)
                lookaheads.append(equalPair(ps[0].intValue(), ps[k].intValue(), groups));
        }
        for (Set<Integer> group : query.getInequalityGroups()) {
            Integer[] ps = group.toArray(new Integer[0]);
            for (int a = 0; a < ps.length; a++)
                for (int b = a + 1; b < ps.length; b++)
                    lookaheads.append(unequalPair(ps[a].intValue(), ps[b].intValue(), groups));
        }
        if (query.isAllLettersDifferent()) {
            for (int a = 1; a <= len; a++)
                for (int b = a + 1; b <= len; b++)
                    lookaheads.append(unequalPair(a, b, groups));
        }
        if (query.isAllUnmatchedLettersDifferent() && !fixed.isEmpty()) {
            for (int a = 1; a <= len; a++) {
                if (fixed.containsKey(Integer.valueOf(a)))
                    continue;
                for (int b = a + 1; b <= len; b++) {
                    if (fixed.containsKey(Integer.valueOf(b)))
                        continue;
                    lookaheads.append(unequalPair(a, b, groups));
                }
            }
        }

        StringBuilder sb = new StringBuilder("^");
        sb.append("(?=[a-z]{").append(len).append("}:)");
        sb.append(lookaheads);
        if (fixed.isEmpty()) {
            sb.append("[a-z]{").append(len).append('}');
        } else {
            for (int p = 1; p <= len; p++) {
                Character lit = fixed.get(Integer.valueOf(p));
                sb.append(lit == null ? "[a-z]" : String.valueOf(lit.charValue()));
            }
        }
        return sb.append(':').toString();
    }

    private static void checkGroup(int len, Set<Integer> group) {
        for (Integer p : group)
            checkPosition(len, p.intValue());
    }

    /**
     * Positions i and j carry the same letter: anchor the key inside the length
     * frame, grab the char at i, then require the char at j to equal it. Each
     * call allocates a fresh backreference group so earlier captures are never
     * shadowed by later lookaheads.
     */
    private static String equalPair(int i, int j, int[] groups) {
        groups[0] += 1;
        return "(?=.{" + (i - 1) + "}(.)(?=.{" + (j - i - 1) + "}\\" + groups[0] + "))";
    }

    /**
     * Positions i and j carry different letters, same frame as {@link #equalPair}.
     */
    private static String unequalPair(int i, int j, int[] groups) {
        groups[0] += 1;
        return "(?=.{" + (i - 1) + "}(.)(?=.{" + (j - i - 1) + "}(?!\\" + groups[0] + ")))";
    }

}