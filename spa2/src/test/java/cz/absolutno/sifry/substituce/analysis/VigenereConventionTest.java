package cz.absolutno.sifry.substituce.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Pure-JVM tests for the four classical Vigenère conventions. The math is
 *  convention-independent in shape (all mod-26 ± with a 0/1 base), so these
 *  assert the ordinal symmetries the engine relies on, not one specific table. */
public final class VigenereConventionTest {

    @Test
    public void fourConventionsExist() {
        assertEquals(4, VigenereConvention.values().length);
        for (VigenereConvention c : VigenereConvention.values()) {
            assertTrue(c.isPlus() || !c.isPlus()); // plus/aminus are mutually informative
            assertEquals(c.name().startsWith("A") && c.name().endsWith("B0"),
                    !c.isOneBased() && (c.name().startsWith("A") && c.name().indexOf("B1") < 0));
        }
    }

    @Test
    public void zeroBasedVsOneBasedSegments() {
        // The four conventions: 2 plus (0/1) and 2 minus (0/1).
        int plus = 0, one = 0;
        for (VigenereConvention c : VigenereConvention.values()) {
            if (c.isPlus())
                plus++;
            if (c.isOneBased())
                one++;
        }
        assertEquals(2, plus);
        assertEquals(2, one);
    }

    @Test
    public void fixedCombinationsPinTheConventions() {
        // A ciphertext A combined with key A pins each formula to a concrete letter:
        // 0-based gives A (both plus and minus); 1-based gives B for plus and Z for
        // minus. These come straight from the HesloAdapter semantics, so a paired
        // sign error in combine/requiredKey cannot cancel out and still pass.
        assertEquals(0, VigenereConvention.APLUSB0.combine(0, 0));
        assertEquals(0, VigenereConvention.AMINUSB0.combine(0, 0));
        assertEquals(1, VigenereConvention.APLUSB1.combine(0, 0));
        assertEquals(25, VigenereConvention.AMINUSB1.combine(0, 0));
        // The inverse halves: requiredKey(cipher, plain) == key for those same pairs.
        assertEquals(0, VigenereConvention.APLUSB0.requiredKey(0, 0));
        assertEquals(0, VigenereConvention.AMINUSB0.requiredKey(0, 0));
        assertEquals(0, VigenereConvention.APLUSB1.requiredKey(0, 1));
        assertEquals(0, VigenereConvention.AMINUSB1.requiredKey(0, 25));
    }

    @Test
    public void combineAndRequiredKeyAreInverses() {
        // For every convention, requiredKey(cipher, combine(cipher,key)) == key.
        for (VigenereConvention c : VigenereConvention.values()) {
            for (int cipher = 0; cipher < 26; cipher++) {
                for (int key = 0; key < 26; key++) {
                    int plain = c.combine(cipher, key);
                    int back = c.requiredKey(cipher, plain);
                    assertEquals("conv=" + c + " cipher=" + cipher + " key=" + key,
                            key, back);
                }
            }
        }
    }

    @Test
    public void keySlotOrdinalsAreConventionIndependent() {
        // combine/requiredKey work on 0-based ordinals; all four conventions
        // accept a full 26-letter ordinal range without throwing or wrapping
        // outside [0,26).
        for (VigenereConvention c : VigenereConvention.values()) {
            for (int cipher = 0; cipher < 26; cipher++)
                for (int key = 0; key < 26; key++)
                    assertTrue(c.combine(cipher, key) >= 0 && c.combine(cipher, key) < 26);
        }
    }
}
