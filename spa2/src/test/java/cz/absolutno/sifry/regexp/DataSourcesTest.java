package cz.absolutno.sifry.regexp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class DataSourcesTest {

    private SharedPreferences sp;

    @Before
    public void setUp() {
        Context ctx = RuntimeEnvironment.getApplication().getApplicationContext();
        sp = ctx.getSharedPreferences("datasources-test", Context.MODE_PRIVATE);
        sp.edit().clear().commit();
    }

    @Test
    public void utilIsDefaultEnabledWhenUnset() {
        assertTrue(DataSources.isEnabled(sp, "cs.canon"));
        assertTrue(DataSources.isEnabled(sp, "wordle.canon"));
        assertTrue(DataSources.isEnabled(null, "cs.canon"));
    }

    @Test
    public void unknownFilenameIsNeverEnabled() {
        assertFalse(DataSources.isEnabled(sp, "nope.canon"));
    }

    @Test
    public void resolveKeepsAnEnabledSelection() {
        sp.edit().putBoolean("pref_source_wordle", false).commit();
        assertEquals("cs.canon", DataSources.resolve(sp, "cs.canon", "en.canon"));
    }

    @Test
    public void resolveFallsBackToFirstEnabledWhenSelectionDisabled() {
        sp.edit().putBoolean("pref_source_cs", false).commit();
        sp.edit().putBoolean("pref_source_en", false).commit();
        assertEquals("periodic.canon", DataSources.resolve(sp, "cs.canon", "cs.canon"));
    }

    @Test
    public void resolveHasSensibleOrderForFallback() {
        sp.edit().putBoolean("pref_source_cs", false).commit();
        sp.edit().putBoolean("pref_source_en", false).commit();
        sp.edit().putBoolean("pref_source_periodic", false).commit();
        sp.edit().putBoolean("pref_source_pokemon", false).commit();
        assertEquals("wordle.canon", DataSources.resolve(sp, "cs.canon", "cs.canon"));
    }

    @Test
    public void resolveSendsEmptySelectionToFallback() {
        sp.edit().putBoolean("pref_source_cs", false).commit();
        String r = DataSources.resolve(sp, "", "cs.canon");
        assertTrue(DataSources.isEnabled(sp, r));
    }

    @Test
    public void resolveReturnsSelectionWhenNothingEnabled() {
        for (String key : DataSources.PREF_KEYS)
            sp.edit().putBoolean(key, false).commit();
        assertEquals("wordle.canon", DataSources.resolve(sp, "wordle.canon", "cs.canon"));
    }

    @Test
    public void firstEnabledOrderMatchesAssets() {
        sp.edit().putBoolean("pref_source_cs", false).commit();
        sp.edit().putBoolean("pref_source_en", false).commit();
        assertEquals("periodic.canon", DataSources.firstEnabled(sp));
        for (String key : DataSources.PREF_KEYS)
            sp.edit().putBoolean(key, false).commit();
        assertNull(DataSources.firstEnabled(sp));
    }
}