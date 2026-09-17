package cz.absolutno.sifry.substituce.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Pure-JVM tests for the Kasiski examination. One fixture is synthetic and
 * exact: plaintext "ABCDEF" encrypted with keyword SYSTEM under the A=0/B=0
 * convention yields "SZUWIR" (A+S=S, B+Y=Z, C+S=U, D+T=W, E+E=I, F+M=R), so
 * "SZUWIRSZUWIR" repeats every trigram at distance 6.
 *
 * <p>The other uses the tutorial's real text (C.-K. Shene, "Kasiski's Method",
 * Michigan Technological University Vigenere tutorial): Hoare's quote encrypted
 * with the 6-letter keyword SYSTEM, 205 letters. Its repeated trigrams include
 * MJC at positions 5 and 35 (distance 30) and ISW at 11 and 47 (distance 36);
 * the tutorial's signal distances 72, 66, 36 and 30 have GCD 6, so 6 must be
 * among the candidate periods (never asserted as the true length — evidence only).
 */
public final class KasiskiAnalyzerTest {

    private static final String CT_REPEATED = "SZUWIRSZUWIR";

    /** Shene's SYSTEM example: Hoare's quote, spaces stripped, uppercase. */
    private static final String CT_SYSTEM =
            "LFWKIMJCLPSISWKHJOGLKMVGURAGKMKMXMAMJCVXWUYLGGIISW"
          + "ALXAEYCXMFKMKBQBDCLAEFLFWKIMJCGUZUGSKECZGBWYMOACFV"
          + "MQKYFWXTWMLAIDOYQBWFGKSDIULQGVSYHJAVEFWBLAEFLFWKIM"
          + "JCFHSNNGGNWPWDAVMQFAAXWFZCXBVELKWMLAVGKYEDEMJXHUXD"
          + "AVYXL";

    @Test
    public void trigramLengthIsThree() {
        assertEquals(3, KasiskiAnalyzer.trigramLength());
    }

    @Test
    public void normalizeLettersUppercasesAndStrips() {
        assertEquals("HELLOWORLD", KasiskiAnalyzer.normalizeLetters("Hello, World!"));
        assertEquals("SZUWIRSZUWIR", KasiskiAnalyzer.normalizeLetters(CT_REPEATED));
        assertEquals("", KasiskiAnalyzer.normalizeLetters(""));
    }

    @Test
    public void repeatedTrigramReportsPositionsAndDistanceOfSix() {
        List<KasiskiAnalyzer.RepeatedSequence> rs =
                KasiskiAnalyzer.findRepeatedTrigrams(CT_REPEATED);
        // Trigram "SZU" occurs at normalized positions 0 and 6.
        KasiskiAnalyzer.RepeatedSequence szu = find(rs, "SZU");
        assertEquals(2, szu.getPositions().size());
        assertEquals(Integer.valueOf(0), szu.getPositions().get(0));
        assertEquals(Integer.valueOf(6), szu.getPositions().get(1));
        assertEquals(1, szu.getDistances().size());
        assertEquals(Integer.valueOf(6), szu.getDistances().get(0));
    }

    @Test
    public void repeatedDistancesAreAllSix() {
        List<Integer> d = KasiskiAnalyzer.repeatedDistances(CT_REPEATED);
        // Four repeated trigrams (SZU, ZUW, UWI, WIR), each at distance 6;
        // IRS and RSZ occur only once and so are not reported.
        assertEquals(4, d.size());
        for (int x : d)
            assertEquals(6, x);
    }

    @Test
    public void tutorialSystemCiphertextMakesSixACandidatePeriod() {
        // MJC and ISW are the tutorial's signal repeats (distances 30 and 36),
        // both multiples of the 6-letter keyword, so 6 must be a candidate period.
        List<KasiskiAnalyzer.RepeatedSequence> rs =
                KasiskiAnalyzer.findRepeatedTrigrams(CT_SYSTEM);
        KasiskiAnalyzer.RepeatedSequence mjc = find(rs, "MJC");
        assertTrue(mjc.getPositions().contains(Integer.valueOf(5)));
        assertTrue(mjc.getPositions().contains(Integer.valueOf(35)));
        assertTrue(mjc.getDistances().contains(Integer.valueOf(30)));
        List<KasiskiAnalyzer.PeriodCandidate> cand =
                KasiskiAnalyzer.candidatePeriods(CT_SYSTEM);
        assertTrue("expected a period-6 candidate in " + cand, containsPeriod(cand, 6));
    }

    @Test
    public void primeFactorsOfSixAndThirty() {
        assertEquals(2, KasiskiAnalyzer.primeFactors(6).size());
        assertEquals(Integer.valueOf(2), KasiskiAnalyzer.primeFactors(6).get(0));
        assertEquals(Integer.valueOf(3), KasiskiAnalyzer.primeFactors(6).get(1));
        // 30 = 2 * 3 * 5 (ascending, with multiplicity).
        List<Integer> f30 = KasiskiAnalyzer.primeFactors(30);
        assertEquals(3, f30.size());
        assertEquals(Integer.valueOf(2), f30.get(0));
        assertEquals(Integer.valueOf(3), f30.get(1));
        assertEquals(Integer.valueOf(5), f30.get(2));
    }

    @Test
    public void divisorsIncludeThePeriodSix() {
        List<Integer> div = KasiskiAnalyzer.divisors(6);
        assertTrue(div.contains(Integer.valueOf(2)));
        assertTrue(div.contains(Integer.valueOf(3)));
        assertTrue(div.contains(Integer.valueOf(6)));
    }

    @Test
    public void divisorsOfOneAreEmptyAndNeverLeakAPeriod() {
        // divisors documents values greater than 1; an overlapping repeat ("AAAA")
        // has distance 1, which must not turn into a bogus period-1 candidate.
        assertTrue(KasiskiAnalyzer.divisors(1).isEmpty());
        assertTrue(KasiskiAnalyzer.candidatePeriods("AAAA").isEmpty());
    }

    @Test
    public void periodCandidatesCompareByValue() {
        List<KasiskiAnalyzer.PeriodCandidate> a = KasiskiAnalyzer.candidatePeriods(CT_REPEATED);
        List<KasiskiAnalyzer.PeriodCandidate> b = KasiskiAnalyzer.candidatePeriods(CT_REPEATED);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        KasiskiAnalyzer.PeriodCandidate source = a.get(0);
        KasiskiAnalyzer.PeriodCandidate copy =
                new KasiskiAnalyzer.PeriodCandidate(source.getPeriod(), source.getEvidence());
        assertEquals(source, copy);
        assertEquals(source.hashCode(), copy.hashCode());
    }

    @Test
    public void candidatePeriodsIncludeKeywordLengthSix() {
        List<KasiskiAnalyzer.PeriodCandidate> cand = KasiskiAnalyzer.candidatePeriods(CT_REPEATED);
        assertTrue("expected a period-6 candidate in " + cand, containsPeriod(cand, 6));
    }

    private static KasiskiAnalyzer.RepeatedSequence find(List<KasiskiAnalyzer.RepeatedSequence> rs,
            String seq) {
        for (KasiskiAnalyzer.RepeatedSequence r : rs)
            if (seq.equals(r.getSequence()))
                return r;
        throw new AssertionError("missing repeated sequence " + seq);
    }

    private static boolean containsPeriod(List<KasiskiAnalyzer.PeriodCandidate> cand, int period) {
        for (KasiskiAnalyzer.PeriodCandidate c : cand)
            if (c.getPeriod() == period)
                return true;
        return false;
    }
}
