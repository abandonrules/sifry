package cz.absolutno.sifry.morse;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class MorseDecoderTest {

    private static ArrayList<Integer> codes(int... x) {
        ArrayList<Integer> l = new ArrayList<>(x.length);
        for (int i : x) l.add(i);
        return l;
    }

    @Test
    public void letters() {
        MorseDecoder d = new MorseDecoder();
        assertEquals("A", d.decode(5));
        assertEquals("B", d.decode(24));
        assertEquals("E", d.decode(2));
        assertEquals("Z", d.decode(28));
    }

    @Test
    public void digits() {
        MorseDecoder d = new MorseDecoder();
        assertEquals("0", d.decode(63));
        assertEquals("1", d.decode(47));
        assertEquals("2", d.decode(39));
        assertEquals("3", d.decode(35));
        assertEquals("4", d.decode(33));
        assertEquals("5", d.decode(32));
    }

    @Test
    public void punctuation() {
        MorseDecoder d = new MorseDecoder();
        assertEquals(".", d.decode(85));
        assertEquals(",", d.decode(115));
        assertEquals("?", d.decode(76));
        assertEquals("'", d.decode(94));
        assertEquals("\"", d.decode(82));
        assertEquals("/", d.decode(50));
        assertEquals("(", d.decode(54));
        assertEquals(")", d.decode(109));
        assertEquals("&", d.decode(40));
        assertEquals(":", d.decode(120));
        assertEquals(";", d.decode(106));
        assertEquals("=", d.decode(49));
        assertEquals("+", d.decode(42));
        assertEquals("\u2013", d.decode(97));
        assertEquals("_", d.decode(77));
        assertEquals("$", d.decode(137));
        assertEquals("@", d.decode(90));
    }

    @Test
    public void addedSeparatorAndUnknown() {
        MorseDecoder d = new MorseDecoder();
        assertEquals("\u00b7", d.decode(1));
        assertEquals("?", d.decode(0));
        assertEquals("?", d.decode(9999));
    }

    @Test
    public void tuplesAndEncode() {
        MorseDecoder d = new MorseDecoder();
        assertEquals("AB", d.decode(codes(5, 24)));
        ArrayList<Integer> out = new ArrayList<>();
        assertEquals(false, d.encode("AB", out));
        assertEquals(codes(5, 24), out);
        assertEquals(false, d.encode("01", out));
        assertEquals(codes(63, 47), out);
    }

    @Test
    public void digitsTuple() {
        MorseDecoder d = new MorseDecoder();
        assertEquals("012", d.decode(codes(63, 47, 39)));
    }
}