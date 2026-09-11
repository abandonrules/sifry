package cz.absolutno.sifry.regexp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tier-3 safety net for the supplemental canon dictionaries: each bundled
 * source must be loadable by the native PCRE search and match an expected key.
 */
@RunWith(AndroidJUnit4.class)
public class DictionaryAssetsTest {

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

    private static String first(String filename, String pattern) throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), new String[]{"raw/" + filename}, new String[]{pattern},
                    false, RegExpNative.MaxListResults);
            RegExpNative.Report rep = waitFor(rn, 60000);
            assertFalse("search timed out for " + filename, rep.running);
            assertFalse("error on " + filename + ": " + rn.getError(), rep.error);
            assertTrue("no match for /" + pattern + "/ in " + filename
                    + " (matches=" + rep.matches + ")", rep.matches >= 1);
            return rn.getResult(0);
        } finally {
            rn.free();
        }
    }

    @Test
    public void periodicTableByNameAndSymbol() throws Exception {
        assertTrue(first("periodic.canon", "^actinium:").startsWith("Actinium (Ac, 89)"));
        assertTrue(first("periodic.canon", "^he:").startsWith("Helium (He, 2)"));
        assertTrue(first("periodic.canon", "^silver:").startsWith("Silver (Ag, 47)"));
    }

    @Test
    public void pokemonByNameWithDiacriticsAndPunctuation() throws Exception {
        assertTrue(first("pokemon.canon", "^pikachu:").startsWith("Pikachu (Electric)"));
        assertTrue(first("pokemon.canon", "^mrmime:").startsWith("Mr. Mime"));
        assertTrue(first("pokemon.canon", "^flabebe:").contains("Flabébé"));
        assertTrue(first("pokemon.canon", "^nidoran:").contains("Nidoran"));
    }

    @Test
    public void wordleAnswersSearchable() throws Exception {
        assertTrue(first("wordle.canon", "^crane:").startsWith("crane"));
        assertTrue(first("wordle.canon", "^soare:").startsWith("soare"));
        assertTrue(first("wordle.canon", "^abaci:").startsWith("abaci"));
    }
}