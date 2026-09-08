package cz.absolutno.sifry.tabulky.mobil;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class MobilDecoderTest {

    private static ArrayList<Integer> codes(int... x) {
        ArrayList<Integer> l = new ArrayList<>(x.length);
        for (int i : x) l.add(i);
        return l;
    }

    @Test
    public void decodesTuple() {
        MobilDecoder d = new MobilDecoder();
        assertEquals("WORD", d.decode(codes(0x80, 0x52, 0x62, 0x20)));
        assertEquals("W", d.decode(0x80));
        assertEquals("O", d.decode(0x52));
        assertEquals("R", d.decode(0x62));
        assertEquals("D", d.decode(0x20));
    }

    @Test
    public void decodesSingleKeyFirstLetter() {
        assertEquals("J", new MobilDecoder().decode(0x40));
    }

    @Test
    public void invalidKeyOrRepReturnsNull() {
        MobilDecoder d = new MobilDecoder();
        assertNull(d.decode(0));   // key 0 has no letters
        assertNull(d.decode(0x90)); // key 9 has no letters
        assertNull(d.decode(0x53)); // key 5 'MNO' is length 3, rep 3 invalid
    }

    @Test
    public void encodesWord() {
        MobilDecoder d = new MobilDecoder();
        ArrayList<Integer> out = new ArrayList<>();
        assertFalse(d.encode("WORD", out));
        assertEquals(codes(0x80, 0x52, 0x62, 0x20), out);
    }

    @Test
    public void encodesLowercaseSameAsUppercase() {
        MobilDecoder d = new MobilDecoder();
        ArrayList<Integer> a = new ArrayList<>();
        ArrayList<Integer> b = new ArrayList<>();
        d.encode("word", a);
        d.encode("WORD", b);
        assertEquals(b, a);
    }

    @Test
    public void letterAccessors() {
        MobilDecoder d = new MobilDecoder();
        assertEquals(3, d.getNumLetters(1));
        assertEquals("S", d.getLetter(6, 3));
        assertEquals("W", d.getLetter(8, 0));
        assertArrayEquals(new String[]{"P", "Q", "R", "S"}, d.getLetters(6));
    }
}