package cz.absolutno.sifry.substituce.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Pure-JVM tests for the VigenereAnalysisState value: key-slot bookkeeping
 *  (letter assignment, lock/unlock, immutable copies) and the state aggregate
 *  whose withConvention/withKeyLength rebuilds drive the workbench. */
public final class VigenereAnalysisStateTest {

    private static final String CT = "SZUWIRSZUWIR";

    private static List<KeySlotState> slots(KeySlotState... items) {
        List<KeySlotState> list = new ArrayList<KeySlotState>();
        for (KeySlotState s : items)
            list.add(s);
        return list;
    }

    private static VigenereAnalysisState state(int keyLength) {
        List<KeySlotState> s = new ArrayList<KeySlotState>();
        for (int i = 0; i < keyLength; i++)
            s.add(new KeySlotState(i));
        return new VigenereAnalysisState(CT, VigenereConvention.APLUSB1, keyLength, s, null);
    }

    @Test
    public void freshKeySlotIsUnlockedWithoutLetter() {
        KeySlotState s = new KeySlotState(2);
        assertEquals(2, s.getSlot());
        assertFalse(s.hasKeyLetter());
        assertEquals(null, s.getKeyLetter());
        assertFalse(s.isLocked());
    }

    @Test
    public void keySlotCarriesLetterAndLock() {
        KeySlotState s = new KeySlotState(1, 'X', true);
        assertEquals(1, s.getSlot());
        assertTrue(s.hasKeyLetter());
        assertEquals(Character.valueOf('X'), s.getKeyLetter());
        assertTrue(s.isLocked());
    }

    @Test
    public void withKeyLetterReturnsDistinctState() {
        KeySlotState base = new KeySlotState(3);
        KeySlotState with = base.withKeyLetter('K');
        assertNotSame(base, with);
        assertFalse(base.hasKeyLetter());
        assertTrue(with.hasKeyLetter());
        assertEquals(Character.valueOf('K'), with.getKeyLetter());
        assertEquals(3, with.getSlot());
        assertFalse(with.isLocked());
    }

    @Test
    public void lockedAndUnlockedCopies() {
        KeySlotState unlocked = new KeySlotState(0, 'A', true).unlockedCopy();
        assertFalse(unlocked.isLocked());
        assertTrue(unlocked.hasKeyLetter());
        assertEquals(Character.valueOf('A'), unlocked.getKeyLetter());

        KeySlotState locked = new KeySlotState(0, 'A', false).lockedCopy();
        assertTrue(locked.isLocked());
        assertTrue(locked.hasKeyLetter());
        assertEquals(Character.valueOf('A'), locked.getKeyLetter());
    }

    @Test
    public void stateGettersRoundTrip() {
        List<KeySlotState> slots = slots(new KeySlotState(0), new KeySlotState(1, 'K', true));
        List<KasiskiAnalyzer.PeriodCandidate> cands = KasiskiAnalyzer.candidatePeriods(CT);
        VigenereAnalysisState st = new VigenereAnalysisState("ABC", VigenereConvention.APLUSB1, 2, slots, cands);
        assertEquals("ABC", st.getCiphertext());
        assertEquals(VigenereConvention.APLUSB1, st.getConvention());
        assertEquals(2, st.getKeyLength());
        assertEquals(2, st.getKeySlots().size());
        assertEquals(slots, st.getKeySlots());
        KeySlotState k = st.getKeySlot(1);
        assertTrue(k.hasKeyLetter());
        assertTrue(k.isLocked());
        assertFalse(st.getPeriodCandidates().isEmpty());
        boolean hasSix = false;
        for (KasiskiAnalyzer.PeriodCandidate pc : st.getPeriodCandidates())
            if (pc.getPeriod() == 6)
                hasSix = true;
        assertTrue("period 6 must be among the candidates", hasSix);
    }

    @Test
    public void keySlotsAreCopiedUnmodifiable() {
        List<KeySlotState> mutable = slots(new KeySlotState(0), new KeySlotState(1));
        VigenereAnalysisState st = new VigenereAnalysisState(CT, VigenereConvention.APLUSB1, 2, mutable, null);
        mutable.clear();
        assertEquals(2, st.getKeySlots().size());
        try {
            st.getKeySlots().add(new KeySlotState(2));
            fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void nullCandidatesBecomeEmptyList() {
        VigenereAnalysisState st = new VigenereAnalysisState(CT, VigenereConvention.APLUSB1, 2,
                slots(new KeySlotState(0), new KeySlotState(1)), null);
        assertTrue(st.getPeriodCandidates().isEmpty());
    }

    @Test
    public void withConventionKeepsEverythingElse() {
        VigenereAnalysisState st = new VigenereAnalysisState(CT, VigenereConvention.AMINUSB1, 4,
                slots(new KeySlotState(0, 'A', true), new KeySlotState(1), new KeySlotState(2), new KeySlotState(3)),
                null);
        VigenereAnalysisState changed = st.withConvention(VigenereConvention.APLUSB0);
        assertNotSame(st, changed);
        assertEquals(VigenereConvention.APLUSB0, changed.getConvention());
        assertEquals(4, changed.getKeyLength());
        assertEquals(CT, changed.getCiphertext());
        assertEquals(st.getKeySlots(), changed.getKeySlots());
        assertEquals(VigenereConvention.AMINUSB1, st.getConvention());
        assertTrue(st.getKeySlots().get(0).hasKeyLetter());
    }

    @Test
    public void withKeyLengthPreservesLettersBelowAndDropsAbove() {
        VigenereAnalysisState st = new VigenereAnalysisState(CT, VigenereConvention.APLUSB1, 4,
                slots(new KeySlotState(0, 'A', true), new KeySlotState(1, 'B', false),
                        new KeySlotState(2), new KeySlotState(3, 'D', true)),
                null);
        VigenereAnalysisState shorter = st.withKeyLength(2);
        assertEquals(2, shorter.getKeyLength());
        assertEquals(2, shorter.getKeySlots().size());
        KeySlotState s0 = shorter.getKeySlot(0);
        assertTrue(s0.hasKeyLetter());
        assertEquals(Character.valueOf('A'), s0.getKeyLetter());
        assertTrue(s0.isLocked());
        KeySlotState s1 = shorter.getKeySlot(1);
        assertTrue(s1.hasKeyLetter());
        assertEquals(Character.valueOf('B'), s1.getKeyLetter());
        assertFalse(s1.isLocked());
        assertEquals(4, st.getKeyLength());
        assertEquals(4, st.getKeySlots().size());
    }

    @Test
    public void withKeyLengthExpandsWithEmptySlots() {
        VigenereAnalysisState st = state(2);
        VigenereAnalysisState longer = st.withKeyLength(5);
        assertNotSame(st, longer);
        assertEquals(5, longer.getKeyLength());
        assertEquals(5, longer.getKeySlots().size());
        for (int i = 0; i < 5; i++)
            assertFalse(longer.getKeySlot(i).hasKeyLetter());
        assertEquals(2, st.getKeyLength());
    }

    @Test
    public void constructorRejectsInvalidArguments() {
        List<KeySlotState> one = slots(new KeySlotState(0));
        assertThrows(IllegalArgumentException.class, () ->
                new VigenereAnalysisState(null, VigenereConvention.APLUSB1, 1, one, null));
        assertThrows(IllegalArgumentException.class, () ->
                new VigenereAnalysisState(CT, null, 1, one, null));
        assertThrows(IllegalArgumentException.class, () ->
                new VigenereAnalysisState(CT, VigenereConvention.APLUSB1, 0, one, null));
        assertThrows(IllegalArgumentException.class, () ->
                new VigenereAnalysisState(CT, VigenereConvention.APLUSB1, 2, one, null));
    }

    @Test
    public void withKeyLengthRejectsZero() {
        assertThrows(IllegalArgumentException.class, () -> state(2).withKeyLength(0));
    }
}