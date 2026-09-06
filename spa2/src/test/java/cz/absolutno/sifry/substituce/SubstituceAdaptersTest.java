package cz.absolutno.sifry.substituce;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cz.absolutno.sifry.common.alphabet.PlainEnglishAlphabet;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class SubstituceAdaptersTest {

    private static PlainEnglishAlphabet en() {
        return new PlainEnglishAlphabet();
    }

    @Test
    public void posuny() {
        PosunyAdapter p = new PosunyAdapter(en());
        assertEquals(0, p.getCount());
        p.setInput("AB");
        assertEquals(26, p.getCount());
        assertEquals("AB", p.getItem(0));
        assertEquals("BC", p.getItem(1));
        assertEquals("ZA", p.getItem(25));
        p.clear();
        assertEquals(0, p.getCount());
    }

    @Test
    public void atbash() {
        AtbashAdapter a = new AtbashAdapter(en());
        a.setInput("AB");
        assertEquals("ZY", a.getItem(0));
    }

    @Test
    public void afinni() {
        AfinniAdapter f = new AfinniAdapter(en());
        f.setInput("AB");
        assertEquals("BC", f.getItem(1));
        f.setCoeff(3);
        assertEquals("AD", f.getItem(0));
    }

    @Test
    public void heslo() {
        HesloAdapter h = new HesloAdapter(en());
        assertEquals(0, h.getCount());
        h.setKey("B");
        h.setInput("A");
        assertEquals(h.getCountValid(), h.getCount());
        assertEquals("C", h.getItem(0));
        assertEquals("Y", h.getItem(1));
        assertEquals("A", h.getItem(2));
        assertEquals("B", h.getItem(3));
        assertEquals("Z", h.getItem(4));
        assertEquals("B", h.getItem(5));
    }

    @Test
    public void autokey() {
        AutokeyAdapter k = new AutokeyAdapter(en());
        k.setInput("AB");
        assertEquals("AC", k.getItem(0));
        assertEquals("AA", k.getItem(1));
        assertEquals("AC", k.getItem(12));
    }

    @Test
    public void pozice() {
        PoziceAdapter p = new PoziceAdapter(en());
        p.setInput("AB");
        assertEquals("BD", p.getItem(0));
        assertEquals("ZZ", p.getItem(1));
        assertEquals("AC", p.getItem(3));
        assertEquals("AA", p.getItem(4));
        assertEquals("AA", p.getItem(5));
        assertEquals("AB", p.getItem(6));
        assertEquals("AB", p.getItem(7));
    }

    @Test
    public void klic() {
        KlicAdapter k = new KlicAdapter(en());
        assertEquals(0, k.getCount());
        k.setKlic("SIF", 0);
        k.setInput("SIF");
        assertEquals(2, k.getCount());
        assertEquals("ABC", k.getItem(0));
        k.clear();
        assertEquals(0, k.getCount());
        k.setKlic("SIF", 0);
        k.setInput("ABC");
        assertEquals("SIF", k.getItem(1));
    }
}