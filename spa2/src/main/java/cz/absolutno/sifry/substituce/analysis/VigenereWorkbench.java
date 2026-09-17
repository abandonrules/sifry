package cz.absolutno.sifry.substituce.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutable, in-memory workbench for the interactive Vigenère / Kasiski screen
 * (issue #37, implementation step 2). It owns exactly the state the key-wheel
 * UI manipulates:
 *
 * <ul>
 *   <li>the ciphertext and the selected letter-combination convention;</li>
 *   <li>the key length and one {@link KeySlotState} per key slot;</li>
 *   <li>the currently selected slot;</li>
 *   <li>which slots the user has changed (the yellow "changed" state);</li>
 *   <li>the derived plaintext and the matching-key evidence.</li>
 * </ul>
 *
 * The class is pure JVM and fully deterministic. It never guesses a key, never
 * ranks hypotheses and never auto-locks anything: the caller (the user) supplies
 * every key letter. Keeping all of this out of the Android View makes the whole
 * interaction model unit-testable on the JVM; the View is meant to be a thin
 * renderer that forwards taps, swipes and button presses to these methods.
 */
public final class VigenereWorkbench {

    /**
     * Key letter used for a slot the user has not assigned yet. 'A' has ordinal
     * 0, which is the neutral element of every supported convention, so an
     * unassigned slot leaves its ciphertext column unchanged in the derived
     * plaintext instead of throwing.
     */
    private static final char NEUTRAL_KEY = 'A';

    /** Number of letters in the (English) alphabet; the key-wheel wraps at 26. */
    private static final int ALPHABET = 26;

    private final String ciphertext;
    private VigenereConvention convention;
    private int keyLength;

    /** One entry per key slot, always exactly {@code keyLength} long. */
    private final List<KeySlotState> slots;

    /**
     * Slots the user has explicitly assigned a letter to. Kept as a set of slot
     * indices so the UI can render the "user changed" state without having to
     * compare against a previous snapshot. Cleared for a slot when its letter is
     * reset to {@code null}.
     */
    private final Set<Integer> changed;

    /** Index of the selected slot, always in {@code [0, keyLength)}. */
    private int selected;

    /**
     * Manually placed word boundaries (issue #37, implementation step 3). Each
     * entry is the index of a plaintext letter after which a boundary sits, so it
     * annotates the gap between two adjacent letters. Boundaries are decorative
     * annotations only: they consume no ciphertext or key position and never
     * change how a letter is derived. They survive key-length and convention
     * changes because they are keyed by letter position, not by slot.
     */
    private final Set<Integer> boundaries;

    /**
     * Creates an empty workbench: every slot unassigned and unlocked, the first
     * slot selected.
     *
     * @param ciphertext the ciphertext to analyse (kept verbatim; the engine
     *                   normalizes case and skips non-letters for alignment)
     * @param convention the letter-combination convention (never silently chosen)
     * @param keyLength  the number of key slots, at least 1
     */
    public VigenereWorkbench(String ciphertext, VigenereConvention convention, int keyLength) {
        if (ciphertext == null)
            throw new IllegalArgumentException("ciphertext is required");
        if (convention == null)
            throw new IllegalArgumentException("convention is required");
        if (keyLength < 1)
            throw new IllegalArgumentException("keyLength must be >= 1");
        this.ciphertext = ciphertext;
        this.convention = convention;
        this.keyLength = keyLength;
        this.slots = new ArrayList<KeySlotState>(keyLength);
        for (int s = 0; s < keyLength; s++)
            this.slots.add(new KeySlotState(s));
        this.changed = new LinkedHashSet<Integer>();
        this.boundaries = new LinkedHashSet<Integer>();
        this.selected = 0;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public VigenereConvention getConvention() {
        return convention;
    }

    public int getKeyLength() {
        return keyLength;
    }

    /** The selected slot index, in {@code [0, keyLength)}. */
    public int getSelectedSlot() {
        return selected;
    }

    /** Immutable view of the current slot states, indexed by slot. */
    public List<KeySlotState> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public KeySlotState getSlot(int slot) {
        checkSlot(slot);
        return slots.get(slot);
    }

    /** True when the user has explicitly assigned a letter to this slot. */
    public boolean isChanged(int slot) {
        checkSlot(slot);
        return changed.contains(slot);
    }

    /** Selects a slot (a single tap in the UI). Does not change any letter. */
    public void select(int slot) {
        checkSlot(slot);
        selected = slot;
    }

    /**
     * Cycles the selected slot's key letter by {@code delta} positions, wrapping
     * A..Z in both directions (a vertical swipe or the accessible up/down
     * controls). See {@link #step(int, int)} for how a blank slot behaves.
     */
    public void stepSelected(int delta) {
        step(selected, delta);
    }

    /**
     * Cycles the given slot's key letter by {@code delta}, wrapping A..Z. An
     * unassigned slot has no letter to step from, so the first swipe simply
     * reveals the end of the alphabet it is heading towards: 'A' for a
     * non-negative step, 'Z' for a negative one. Once a letter is present the
     * full {@code delta} is applied.
     */
    public void step(int slot, int delta) {
        checkSlot(slot);
        Character current = slots.get(slot).getKeyLetter();
        if (current == null) {
            setSlotLetter(slot, delta >= 0 ? 'A' : 'Z');
            return;
        }
        int next = Math.floorMod((current - 'A') + delta, ALPHABET);
        setSlotLetter(slot, (char) ('A' + next));
    }

    /**
     * Assigns (or clears, with {@code null}) a slot's key letter. Only A..Z is
     * accepted; anything else is a programming error and throws. Assigning a
     * letter marks the slot as user-changed, clearing it removes the mark.
     */
    public void setSlotLetter(int slot, Character letter) {
        checkSlot(slot);
        if (letter != null && (letter < 'A' || letter > 'Z'))
            throw new IllegalArgumentException("key letter must be A..Z or null");
        slots.set(slot, slots.get(slot).withKeyLetter(letter));
        if (letter == null)
            changed.remove(slot);
        else
            changed.add(slot);
    }

    /**
     * Toggles the locked state of a slot (long-press in the UI). Locking is a
     * user statement, never inferred; it does not change the letter.
     */
    public void toggleLock(int slot) {
        checkSlot(slot);
        KeySlotState s = slots.get(slot);
        slots.set(slot, s.isLocked() ? s.unlockedCopy() : s.lockedCopy());
    }

    /**
     * Derives the full plaintext from the current key letters. Unassigned slots
     * use the neutral key 'A'; non-letters pass through unchanged.
     */
    public String getPlaintext() {
        return VigenereEngine.derivePlain(ciphertext, convention, keyWithNeutral());
    }

    /**
     * Matching-key evidence: for every slot that currently shares its key letter
     * with at least one other slot, the other slots holding that same letter.
     * This is evidence only — the caller must never auto-change or auto-lock a
     * slot because it appears here.
     */
    public Map<Integer, List<Integer>> matchingKeys() {
        Map<Character, List<Integer>> byLetter = new LinkedHashMap<Character, List<Integer>>();
        for (KeySlotState s : slots) {
            if (!s.hasKeyLetter())
                continue;
            List<Integer> group = byLetter.get(s.getKeyLetter());
            if (group == null) {
                group = new ArrayList<Integer>();
                byLetter.put(s.getKeyLetter(), group);
            }
            group.add(s.getSlot());
        }
        Map<Integer, List<Integer>> out = new LinkedHashMap<Integer, List<Integer>>();
        for (List<Integer> group : byLetter.values()) {
            if (group.size() < 2)
                continue;
            for (int slot : group) {
                List<Integer> others = new ArrayList<Integer>(group);
                others.remove(Integer.valueOf(slot));
                out.put(slot, others);
            }
        }
        return out;
    }

    /**
     * Returns a shallow copy with a different key length. Key letters, lock flags
     * and the changed marks of slots below the new length are kept; slots at or
     * above it are dropped. The selection is clamped into range.
     */
    public VigenereWorkbench withKeyLength(int len) {
        if (len < 1)
            throw new IllegalArgumentException("keyLength must be >= 1");
        VigenereWorkbench copy = new VigenereWorkbench(ciphertext, convention, len);
        for (KeySlotState s : slots) {
            if (s.getSlot() < len && s.hasKeyLetter()) {
                copy.setSlotLetter(s.getSlot(), s.getKeyLetter());
                if (s.isLocked())
                    copy.toggleLock(s.getSlot());
            }
        }
        copy.selected = Math.min(selected, len - 1);
        // Boundaries are keyed by letter position, so a key-length change does
        // not touch them.
        copy.boundaries.addAll(boundaries);
        return copy;
    }

    /**
     * Returns a shallow copy under a different convention. Key letters are
     * ordinals and convention-independent, so they carry over unchanged; only the
     * derived plaintext differs. Boundaries carry over too.
     */
    public VigenereWorkbench withConvention(VigenereConvention c) {
        VigenereWorkbench copy = new VigenereWorkbench(ciphertext, c, keyLength);
        copy.slots.clear();
        copy.slots.addAll(slots);
        copy.changed.addAll(changed);
        copy.boundaries.addAll(boundaries);
        copy.selected = selected;
        return copy;
    }

    /** Number of letters in the ciphertext; the coordinate space of boundaries. */
    public int getLetterCount() {
        int count = 0;
        for (int i = 0; i < ciphertext.length(); i++)
            if (VigenereEngine.isLetter(ciphertext.charAt(i)))
                count++;
        return count;
    }

    /**
     * True when a word boundary has been placed after the given plaintext letter.
     * The index is a letter index ({@code 0 .. getLetterCount()-1}), so non-letter
     * characters never shift it.
     */
    public boolean hasBoundaryAfter(int letterIndex) {
        return boundaries.contains(letterIndex);
    }

    /**
     * Toggles the boundary in the gap after the given plaintext letter. Only gaps
     * that actually sit between two letters are valid, i.e. {@code 0 .. count-2}.
     * A boundary is a user annotation: this never edits a key letter.
     */
    public void toggleBoundaryAfter(int letterIndex) {
        int count = getLetterCount();
        if (letterIndex < 0 || letterIndex >= count - 1)
            throw new IllegalArgumentException("no gap after letter " + letterIndex);
        if (!boundaries.remove(Integer.valueOf(letterIndex)))
            boundaries.add(letterIndex);
    }

    /** Immutable view of the boundary positions (letter indices). */
    public Set<Integer> getBoundaries() {
        return Collections.unmodifiableSet(boundaries);
    }

    /** Builds the key string the engine expects, substituting 'A' for blanks. */
    private String keyWithNeutral() {
        StringBuilder sb = new StringBuilder(keyLength);
        for (KeySlotState s : slots)
            sb.append(s.hasKeyLetter() ? s.getKeyLetter() : NEUTRAL_KEY);
        return sb.toString();
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= keyLength)
            throw new IllegalArgumentException("slot out of range: " + slot);
    }

}
