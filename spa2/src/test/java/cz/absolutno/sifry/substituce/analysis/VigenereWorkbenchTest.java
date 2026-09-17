package cz.absolutno.sifry.substituce.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Pure-JVM tests for the interactive workbench controller that drives the
 * key-wheel UI (issue #37, step 2). None of this needs an Android device, so the
 * whole interaction model is covered by the CI `testDebugUnitTest` suite.
 */
public final class VigenereWorkbenchTest {

    private static final VigenereConvention CONV = VigenereConvention.AMINUSB0;
    private static final String CT = "LXFOPVEFRNHR";
    private static final String PT = "ATTACKATDAWN";
    private static final String KEY = "LEMON";

    @Test
    public void newWorkbenchHasEmptySlotsAndFirstSelected() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        assertEquals(KEY.length(), wb.getKeyLength());
        assertEquals(KEY.length(), wb.getSlots().size());
        assertEquals(0, wb.getSelectedSlot());
        for (int s = 0; s < KEY.length(); s++) {
            assertFalse("slot " + s, wb.getSlot(s).hasKeyLetter());
            assertFalse("slot " + s, wb.isChanged(s));
            assertFalse("slot " + s, wb.getSlot(s).isLocked());
        }
    }

    @Test
    public void unassignedSlotsLeavePlaintextUnchanged() {
        VigenereWorkbench wb = new VigenereWorkbench("abc", VigenereConvention.APLUSB0, 3);
        // Neutral 'A' (ordinal 0) is the identity for every convention.
        assertEquals("ABC", wb.getPlaintext());
    }

    @Test
    public void selectingAndSteppingWrapsBothDirections() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        wb.select(2);
        assertEquals(2, wb.getSelectedSlot());
        // First step from empty lands on A, then B ...
        wb.stepSelected(1);
        assertEquals(Character.valueOf('A'), wb.getSlot(2).getKeyLetter());
        wb.stepSelected(1);
        assertEquals(Character.valueOf('B'), wb.getSlot(2).getKeyLetter());
        // ... and stepping down from A wraps to Z.
        wb.stepSelected(-1);
        wb.stepSelected(-1);
        assertEquals(Character.valueOf('Z'), wb.getSlot(2).getKeyLetter());
        assertTrue(wb.isChanged(2));
    }

    @Test
    public void steppingOneSlotDoesNotTouchOthers() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        // First swipe on a blank slot just reveals 'A'; the second applies delta.
        wb.step(1, 5);
        assertEquals(Character.valueOf('A'), wb.getSlot(1).getKeyLetter());
        wb.step(1, 5);
        assertEquals(Character.valueOf('F'), wb.getSlot(1).getKeyLetter());
        assertFalse(wb.getSlot(0).hasKeyLetter());
        assertFalse(wb.getSlot(2).hasKeyLetter());
    }

    @Test
    public void setSlotLetterMarksAndClearsChanged() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        wb.setSlotLetter(0, 'K');
        assertTrue(wb.isChanged(0));
        wb.setSlotLetter(0, null);
        assertFalse(wb.isChanged(0));
        assertFalse(wb.getSlot(0).hasKeyLetter());
    }

    @Test
    public void setSlotLetterRejectsNonLetters() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        assertThrows(IllegalArgumentException.class, () -> wb.setSlotLetter(0, 'k'));
        assertThrows(IllegalArgumentException.class, () -> wb.setSlotLetter(0, '1'));
        assertThrows(IllegalArgumentException.class, () -> wb.select(KEY.length()));
        assertThrows(IllegalArgumentException.class, () -> wb.select(-1));
    }

    @Test
    public void toggleLockKeepsLetterAndFlipsBothWays() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        wb.setSlotLetter(3, 'O');
        wb.toggleLock(3);
        assertTrue(wb.getSlot(3).isLocked());
        assertEquals(Character.valueOf('O'), wb.getSlot(3).getKeyLetter());
        wb.toggleLock(3);
        assertFalse(wb.getSlot(3).isLocked());
        assertEquals(Character.valueOf('O'), wb.getSlot(3).getKeyLetter());
    }

    @Test
    public void settingFullKeyDerivesTextbookPlaintext() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        for (int s = 0; s < KEY.length(); s++)
            wb.setSlotLetter(s, KEY.charAt(s));
        assertEquals(PT, wb.getPlaintext());
    }

    @Test
    public void matchingKeysGroupsOnlyRepeatedLetters() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, 4);
        wb.setSlotLetter(0, 'K');
        wb.setSlotLetter(1, 'X'); // singleton -> no evidence
        wb.setSlotLetter(2, 'K');
        // slot 3 intentionally left blank -> never reported
        Map<Integer, List<Integer>> match = wb.matchingKeys();
        assertEquals(2, match.size());
        assertEquals(Integer.valueOf(2), match.get(0).get(0));
        assertEquals(Integer.valueOf(0), match.get(2).get(0));
        assertFalse(match.containsKey(1));
        assertFalse(match.containsKey(3));
    }

    @Test
    public void withKeyLengthPreservesBelowAndDropsAbove() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, 4);
        wb.setSlotLetter(0, 'K');
        wb.toggleLock(0);
        wb.setSlotLetter(1, 'B');
        wb.setSlotLetter(3, 'D');
        wb.select(3);

        VigenereWorkbench shorter = wb.withKeyLength(2);
        assertNotSame(wb, shorter);
        assertEquals(2, shorter.getKeyLength());
        assertEquals(Character.valueOf('K'), shorter.getSlot(0).getKeyLetter());
        assertTrue(shorter.getSlot(0).isLocked());
        assertTrue(shorter.isChanged(0));
        assertEquals(Character.valueOf('B'), shorter.getSlot(1).getKeyLetter());
        // Selection was clamped from 3 into [0, 2).
        assertEquals(1, shorter.getSelectedSlot());
        // The original is untouched.
        assertEquals(4, wb.getKeyLength());
    }

    @Test
    public void withConventionCarriesKeyLetters() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        for (int s = 0; s < KEY.length(); s++)
            wb.setSlotLetter(s, KEY.charAt(s));
        VigenereWorkbench plus = wb.withConvention(VigenereConvention.APLUSB0);
        assertEquals(VigenereConvention.APLUSB0, plus.getConvention());
        for (int s = 0; s < KEY.length(); s++)
            assertEquals(Character.valueOf(KEY.charAt(s)), plus.getSlot(s).getKeyLetter());
        // Same key, different convention -> different plaintext.
        assertFalse(PT.equals(plus.getPlaintext()));
    }

    @Test
    public void boundariesStartEmptyAndAreDecorative() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        for (int s = 0; s < KEY.length(); s++)
            wb.setSlotLetter(s, KEY.charAt(s));
        assertTrue(wb.getBoundaries().isEmpty());
        String plain = wb.getPlaintext();
        for (int i = 0; i < wb.getLetterCount() - 1; i++)
            wb.toggleBoundaryAfter(i);
        assertEquals(wb.getLetterCount() - 1, wb.getBoundaries().size());
        // Boundaries annotate the text; they never change derivation or the key.
        assertEquals(plain, wb.getPlaintext());
    }

    @Test
    public void toggleBoundaryFlipsTheSameGap() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        assertFalse(wb.hasBoundaryAfter(2));
        wb.toggleBoundaryAfter(2);
        assertTrue(wb.hasBoundaryAfter(2));
        wb.toggleBoundaryAfter(2);
        assertFalse(wb.hasBoundaryAfter(2));
        assertTrue(wb.getBoundaries().isEmpty());
    }

    @Test
    public void boundariesAreKeyedByLetterIndexNotAbsolutePosition() {
        // 7 letters (L X F O P V E); spaces and punctuation are not included in
        // the coordinate space, so a gap index skips over every non-letter.
        VigenereWorkbench wb = new VigenereWorkbench("LXF OP-VE", VigenereConvention.APLUSB0, 3);
        assertEquals(7, wb.getLetterCount());
        wb.toggleBoundaryAfter(2); // gap between F and O
        assertTrue(wb.hasBoundaryAfter(2));
        assertFalse(wb.hasBoundaryAfter(5));
        assertFalse(wb.hasBoundaryAfter(6));
    }

    @Test
    public void invalidBoundaryGapsAreRejected() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        assertThrows(IllegalArgumentException.class, () -> wb.toggleBoundaryAfter(-1));
        // The last letter has no gap after it.
        assertThrows(IllegalArgumentException.class, () -> wb.toggleBoundaryAfter(wb.getLetterCount() - 1));
        assertThrows(IllegalArgumentException.class, () -> wb.toggleBoundaryAfter(wb.getLetterCount()));
    }

    @Test
    public void singleLetterAndEmptyCiphertextHaveNoBoundaryGaps() {
        VigenereWorkbench single = new VigenereWorkbench("x", VigenereConvention.APLUSB0, 1);
        assertEquals(1, single.getLetterCount());
        assertThrows(IllegalArgumentException.class, () -> single.toggleBoundaryAfter(0));
        VigenereWorkbench empty = new VigenereWorkbench("", VigenereConvention.APLUSB0, 1);
        assertEquals(0, empty.getLetterCount());
        assertThrows(IllegalArgumentException.class, () -> empty.toggleBoundaryAfter(0));
    }

    @Test
    public void getBoundariesIsAnUnmodifiableView() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, KEY.length());
        wb.toggleBoundaryAfter(3);
        assertThrows(UnsupportedOperationException.class, () -> wb.getBoundaries().add(5));
    }

    @Test
    public void boundariesSurviveKeyLengthAndConventionChanges() {
        VigenereWorkbench wb = new VigenereWorkbench(CT, CONV, 5);
        wb.setSlotLetter(0, 'L');
        wb.toggleBoundaryAfter(2);
        wb.toggleBoundaryAfter(7);
        VigenereWorkbench resized = wb.withKeyLength(3);
        assertTrue(resized.hasBoundaryAfter(2));
        assertTrue(resized.hasBoundaryAfter(7));
        assertFalse(wb.getBoundaries().isEmpty());
        VigenereWorkbench switched = wb.withConvention(VigenereConvention.APLUSB1);
        assertTrue(switched.hasBoundaryAfter(2));
        assertTrue(switched.hasBoundaryAfter(7));
    }

}
