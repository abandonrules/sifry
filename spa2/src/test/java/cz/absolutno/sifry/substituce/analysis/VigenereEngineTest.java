package cz.absolutno.sifry.substituce.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

/**
 * Pure-JVM tests for the Vigenère cipher engine: slot alignment, plaintext
 * derivation (full and per-column), and required-key derivation with its
 * conflict report. Decryption uses the A-B (od 0) convention, which recovers
 * the textbook plaintext from the textbook ciphertext, independent of any UI.
 */
public final class VigenereEngineTest {

    private static final VigenereConvention CONV = VigenereConvention.AMINUSB0;
    // "LEMON" key, convention A-B (od 0): decrypting "LXFOPVEFRNHR" gives "ATTACKATDAWN"
    private static final String CT = "LXFOPVEFRNHR";
    private static final String PT = "ATTACKATDAWN";
    private static final String KEY = "LEMON";

    @Test
    public void alignAssignsRunningSlotModuloKeyLength() {
        int keyLength = KEY.length();
        VigenereEngine.Alignment a = VigenereEngine.align(CT, keyLength);
        assertEquals(CT.length(), a.length());
        int letter = 0;
        for (int i = 0; i < CT.length(); i++) {
            if (Character.isLetter(CT.charAt(i))) {
                assertEquals(letter % keyLength, a.slotAt(i));
                letter++;
            } else {
                assertEquals(-1, a.slotAt(i));
            }
        }
    }

    @Test
    public void nonLettersDoNotConsumeKeySlots() {
        // A comma and a space sit between letters; they must not advance the slot.
        String mixed = "HELLO, WORLD";
        VigenereEngine.Alignment a = VigenereEngine.align(mixed, 3);
        // Letter index -> expected slot: H=0 E=1 L=2 L=0 O=1 (comma, space) W=2 O=0 R=1 L=2 D=0
        int[] expected = {0, 1, 2, 0, 1, -1, -1, 2, 0, 1, 2, 0};
        assertEquals(expected.length, a.length());
        for (int i = 0; i < expected.length; i++)
            assertEquals("pos " + i, expected[i], a.slotAt(i));
    }

    @Test
    public void derivePlainRecoversTextbookPlaintext() {
        String plain = VigenereEngine.derivePlain(CT, CONV, KEY);
        assertEquals(PT, plain);
    }

    @Test
    public void derivePlainKeepsNonLettersInPlace() {
        String mixed = "LXFOPV EFR-NHR"; // same letters, separated by a space and a hyphen
        String plain = VigenereEngine.derivePlain(mixed, CONV, KEY);
        assertEquals("ATTACK ATD-AWN", plain);
    }

    @Test
    public void derivePlainColumnRecomputesOnlyOneSlot() {
        // Recomputing every slot one at a time must build the full plaintext.
        String current = VigenereEngine.derivePlain(CT, CONV, KEY);
        for (int slot = 0; slot < KEY.length(); slot++) {
            String col = VigenereEngine.derivePlainColumn(CT, CONV, KEY, slot, current);
            assertEquals("slot " + slot, PT, col);
        }
    }

    @Test
    public void derivePlainColumnPreservesOtherSlots() {
        // Slot 0 governs positions 0, 5 and 10 ("AKW" in the plaintext). Corrupting
        // them and then asking for slot 1 must keep the corruption untouched while
        // slot 1 letters are recomputed; an implementation that silently ignores
        // onlySlot and recomputes everything would wipe the 'Z' markers.
        String current = VigenereEngine.derivePlain(CT, CONV, KEY);
        char[] corrupted = current.toCharArray();
        corrupted[0] = 'Z';
        corrupted[5] = 'Z';
        corrupted[10] = 'Z';
        String col = VigenereEngine.derivePlainColumn(CT, CONV, KEY, 1, new String(corrupted));
        assertEquals('Z', col.charAt(0));
        assertEquals('Z', col.charAt(5));
        assertEquals('Z', col.charAt(10));
        assertEquals(PT.charAt(1), col.charAt(1));
        assertEquals(PT.charAt(6), col.charAt(6));
        assertEquals(PT.charAt(11), col.charAt(11));
        assertEquals(PT.charAt(2), col.charAt(2));
    }

    @Test
    public void deriveRequiredKeyRoundTripsWithDerivePlain() {
        VigenereEngine.DerivedKey dk = VigenereEngine.deriveRequiredKey(CT, PT, CONV, KEY.length());
        assertFalse(dk.hasConflicts());
        assertTrue(dk.getConflicts().isEmpty());
        Map<Integer, Character> required = dk.getRequired();
        assertNotNull(required);
        // Every slot that governs at least one letter must have a required key letter,
        // and (with no conflicts) assembling them must reproduce the original key.
        StringBuilder rebuilt = new StringBuilder();
        for (int slot = 0; slot < KEY.length(); slot++) {
            Character letter = required.get(slot);
            assertNotNull("slot " + slot, letter);
            rebuilt.append(letter);
        }
        assertEquals(KEY, rebuilt.toString());
    }

    @Test
    public void deriveRequiredKeyReportsConflicts() {
        // Two plaintext letters mapped to the same slot with different required
        // key letters must be reported, not silently overwritten.
        String badPlain = "ATTACKATDLWN"; // last letter drifted one column
        VigenereEngine.DerivedKey dk = VigenereEngine.deriveRequiredKey(CT, badPlain, CONV, KEY.length());
        assertTrue(dk.hasConflicts());
        assertFalse(dk.getConflicts().isEmpty());
    }

    @Test
    public void requiredKeyConventionRoundTrip() {
        // derivePlain(deriveRequiredKey inverse): combine->requiredKey is the same
        // as convention.requiredKey(combine(c,k), c) == k, checked via the engine.
        for (VigenereConvention conv : VigenereConvention.values()) {
            for (int cipher = 0; cipher < 26; cipher++) {
                for (int key = 0; key < 26; key++) {
                    int plain = conv.combine(cipher, key);
                    assertEquals("conv=" + conv + " cipher=" + cipher + " key=" + key,
                            key, conv.requiredKey(cipher, plain));
                }
            }
        }
    }

    @Test
    public void lowercaseInputIsFoldedBeforeEncrypting() {
        // Pasted text is often lower case; the engine documents that it folds to
        // upper case, so the ciphertext and the key must both be case-insensitive.
        assertEquals(PT, VigenereEngine.derivePlain(CT.toLowerCase(), CONV, KEY));
        assertEquals(PT, VigenereEngine.derivePlain(CT, CONV, KEY.toLowerCase()));
        VigenereEngine.DerivedKey dk = VigenereEngine.deriveRequiredKey(CT, PT.toLowerCase(), CONV, KEY.length());
        assertFalse(dk.hasConflicts());
    }

    @Test
    public void alignRejectsNonPositiveKeyLength() {
        try {
            VigenereEngine.align(CT, 0);
            assertTrue("expected IllegalArgumentException", false);
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void isLetterRangeOnAlignment() {
        // Sanity: every slot is either in [0, keyLength) or -1.
        int keyLength = 7;
        VigenereEngine.Alignment a = VigenereEngine.align(CT + "!", keyLength);
        for (int i = 0; i < a.length(); i++) {
            int s = a.slotAt(i);
            assertTrue(s >= -1 && s < keyLength);
        }
    }
}
