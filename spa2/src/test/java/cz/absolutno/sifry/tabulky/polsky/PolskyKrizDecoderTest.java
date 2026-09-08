package cz.absolutno.sifry.tabulky.polsky;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PolskyKrizDecoderTest {

    private Locale saved;

    @Before
    public void saveLocale() {
        saved = Locale.getDefault();
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(saved);
    }

    private static int[] kriz(int a, int b, int c) {
        return new int[]{a, b, c};
    }

    @Test
    public void czechLetters() {
        Locale.setDefault(new Locale("cs", "CZ"));
        PolskyKrizDecoder d = new PolskyKrizDecoder();
        assertEquals("A", d.decode(kriz(0, 0, 0)));
        assertEquals("H", d.decode(kriz(0, 2, 1)));
        assertEquals("CH", d.decode(kriz(0, 2, 2)));
        assertEquals("Z", d.decode(kriz(2, 2, 2)));
    }

    @Test
    public void czechWithoutCh() {
        Locale.setDefault(new Locale("cs", "CZ"));
        PolskyKrizDecoder d = new PolskyKrizDecoder("-Ch");
        assertEquals("A", d.decode(kriz(0, 0, 0)));
        assertEquals("I", d.decode(kriz(0, 2, 2)));
        assertEquals("?", d.decode(kriz(2, 2, 2)));
    }

    @Test
    public void englishVariantIsPlain26() {
        Locale.setDefault(Locale.ENGLISH);
        PolskyKrizDecoder d = new PolskyKrizDecoder();
        assertEquals("A", d.decode(kriz(0, 0, 0)));
        assertEquals("I", d.decode(kriz(0, 2, 2)));
        assertEquals("?", d.decode(kriz(2, 2, 2)));
    }

    @Test
    public void setVarSwitchesInstance() {
        Locale.setDefault(new Locale("cs", "CZ"));
        PolskyKrizDecoder d = new PolskyKrizDecoder();
        d.setVar("-Ch");
        assertEquals("I", d.decode(kriz(0, 2, 2)));
        assertEquals("?", d.decode(kriz(2, 2, 2)));
        d.setVar("");
        assertEquals("CH", d.decode(kriz(0, 2, 2)));
        assertEquals("Z", d.decode(kriz(2, 2, 2)));
    }
}