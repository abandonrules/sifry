package cz.absolutno.sifry.substituce.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mechanical Kasiski examination of ciphertext. Pure JVM; no Android dependencies.
 * <p>
 * The helper finds repeated letter sequences (trigrams by default) in the normalized
 * (uppercase A-Z) ciphertext, reports their occurrence positions and the distances
 * between consecutive occurrences, factorizes those distances, and collects the
 * common factor evidence as candidate key lengths.
 * <p>
 * All of this is evidence only: this class never asserts that any candidate is the
 * true key length. The solver picks.
 */
public final class KasiskiAnalyzer {

    private static final int TRIGRAM = 3;

    private KasiskiAnalyzer() {
    }

    /** Repeat substring length used by default. */
    public static int trigramLength() {
        return TRIGRAM;
    }

    /** Normalizes ciphertext to uppercase A-Z letters only, in order. */
    public static String normalizeLetters(String ciphertext) {
        StringBuilder sb = new StringBuilder(ciphertext.length());
        for (int i = 0; i < ciphertext.length(); i++) {
            char c = ciphertext.charAt(i);
            if (c >= 'a' && c <= 'z')
                sb.append((char) (c - ('a' - 'A')));
            else if (c >= 'A' && c <= 'Z')
                sb.append(c);
        }
        return sb.toString();
    }

    /** A repeated sequence: the substring and the positions of its occurrences. */
    public static final class RepeatedSequence {
        private final String sequence;
        private final List<Integer> positions;

        RepeatedSequence(String sequence, List<Integer> positions) {
            this.sequence = sequence;
            this.positions = positions;
        }

        public String getSequence() {
            return sequence;
        }

        /** Occurrence start positions in the *normalized* letter string, ascending. */
        public List<Integer> getPositions() {
            return positions;
        }

        /** Distances between consecutive occurrences, same size as positions - 1. */
        public List<Integer> getDistances() {
            List<Integer> d = new ArrayList<Integer>();
            for (int i = 1; i < positions.size(); i++)
                d.add(positions.get(i) - positions.get(i - 1));
            return d;
        }
    }

    /**
     * Finds all letter trigrams that occur at least twice, with their occurrence
     * positions in the normalized letters. Each distinct trigram is reported once.
     */
    public static List<RepeatedSequence> findRepeatedTrigrams(String ciphertext) {
        String letters = normalizeLetters(ciphertext);
        Map<String, List<Integer>> bySeq = new LinkedHashMap<String, List<Integer>>();
        for (int i = 0; i + TRIGRAM <= letters.length(); i++) {
            String seq = letters.substring(i, i + TRIGRAM);
            List<Integer> pos = bySeq.get(seq);
            if (pos == null) {
                pos = new ArrayList<Integer>();
                bySeq.put(seq, pos);
            }
            pos.add(i);
        }
        List<RepeatedSequence> out = new ArrayList<RepeatedSequence>();
        for (Map.Entry<String, List<Integer>> e : bySeq.entrySet())
            if (e.getValue().size() >= 2)
                out.add(new RepeatedSequence(e.getKey(), e.getValue()));
        return out;
    }

    /** All consecutive-occurrence distances across all repeated trigrams, ascending. */
    public static List<Integer> repeatedDistances(String ciphertext) {
        List<Integer> all = new ArrayList<Integer>();
        for (RepeatedSequence rs : findRepeatedTrigrams(ciphertext))
            all.addAll(rs.getDistances());
        Collections.sort(all);
        return all;
    }

    /**
     * Prime factorization of a positive integer, ascending with multiplicity,
     * e.g. 84 -> [2, 2, 3, 7].
     */
    public static List<Integer> primeFactors(int n) {
        if (n <= 0)
            throw new IllegalArgumentException("n must be positive");
        List<Integer> factors = new ArrayList<Integer>();
        int d = 2;
        while (d * d <= n) {
            while (n % d == 0) {
                factors.add(d);
                n /= d;
            }
            d++;
        }
        if (n > 1)
            factors.add(n);
        return factors;
    }

    /** Distinct divisors of n greater than 1, ascending. */
    public static List<Integer> divisors(int n) {
        if (n <= 0)
            throw new IllegalArgumentException("n must be positive");
        Map<Integer, Boolean> div = new HashMap<Integer, Boolean>();
        for (int i = 2; i * i <= n; i++)
            if (n % i == 0) {
                div.put(i, Boolean.TRUE);
                div.put(n / i, Boolean.TRUE);
            }
        div.put(n, Boolean.TRUE);
        List<Integer> out = new ArrayList<Integer>(div.keySet());
        Collections.sort(out);
        return out;
    }

    /** A candidate key length and how many repeated-trigram distances it divides. */
    public static final class PeriodCandidate {
        private final int period;
        private final int evidence;

        PeriodCandidate(int period, int evidence) {
            this.period = period;
            this.evidence = evidence;
        }

        public int getPeriod() {
            return period;
        }

        public int getEvidence() {
            return evidence;
        }
    }

    /**
     * Candidate periods: each divisor of every repeated trigram distance, scored by the
     * number of repeated-trigram distances it divides. Sorted by evidence (desc), then
     * period (asc). Evidence only; never a guarantee.
     */
    public static List<PeriodCandidate> candidatePeriods(String ciphertext) {
        Map<Integer, Integer> score = new HashMap<Integer, Integer>();
        List<Integer> distances = repeatedDistances(ciphertext);
        for (int d : distances)
            for (int div : divisors(d)) {
                Integer cur = score.get(div);
                score.put(div, (cur == null ? 0 : cur) + 1);
            }
        List<PeriodCandidate> out = new ArrayList<PeriodCandidate>();
        for (Map.Entry<Integer, Integer> e : score.entrySet())
            out.add(new PeriodCandidate(e.getKey(), e.getValue()));
        Collections.sort(out, new Comparator<PeriodCandidate>() {
            public int compare(PeriodCandidate a, PeriodCandidate b) {
                if (a.evidence != b.evidence)
                    return b.evidence - a.evidence;
                return a.period - b.period;
            }
        });
        return out;
    }

}