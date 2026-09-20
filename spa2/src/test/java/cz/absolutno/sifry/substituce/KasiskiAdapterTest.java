package cz.absolutno.sifry.substituce;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cz.absolutno.sifry.common.alphabet.PlainEnglishAlphabet;

/**
 * Unit coverage for the Kasiski evidence rows (issue #37, step 8). The cipher
 * maths lives in {@code KasiskiAnalyzer}; here we check the adapter turns it
 * into the expected human-readable evidence strings.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class KasiskiAdapterTest {

    private static KasiskiAdapter adapter() {
        return new KasiskiAdapter(new PlainEnglishAlphabet());
    }

    @Test
    public void rendersSequencePositionsDistancesAndFactors() {
        // "ABC" occurs at offsets 0, 5 and 9 in the normalized letters; the
        // distances are therefore 5 and 4, and 4 factors as 2.2.
        KasiskiAdapter a = adapter();
        a.setInput("ABCxyABCzABC");
        assertEquals(1, a.getCount());
        assertEquals("ABC  \u00d73  @0, 5, 9", a.getItemDesc(0));
        assertEquals("5=5, 4=2\u00b72", a.getItem(0));
    }

    @Test
    public void noRepeatsYieldsNoRows() {
        KasiskiAdapter a = adapter();
        a.setInput("ABCDEFGH");
        assertEquals(0, a.getCount());
    }
}
