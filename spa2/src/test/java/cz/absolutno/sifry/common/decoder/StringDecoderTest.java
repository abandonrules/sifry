package cz.absolutno.sifry.common.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cz.absolutno.sifry.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class StringDecoderTest {

    @Test
    public void decodesValueAfterColon() {
        StringDecoder d = new StringDecoder(R.array.saBRLetters);
        assertEquals("A", d.decode(1));
        assertEquals("B", d.decode(3));
        assertEquals("W", d.decode(58));
        assertNull(d.decode(0));
    }

    @Test
    public void isAlwaysTemp() {
        assertTrue(new StringDecoder(R.array.saBRLetters).isTemp());
    }

    @Test
    public void descriptionComesFromMiddleSegment() {
        StringDecoder d = new StringDecoder(R.array.saBRLetters);
        // entry is "1:A"; desc is the middle segment ("A")
        assertEquals("A", d.getDesc(1));
        assertNull(d.getDesc(0));
    }

    @Test
    public void invalidInnermostColonStillParses() {
        // Braille letters have no inner colon, but a malformed entry still
        // rounds trip: make sure a code that does not exist stays null.
        assertEquals(null, new StringDecoder(R.array.saBRLetters).decode(12345));
    }
}