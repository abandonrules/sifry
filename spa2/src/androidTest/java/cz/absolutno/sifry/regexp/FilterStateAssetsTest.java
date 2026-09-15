package cz.absolutno.sifry.regexp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.res.AssetManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import cz.absolutno.sifry.common.dictionary.DictionaryQueryCompiler;
import cz.absolutno.sifry.common.dictionary.WordPatternQuery;

/**
 * Tier-3 end-to-end check of the Yes/No/Or tri-state on real canon data. Each
 * scenario mirrors a {@code RegExpDFragment.runSearch} pass: per-row patterns
 * are built exactly as the fragment does, {@link FilterRule#foldPatterns} folds
 * them with the row states, and the resulting AND array is run through the
 * native PCRE engine. The native match count is compared against an independent
 * Java reference that applies the same per-slot negation semantics over the
 * full dictionary lines, so any drift between the UI intent and the engine is
 * caught even when the underlying dictionaries change.
 */
@RunWith(AndroidJUnit4.class)
public class FilterStateAssetsTest {

    private static final int YES = FilterRule.ST_YES;
    private static final int NO = FilterRule.ST_NO;
    private static final int OR = FilterRule.ST_OR;

    private static RegExpNative.Report waitFor(RegExpNative rn, long timeoutMs)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        RegExpNative.Report rep;
        do {
            rep = rn.getProgress();
            if (!rep.running)
                return rep;
            Thread.sleep(50);
        } while (System.currentTimeMillis() - start < timeoutMs);
        return rep;
    }

    private static AssetManager assets() {
        return ApplicationProvider.getApplicationContext().getAssets();
    }

    private static final class Row {
        final FilterRule.Kind kind;
        final FilterRule.Op op;
        final String v1;
        final String v2;
        final int state;

        Row(FilterRule.Kind kind, FilterRule.Op op, String v1, String v2, int state) {
            this.kind = kind;
            this.op = op;
            this.v1 = v1;
            this.v2 = v2;
            this.state = state;
        }
    }

    /** Mirror of {@code RegExpDFragment.buildPattern}. */
    private static String patternOf(Row r) {
        if (r.kind == FilterRule.Kind.EQUALS || r.kind == FilterRule.Kind.STARTS
                || r.kind == FilterRule.Kind.ENDS) {
            String a = r.v1 == null ? "" : r.v1.trim().toLowerCase();
            if (a.isEmpty())
                return "";
            if (r.kind == FilterRule.Kind.EQUALS)
                return DictionaryQueryCompiler.exactKeyPattern(a);
            WordPatternQuery.Builder q = WordPatternQuery.builder();
            if (r.kind == FilterRule.Kind.STARTS)
                q.prefix(a);
            else
                q.suffix(a);
            return DictionaryQueryCompiler.compile(q.build());
        }
        return FilterRule.pattern(r.kind, r.op, r.v1, r.v2);
    }

    private static String[] fold(Row[] rows) {
        return FilterRule.foldPatterns(
                toPats(rows), toStates(rows)).toArray(new String[0]);
    }

    private static java.util.List<String> toPats(Row[] rows) {
        java.util.List<String> pats = new java.util.ArrayList<String>();
        for (Row r : rows)
            pats.add(patternOf(r));
        return pats;
    }

    private static java.util.List<Integer> toStates(Row[] rows) {
        java.util.List<Integer> states = new java.util.ArrayList<Integer>();
        for (Row r : rows)
            states.add(Integer.valueOf(r.state));
        return states;
    }

    private static long nativeCount(String[] fns, String[] folded) throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            String raw[] = new String[fns.length];
            for (int i = 0; i < fns.length; i++)
                raw[i] = "raw/" + fns[i];
            rn.startThread(assets(), raw, folded, false, RegExpNative.MaxListResults);
            RegExpNative.Report rep = waitFor(rn, 120000);
            assertFalse("search timed out", rep.running);
            assertFalse("error: " + rn.getError(), rep.error);
            return rep.matches;
        } finally {
            rn.free();
        }
    }

    /** Independent reference: slot semantics identical to regrep.cpp threadMain. */
    private static long referenceCount(String[] fns, String[] folded) throws Exception {
        java.util.regex.Pattern slots[] = new java.util.regex.Pattern[folded.length];
        boolean[] neg = new boolean[folded.length];
        for (int i = 0; i < folded.length; i++) {
            String s = folded[i];
            if (s.isEmpty())
                continue;
            if (s.charAt(0) == '!') {
                neg[i] = true;
                s = s.substring(1);
            }
            if (!s.isEmpty())
                slots[i] = java.util.regex.Pattern.compile(s);
        }
        long count = 0;
        for (String fn : fns) {
            Set<String> seenKeys = new HashSet<String>();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    assets().open("raw/" + fn), "UTF-8"));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    int colon = line.indexOf(':');
                    String key = colon < 0 ? line : line.substring(0, colon);
                    if (!seenKeys.add(key))
                        continue;
                    boolean hit = true;
                    for (int i = 0; i < slots.length; i++) {
                        if (slots[i] == null)
                            continue;
                        boolean m = slots[i].matcher(line).find();
                        if (m == neg[i]) {
                            hit = false;
                            break;
                        }
                    }
                    if (hit)
                        count++;
                }
            } finally {
                br.close();
            }
        }
        return count;
    }

    private static void assertScenario(String[] fns, Row[] rows) throws Exception {
        String[] folded = fold(rows);
        assertEquals("folded=" + java.util.Arrays.toString(folded),
                referenceCount(fns, folded), nativeCount(fns, folded));
    }

    private static final String[] EN = {"en.canon"};

    private static Row row(FilterRule.Kind k, String v1, int state) {
        return new Row(k, FilterRule.Op.EQ, v1, "", state);
    }

    @Test
    public void twoYesRowsAreJustAnd() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", YES),
                row(FilterRule.Kind.CONTAINS, "th", YES)
        });
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "un", YES),
                row(FilterRule.Kind.CONTAINS, "ly", YES)
        });
    }

    @Test
    public void noRowNegatesItsConstraintAlone() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", YES),
                row(FilterRule.Kind.CONTAINS, "th", NO)
        });
    }

    @Test
    public void orRowsFoldIntoOneAlternationSlot() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", YES),
                row(FilterRule.Kind.CONTAINS, "th", OR)
        });
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", YES),
                row(FilterRule.Kind.CONTAINS, "th", OR),
                row(FilterRule.Kind.CONTAINS, "er", OR)
        });
    }

    @Test
    public void noGroupNegatesWholeAlternation() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", NO),
                row(FilterRule.Kind.CONTAINS, "th", OR),
                row(FilterRule.Kind.CONTAINS, "er", OR)
        });
    }

    @Test
    public void noAfterYesWithOrAlternatesOnlyTheNoGroup() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", YES),
                row(FilterRule.Kind.CONTAINS, "th", NO),
                row(FilterRule.Kind.CONTAINS, "er", OR)
        });
    }

    @Test
    public void noBetweenYesAndOrKeepsBoth() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", YES),
                row(FilterRule.Kind.CONTAINS, "th", NO),
                row(FilterRule.Kind.CONTAINS, "er", YES)
        });
    }

    @Test
    public void mixedKindsApprovedPrefixAndNegatedLength() throws Exception {
        assertScenario(EN, new Row[]{
                new Row(FilterRule.Kind.STARTS, FilterRule.Op.EQ, "ea", "", YES),
                new Row(FilterRule.Kind.LENGTH, FilterRule.Op.EQ, "5", "", NO)
        });
    }

    @Test
    public void multiSourceRespectsPerFileDedup() throws Exception {
        assertScenario(new String[]{"en.canon", "cs.canon"}, new Row[]{
                row(FilterRule.Kind.CONTAINS, "ea", YES),
                row(FilterRule.Kind.CONTAINS, "th", OR)
        });
    }

    @Test
    public void emptyRowsContributeNothing() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "", YES),
                row(FilterRule.Kind.CONTAINS, "km", NO)
        });
    }

    @Test
    public void absentTokensYieldZeroEitherWay() throws Exception {
        assertScenario(EN, new Row[]{
                row(FilterRule.Kind.CONTAINS, "qq", YES),
                row(FilterRule.Kind.CONTAINS, "zz", OR)
        });
    }
}