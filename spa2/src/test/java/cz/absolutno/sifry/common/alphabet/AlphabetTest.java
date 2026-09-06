package cz.absolutno.sifry.common.alphabet;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AlphabetTest {

    private Locale saved;

    @Before
    public void saveLocale() {
        saved = Locale.getDefault();
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(saved);
    }

    private static ArrayList<Integer> ords(StringParser sp) {
        ArrayList<Integer> out = new ArrayList<>();
        int o;
        while ((o = sp.getNextOrd()) != StringParser.EOF)
            out.add(o);
        return out;
    }

    @Test
    public void englishDefault26() {
        Locale.setDefault(Locale.ENGLISH);
        Alphabet a = Alphabet.getVariantInstance(26, "");
        assertEquals(26, a.count());
        assertEquals("Z", a.chr(25));
        assertEquals(25, a.ord("Z"));
        assertEquals("?", a.chr(-1));
        assertEquals(StringParser.ERR, a.ord("CH"));
    }

    @Test
    public void english25DropsQ() {
        Locale.setDefault(Locale.ENGLISH);
        Alphabet a = Alphabet.getVariantInstance(25, "");
        assertEquals(25, a.count());
        assertEquals("R", a.chr(16));
        assertEquals("Z", a.chr(24));
        assertEquals(StringParser.ERR, a.ord("Q"));
        assertEquals(24, a.ord("Z"));
        assertEquals(9, a.ord("J"));
    }

    @Test
    public void english25J() {
        Locale.setDefault(Locale.ENGLISH);
        Alphabet a = Alphabet.getVariantInstance(25, "J");
        assertEquals(25, a.count());
        assertEquals("I", a.chr(8));
        assertEquals("K", a.chr(9));
        assertEquals(8, a.ord("J"));
        assertEquals(8, a.ord("I"));
    }

    @Test
    public void english25K() {
        Locale.setDefault(Locale.ENGLISH);
        Alphabet a = Alphabet.getVariantInstance(25, "K");
        assertEquals(25, a.count());
        assertEquals("C", a.chr(2));
        assertEquals("J", a.chr(9));
        assertEquals("L", a.chr(10));
        assertEquals(2, a.ord("K"));
    }

    @Test
    public void english24DropsQX() {
        Locale.setDefault(Locale.ENGLISH);
        Alphabet a = Alphabet.getVariantInstance(24, "");
        assertEquals(24, a.count());
        assertEquals("R", a.chr(16));
        assertEquals("Y", a.chr(22));
        assertEquals("Z", a.chr(23));
        assertEquals(StringParser.ERR, a.ord("Q"));
        assertEquals(StringParser.ERR, a.ord("X"));
        assertEquals(23, a.ord("Z"));
    }

    @Test
    public void english24JQ() {
        Locale.setDefault(Locale.ENGLISH);
        Alphabet a = Alphabet.getVariantInstance(24, "JQ");
        assertEquals(24, a.count());
        assertEquals(StringParser.ERR, a.ord("Q"));
        assertEquals(8, a.ord("J"));
    }

    @Test
    public void czech27Full() {
        Locale.setDefault(new Locale("cs", "CZ"));
        Alphabet a = Alphabet.getVariantInstance(27, "");
        assertEquals(27, a.count());
        assertEquals("CH", a.chr(8));
        assertEquals("I", a.chr(9));
        assertEquals("Z", a.chr(26));
        assertEquals(8, a.ord("CH"));
        assertEquals(2, a.ord("C"));
        assertEquals(StringParser.ERR, a.ord("CHC"));
    }

    @Test
    public void czech27ParserChDigraph() {
        Locale.setDefault(new Locale("cs", "CZ"));
        Alphabet a = Alphabet.getVariantInstance(27, "");
        ArrayList<Integer> o = ords(a.getStringParser("chj"));
        assertEquals(java.util.Arrays.asList(8, 10), o);
        ArrayList<Integer> cc = ords(a.getStringParser("chch"));
        assertEquals(java.util.Arrays.asList(8, 8), cc);
    }

    @Test
    public void czechWithoutCh26() {
        Locale.setDefault(new Locale("cs", "CZ"));
        Alphabet a = Alphabet.getVariantInstance(26, "");
        assertEquals(26, a.count());
        assertEquals(StringParser.ERR, a.ord("CH"));
        assertEquals("I", a.chr(8));
        ArrayList<Integer> o = ords(a.getStringParser("ch"));
        assertEquals(java.util.Arrays.asList(2, 7), o);
    }

    @Test
    public void czech25DropsQ() {
        Locale.setDefault(new Locale("cs", "CZ"));
        Alphabet a = Alphabet.getVariantInstance(25, "");
        assertEquals(25, a.count());
        assertEquals(StringParser.ERR, a.ord("Q"));
        assertEquals(24, a.ord("Z"));
    }

    @Test
    public void czech25J() {
        Locale.setDefault(new Locale("cs", "CZ"));
        Alphabet a = Alphabet.getVariantInstance(25, "J");
        assertEquals(25, a.count());
        assertEquals("I", a.chr(8));
        assertEquals("K", a.chr(9));
        assertEquals(8, a.ord("J"));
    }

    @Test
    public void czech24DropsQWX() {
        Locale.setDefault(new Locale("cs", "CZ"));
        Alphabet a = Alphabet.getVariantInstance(24, "");
        assertEquals(24, a.count());
        assertEquals("X", a.chr(21));
        assertEquals("Z", a.chr(23));
        assertEquals(StringParser.ERR, a.ord("Q"));
        assertEquals(StringParser.ERR, a.ord("W"));
        assertEquals(23, a.ord("Z"));
    }
}