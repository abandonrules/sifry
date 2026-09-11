package cz.absolutno.sifry.regexp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tier-3 link between the FilterRule pattern compiler and the bundled canon
 * assets: every constraint kind must produce a pattern the native PCRE search
 * accepts and that matches the intended entries.
 */
@RunWith(AndroidJUnit4.class)
public class FilterRuleAssetsTest {

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

    private static long count(String filename, String pattern) throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), new String[]{"raw/" + filename}, new String[]{pattern},
                    false, RegExpNative.MaxListResults);
            RegExpNative.Report rep = waitFor(rn, 60000);
            assertFalse("search timed out for " + filename, rep.running);
            assertFalse("error on " + filename + ": " + rn.getError(), rep.error);
            return rep.matches;
        } finally {
            rn.free();
        }
    }

    private static long countMulti(String fns[], String pattern) throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), fns, new String[]{pattern}, false,
                    RegExpNative.MaxListResults);
            RegExpNative.Report rep = waitFor(rn, 60000);
            assertFalse("search timed out", rep.running);
            assertFalse("error: " + rn.getError(), rep.error);
            return rep.matches;
        } finally {
            rn.free();
        }
    }

    private static String first(String filename, String pattern) throws Exception {
        long n = count(filename, pattern);
        assertTrue("no match for /" + pattern + "/ in " + filename + " (matches=" + n + ")",
                n >= 1);
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), new String[]{"raw/" + filename}, new String[]{pattern},
                    false, RegExpNative.MaxListResults);
            RegExpNative.Report rep = waitFor(rn, 60000);
            assertFalse("search timed out for " + filename, rep.running);
            assertFalse("error on " + filename + ": " + rn.getError(), rep.error);
            return rn.getResult(0);
        } finally {
            rn.free();
        }
    }

    @Test
    public void periodicSymbolAndAtomicNumber() throws Exception {
        assertTrue(first("periodic.canon", FilterRule.pattern(FilterRule.Kind.SYMBOL,
                FilterRule.Op.EQ, "he", "")).contains("Helium (He, 2)"));
        assertTrue(first("periodic.canon", FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER,
                FilterRule.Op.EQ, "2", "")).contains("(He, 2)"));
        assertTrue(first("periodic.canon", FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER,
                FilterRule.Op.LT, "2", "")).contains("(H, 1)"));
        assertTrue(first("periodic.canon", FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER,
                FilterRule.Op.BETWEEN, "30", "35")).contains("(As, 33)"));
        assertEquals(12, count("periodic.canon", FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER,
                FilterRule.Op.BETWEEN, "30", "35")));
    }

    @Test
    public void periodicSymbolAndAtomicNumberNegative() throws Exception {
        assertTrue("xx symbol got matches",
                count("periodic.canon", FilterRule.pattern(FilterRule.Kind.SYMBOL,
                        FilterRule.Op.EQ, "xx", "")) == 0);
        assertEquals(null, FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER,
                FilterRule.Op.GT, "118", ""));
        assertEquals(236, count("periodic.canon", ""));
    }

    @Test
    public void pokemonTypeAndDexNumber() throws Exception {
        String type = first("pokemon.canon", FilterRule.pattern(FilterRule.Kind.TYPE,
                FilterRule.Op.EQ, "electric", ""));
        assertTrue("type first=" + type, type.contains("(Electric)"));
        String dex = first("pokemon.canon", FilterRule.pattern(FilterRule.Kind.DEX_NUMBER,
                FilterRule.Op.EQ, "25", ""));
        assertTrue("dex first=" + dex, dex.contains("Pikachu"));
        String between = first("pokemon.canon", FilterRule.pattern(FilterRule.Kind.DEX_NUMBER,
                FilterRule.Op.BETWEEN, "1", "4"));
        assertTrue("dex between first=" + between, between.contains("Bulbasaur"));
    }

    @Test
    public void wordleRangeAndLength() throws Exception {
        String word = first("wordle.canon", FilterRule.pattern(FilterRule.Kind.RANGE,
                FilterRule.Op.EQ, "crane", "crate"));
        assertTrue("out of range: " + word, "crane".compareTo(word) <= 0
                && word.compareTo("crate") <= 0);
        assertEquals("aahed", first("wordle.canon", FilterRule.pattern(FilterRule.Kind.LENGTH,
                FilterRule.Op.EQ, "5", "")));
        assertEquals(0, count("wordle.canon", FilterRule.pattern(FilterRule.Kind.LENGTH,
                FilterRule.Op.LT, "5", "")));
    }

    @Test
    public void englishStartsWith() throws Exception {
        assertEquals("hello", first("en.canon", FilterRule.pattern(FilterRule.Kind.STARTS,
                FilterRule.Op.EQ, "hello", "")));
    }

    @Test
    public void multiSourceSearchesAllFiles() throws Exception {
        assertEquals("periodic(236)+pokemon(1025)",
                1261, countMulti(new String[]{"raw/periodic.canon", "raw/pokemon.canon"}, ""));
    }

    @Test
    public void multiSourcePatternOnlyHitsItsSource() throws Exception {
        String dex = FilterRule.pattern(FilterRule.Kind.DEX_NUMBER, FilterRule.Op.EQ, "25", "");
        assertEquals("Dex filter must not match periodic table entries", 0,
                count("periodic.canon", dex));
        assertEquals(1, countMulti(new String[]{"raw/periodic.canon", "raw/pokemon.canon"}, dex));
    }

    @Test
    public void multiSourceMatchListCappedAtMax() throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), new String[]{"raw/periodic.canon", "raw/pokemon.canon"},
                    new String[]{""}, false, RegExpNative.MaxListResults);
            RegExpNative.Report rep = waitFor(rn, 60000);
            assertFalse("error: " + rn.getError(), rep.error);
            assertEquals(1261, rep.matches);
            assertTrue("list should hold exactly MaxListResults",
                    rep.matches >= RegExpNative.MaxListResults);
            assertFalse("cap check result must not be a real match",
                    "".equals(rn.getResult(0)));
            assertEquals("", rn.getResult(RegExpNative.MaxListResults));
        } finally {
            rn.free();
        }
    }

    @Test
    public void verboseModeIncludesCanonicalKey() throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), new String[]{"raw/periodic.canon"},
                    new String[]{"^he:"}, true, RegExpNative.ShowAllResults);
            RegExpNative.Report rep = waitFor(rn, 60000);
            assertFalse("error: " + rn.getError(), rep.error);
            assertEquals(1, rep.matches);
            String line = rn.getResult(0);
            assertTrue("verbose line should carry the key before ':', got '" + line + "'",
                    line.contains("he:"));
            assertTrue("verbose line should carry the display after ':', got '" + line + "'",
                    line.endsWith("Helium (He, 2)"));
        } finally {
            rn.free();
        }
    }
}