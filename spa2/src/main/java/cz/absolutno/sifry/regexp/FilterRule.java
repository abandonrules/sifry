package cz.absolutno.sifry.regexp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class FilterRule {

    public enum Kind {
        CONTAINS, STARTS, ENDS, EQUALS, RANGE, LENGTH, SYMBOL, ATOMIC_NUMBER, TYPE, DEX_NUMBER
    }

    public enum Op { EQ, LT, GT, LE, GE, BETWEEN }

    public static final int ATOMIC_UPPER = 118;
    public static final int DEX_UPPER = 1025;
    public static final int LENGTH_CAP = 40;

    private FilterRule() {
    }

    public static List<Kind> kindsFor(String filename) {
        List<Kind> kinds = new ArrayList<Kind>();
        Collections.addAll(kinds, Kind.CONTAINS, Kind.STARTS, Kind.ENDS, Kind.EQUALS);
        if ("periodic.canon".equals(filename)) {
            kinds.add(Kind.SYMBOL);
            kinds.add(Kind.ATOMIC_NUMBER);
        } else if ("pokemon.canon".equals(filename)) {
            kinds.add(Kind.TYPE);
            kinds.add(Kind.DEX_NUMBER);
        } else {
            kinds.add(Kind.RANGE);
            kinds.add(Kind.LENGTH);
        }
        return kinds;
    }

    /**
     * Union of the constraint kinds offered by all searched sources, in the
     * canonical label order: the four text kinds always, then word-dictionary
     * kinds, then periodic-table and Pokemon-specific kinds.
     */
    public static List<Kind> kindsForAll(List<String> filenames) {
        List<Kind> kinds = new ArrayList<Kind>();
        Collections.addAll(kinds, Kind.CONTAINS, Kind.STARTS, Kind.ENDS, Kind.EQUALS);
        boolean word = false, periodic = false, pokemon = false;
        for (String f : filenames) {
            if ("periodic.canon".equals(f))
                periodic = true;
            else if ("pokemon.canon".equals(f))
                pokemon = true;
            else
                word = true;
        }
        if (word) {
            kinds.add(Kind.RANGE);
            kinds.add(Kind.LENGTH);
        }
        if (periodic) {
            kinds.add(Kind.SYMBOL);
            kinds.add(Kind.ATOMIC_NUMBER);
        }
        if (pokemon) {
            kinds.add(Kind.TYPE);
            kinds.add(Kind.DEX_NUMBER);
        }
        return kinds;
    }

    public static boolean isNumeric(Kind kind) {
        return kind == Kind.LENGTH || kind == Kind.ATOMIC_NUMBER || kind == Kind.DEX_NUMBER;
    }

    public static boolean isNumeric(Op op) {
        return op == Op.EQ || op == Op.LT || op == Op.GT || op == Op.LE || op == Op.GE || op == Op.BETWEEN;
    }

    public static boolean needsSecondValue(Kind kind, Op op) {
        return kind == Kind.RANGE || op == Op.BETWEEN;
    }

    public static String pattern(Kind kind, Op op, String v1, String v2) {
        String a = v1 == null ? "" : v1.trim();
        String b = v2 == null ? "" : v2.trim();
        int lo, hi;
        switch (kind) {
            case CONTAINS:
                return a.isEmpty() ? null : a;
            case STARTS:
                return a.isEmpty() ? null : "^" + quote(a.toLowerCase());
            case ENDS:
                return a.isEmpty() ? null : quote(a.toLowerCase()) + ":";
            case EQUALS:
                return a.isEmpty() ? null : "^" + quote(a.toLowerCase()) + ":";
            case RANGE:
                if (a.isEmpty() || b.isEmpty())
                    return null;
                return lexRange(a.toLowerCase(), b.toLowerCase());
            case LENGTH:
                if (a.isEmpty() || !a.matches("[0-9]+"))
                    return null;
                int n = Integer.parseInt(a);
                int[] r = numericBounds(kind, op, n, b);
                if (r == null)
                    return null;
                return "^[a-z]{" + r[0] + "," + r[1] + "}:";
            case SYMBOL:
                return a.isEmpty() ? null : "\\((?i)" + quote(a) + ",\\s*\\d+\\)$";
            case ATOMIC_NUMBER:
                if (a.isEmpty() || !a.matches("[0-9]+"))
                    return null;
                int[] rb = numericBounds(kind, op, Integer.parseInt(a), b);
                if (rb == null)
                    return null;
                return ",\\s*(?:" + digitRange(rb[0], rb[1]) + ")\\)$";
            case TYPE:
                return a.isEmpty() ? null : "\\((?i)[^)]*\\b" + quote(a) + "\\b[^)]*\\)";
            case DEX_NUMBER:
                if (a.isEmpty() || !a.matches("[0-9]+"))
                    return null;
                int[] rd = numericBounds(kind, op, Integer.parseInt(a), b);
                if (rd == null)
                    return null;
                return "#(?:" + digitRange(rd[0], rd[1]) + ")$";
            default:
                return null;
        }
    }

    private static int[] numericBounds(Kind kind, Op op, int n, String b) {
        int upper;
        if (kind == Kind.LENGTH) upper = LENGTH_CAP;
        else if (kind == Kind.ATOMIC_NUMBER) upper = ATOMIC_UPPER;
        else upper = DEX_UPPER;
        if (op == Op.EQ) return inRange(n, n, upper);
        if (op == Op.LT) return inRange(1, n - 1, upper);
        if (op == Op.GT) return inRange(n + 1, upper, upper);
        if (op == Op.LE) return inRange(1, n, upper);
        if (op == Op.GE) return inRange(n, upper, upper);
        int m = (b == null || !b.matches("[0-9]+")) ? -1 : Integer.parseInt(b);
        if (m < n)
            return null;
        return inRange(n, m, upper);
    }

    private static int[] inRange(int lo, int hi, int upper) {
        if (hi < 1 || lo > upper)
            return null;
        return new int[]{Math.max(1, lo), Math.min(hi, upper)};
    }

    private static String quote(String s) {
        return Pattern.quote(s);
    }

    static String digitRange(int lo, int hi) {
        if (lo < 1 || hi < lo)
            return null;
        if (lo == hi)
            return String.valueOf(lo);
        int loLen = Integer.toString(lo).length();
        int hiLen = Integer.toString(hi).length();
        StringBuilder sb = new StringBuilder("(?:");
        String sep = "";
        for (int len = loLen; len <= hiLen; len++) {
            int l = (len == loLen) ? lo : pow10(len - 1);
            int h = (len == hiLen) ? hi : pow10(len) - 1;
            if (l > h)
                continue;
            sb.append(sep).append(sameLen(String.valueOf(l), String.valueOf(h)));
            sep = "|";
        }
        return sb.append(')').toString();
    }

    private static int pow10(int n) {
        int r = 1;
        for (int i = 0; i < n; i++)
            r *= 10;
        return r;
    }

    private static String sameLen(String s, String t) {
        if (s.equals(t))
            return s;
        StringBuilder p = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char a = s.charAt(i), b = t.charAt(i);
            if (a == b) {
                p.append(a);
                continue;
            }
            int rest = s.length() - i - 1;
            StringBuilder out = new StringBuilder("(?:");
            if (a + 1 <= b - 1)
                out.append(p).append('[').append((char) (a + 1)).append('-').append((char) (b - 1))
                        .append("]\\d{").append(rest).append("}|");
            out.append(p).append(a).append(rest == 0 ? "" : sameLen(s.substring(i + 1), nines(rest))).append('|');
            out.append(p).append(b).append(rest == 0 ? "" : sameLen(zeros(rest), t.substring(i + 1)));
            return out.append(')').toString();
        }
        return p.toString();
    }

    private static String nines(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('9');
        return sb.toString();
    }

    private static String zeros(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('0');
        return sb.toString();
    }

    static String lexRange(String lo, String hi) {
        if (lo.compareTo(hi) > 0)
            return null;
        if (lo.equals(hi))
            return "^" + quote(lo) + ":";
        return "^" + lexR(lo, hi) + ":";
    }

    private static String lexR(String s, String t) {
        int i = 0;
        while (i < s.length() && i < t.length() && s.charAt(i) == t.charAt(i))
            i++;
        String p = s.substring(0, i);
        if (i == s.length()) {
            return "(?:" + quote(s) + "|" + quote(s) + atMost(t.substring(i)) + ")";
        }
        char a = s.charAt(i), b = t.charAt(i);
        StringBuilder out = new StringBuilder("(?:");
        String sep = "";
        if (a + 1 <= b - 1) {
            out.append(sep).append(quote(p)).append('[').append((char) (a + 1)).append('-')
                    .append((char) (b - 1)).append("][a-z]*");
            sep = "|";
        }
        out.append(sep).append(quote(p)).append(a).append(atLeast(s.substring(i + 1)));
        sep = "|";
        out.append(sep).append(quote(p)).append(b).append(atMost(t.substring(i + 1)));
        return out.append(')').toString();
    }

    private static String atLeast(String x) {
        if (x.isEmpty())
            return "[a-z]*";
        char c = x.charAt(0);
        String rest = atLeast(x.substring(1));
        if (c == 'z')
            return c + rest;
        return "(?:" + c + rest + "|[" + (char) (c + 1) + "-z][a-z]*)";
    }

    private static String atMost(String x) {
        if (x.isEmpty())
            return "";
        char c = x.charAt(0);
        String tail = atMost(x.substring(1));
        String below = (c == 'a') ? "" : "|[a-" + (char) (c - 1) + "][a-z]*";
        return "(?:|" + c + tail + below + ")";
    }
}