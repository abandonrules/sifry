package cz.absolutno.sifry.common.datapack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class GreekDataPackTest {

    private GreekDataPack pack;

    @Before
    public void setUp() throws IOException {
        AssetManager assets = RuntimeEnvironment.getApplication().getApplicationContext().getAssets();
        pack = Packs.greek(assets);
    }

    @Test
    public void symbolPiToName() {
        EntityResult r = pack.bySymbol("π");
        assertNotNull(r);
        assertEquals("Pi", r.displayText());
    }

    @Test
    public void symbolPiToOrdinal() {
        EntityResult r = pack.bySymbol("π");
        assertEquals("16", r.property("ordinal"));
    }

    @Test
    public void omegaSymbolToOrdinal24() {
        EntityResult r = pack.bySymbol("Ω");
        assertNotNull(r);
        assertEquals("24", r.property("ordinal"));
        assertEquals("Omega", r.displayText());
    }

    @Test
    public void finalSigmaAlias() {
        EntityResult r = pack.bySymbol("ς");
        assertNotNull(r);
        assertEquals("Sigma", r.displayText());
        assertEquals(18, Integer.parseInt(r.property("ordinal")));
    }

    @Test
    public void nameLookup() {
        EntityResult r = pack.byName("pi");
        assertNotNull(r);
        assertEquals("16", r.property("ordinal"));
    }

    @Test
    public void ordinalLookup() {
        EntityResult r = pack.byOrdinal(16);
        assertNotNull(r);
        assertEquals("Pi", r.displayText());
        assertEquals("π", r.property("lowercase"));
        EntityResult omega = pack.byOrdinal(24);
        assertNotNull(omega);
        assertEquals("Omega", omega.displayText());
    }

    @Test
    public void upperAndLowerSymbols() {
        assertEquals("Alpha", pack.bySymbol("α").displayText());
        assertEquals("Alpha", pack.bySymbol("Α").displayText());
    }

    @Test
    public void everyLetterHasBothForms() {
        String[] names = { "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta",
                "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma",
                "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega" };
        for (String n : names) {
            EntityResult r = pack.byName(n);
            assertNotNull(n, r);
            assertTrue(n + " lowercase", r.property("lowercase") != null);
            assertTrue(n + " uppercase", r.property("uppercase") != null);
            assertNotNull(n + " ordinal", r.property("ordinal"));
        }
    }

    @Test
    public void searchBySymbol() {
        assertEquals("Pi", pack.search(FilterSpec.identity("π")).results().get(0).displayText());
    }

    @Test
    public void searchByOrdinal() {
        assertEquals("Pi", pack.search(FilterSpec.identity("16")).results().get(0).displayText());
    }
}