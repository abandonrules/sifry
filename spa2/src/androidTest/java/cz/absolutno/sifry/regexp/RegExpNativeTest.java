package cz.absolutno.sifry.regexp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RegExpNativeTest {

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

    @Test
    public void matchesWord() throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), "raw/en.canon", new String[]{"^aardvark:"});
            RegExpNative.Report rep = waitFor(rn, 30000);
            assertFalse("regexp search timed out", rep.running);
            assertFalse("error: " + rn.getError(), rep.error);
            assertTrue("no matches for ^aardvark: (matches=" + rep.matches + ")", rep.matches >= 1);
            assertEquals("aardvark", rn.getResult(0));
        } finally {
            rn.free();
        }
    }

    @Test
    public void switchDictionaryReplacesResults() throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), "raw/en.canon", new String[]{"^aardvark:"});
            RegExpNative.Report rep = waitFor(rn, 30000);
            assertFalse("regexp search timed out", rep.running);
            assertFalse("error: " + rn.getError(), rep.error);
            assertEquals("en results: (matches=" + rep.matches + ")", 1, rep.matches);
            assertEquals("aardvark", rn.getResult(0));

            rn.startThread(assets(), "raw/cs.canon", new String[]{"^sifry:"});
            rep = waitFor(rn, 60000);
            assertFalse("regexp search timed out", rep.running);
            assertFalse("cs error: " + rn.getError(), rep.error);
            assertTrue("cs switch lost matches (matches=" + rep.matches + ")", rep.matches >= 2);
            assertEquals("šifry", rn.getResult(0));
            rn.free();
        }
    }

    @Test
    public void noMatches() throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), "raw/en.canon", new String[]{"^zzzzqqq:"});
            RegExpNative.Report rep = waitFor(rn, 30000);
            assertFalse("error: " + rn.getError(), rep.error);
            assertEquals(0, rep.matches);
        } finally {
            rn.free();
        }
    }

    @Test
    public void missingAssetFails() throws Exception {
        RegExpNative rn = new RegExpNative();
        try {
            rn.startThread(assets(), "raw/no-such.gz", new String[]{"^x:"});
            RegExpNative.Report rep = rn.getProgress();
            assertTrue(rep.error);
            assertFalse(rep.running);
            assertTrue(rn.getError().contains("Can't open asset"));
        } finally {
            rn.free();
        }
    }
}