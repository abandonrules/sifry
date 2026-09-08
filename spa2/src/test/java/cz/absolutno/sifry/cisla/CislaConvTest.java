package cz.absolutno.sifry.cisla;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cz.absolutno.sifry.R;

public class CislaConvTest {

    @Test
    public void permParseEmpty() {
        assertEquals(0, CislaConv.parsePerm("", R.id.idCDPerm1));
    }

    @Test
    public void permMissesGray() {
        assertEquals(1, CislaConv.parsePerm("ABCD", R.id.idCDPerm1));
    }

    @Test
    public void permRoundtripAll() {
        for (int x = 0; x <= 23; x++) {
            assertEquals(x + 1, CislaConv.parsePerm(CislaConv.toPerm(x), R.id.idCDPerm1));
        }
    }

    @Test
    public void permToKnownSentinel() {
        assertEquals("ABCD", CislaConv.toPerm(0));
        assertEquals("DCBA", CislaConv.toPerm(23));
    }

    @Test
    public void romanParseKnown() {
        assertEquals(1984, CislaConv.parseRoman("MCMLXXXIV"));
    }

    @Test
    public void romanRejectsIncorrect() {
        assertEquals(-1, CislaConv.parseRoman("IC"));
        assertEquals(-1, CislaConv.parseRoman("ABC"));
    }

    @Test
    public void romanRoundtripSmall() {
        for (int x = 1; x <= 39; x++) {
            assertEquals(x, CislaConv.parseRoman(CislaConv.toRoman(x)));
        }
    }

    @Test
    public void romanZero() {
        assertEquals("\u2013\u2013\u2013\u2013", CislaConv.toRoman(0));
    }

    @Test
    public void binaryTernary() {
        assertEquals("00001101", CislaConv.toBinary(13, 8));
        assertEquals("111", CislaConv.toTernary(13));
        assertEquals("100", CislaConv.toTernary(9));
    }
}