package cz.absolutno.sifry.substituce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.absolutno.sifry.common.alphabet.Alphabet;
import cz.absolutno.sifry.substituce.analysis.KasiskiAnalyzer;

/**
 * Renders the raw Kasiski evidence (issue #37, implementation step 8) into the
 * shared result list: one row per repeated trigram.
 *
 * <p>The upper line shows the repeated sequence, how often it occurs and the
 * positions of those occurrences; the lower line shows the distances between
 * consecutive occurrences together with their prime factorization. This is the
 * material the candidate-period dropdown is derived from, exposed so the solver
 * can check the evidence instead of trusting a single computed number.</p>
 *
 * <p>It is evidence only. The adapter never picks a key length and never
 * promotes a factor to a "period".</p>
 */
final class KasiskiAdapter extends AbstractSubstAdapter {

    /** Upper bound on the number of rows, so a pathological ciphertext cannot
     *  flood the list; the strongest evidence is kept. */
    private static final int MAX_ROWS = 50;

    /** Repeated sequences for the current input, strongest first. */
    private List<KasiskiAnalyzer.RepeatedSequence> sequences = new ArrayList<>();

    KasiskiAdapter(Alphabet abc) {
        super(abc);
    }

    @Override
    public void setInput(String str) {
        sequences = new ArrayList<>();
        if (str != null && str.length() != 0) {
            sequences.addAll(KasiskiAnalyzer.findRepeatedTrigrams(str));
            // Most frequent sequences first; alphabetical order breaks ties so
            // the list is stable between runs.
            Collections.sort(sequences, new Comparator<KasiskiAnalyzer.RepeatedSequence>() {
                @Override
                public int compare(KasiskiAnalyzer.RepeatedSequence a, KasiskiAnalyzer.RepeatedSequence b) {
                    if (a.getPositions().size() != b.getPositions().size())
                        return b.getPositions().size() - a.getPositions().size();
                    return a.getSequence().compareTo(b.getSequence());
                }
            });
            if (sequences.size() > MAX_ROWS)
                sequences = sequences.subList(0, MAX_ROWS);
        }
        super.setInput(str);
    }

    @Override
    protected int getCountValid() {
        return sequences.size();
    }

    @Override
    protected String getItemDesc(int position) {
        KasiskiAnalyzer.RepeatedSequence rs = sequences.get(position);
        return rs.getSequence() + "  \u00d7" + rs.getPositions().size()
                + "  @" + join(rs.getPositions(), ", ");
    }

    @Override
    public String getItem(int position) {
        KasiskiAnalyzer.RepeatedSequence rs = sequences.get(position);
        List<Integer> distances = rs.getDistances();
        List<String> parts = new ArrayList<>(distances.size());
        for (int d : distances)
            parts.add(d + "=" + factorization(d));
        return join(parts, ", ");
    }

    /** "84" -> "2\u00b72\u00b73\u00b77"; a prime maps to itself. */
    private static String factorization(int n) {
        List<Integer> factors = KasiskiAnalyzer.primeFactors(n);
        return join(factors, "\u00b7");
    }

    /** Minimal join helper; avoids pulling in a text utility for two callers. */
    private static String join(List<?> values, String separator) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            if (sb.length() > 0)
                sb.append(separator);
            sb.append(value);
        }
        return sb.toString();
    }
}
