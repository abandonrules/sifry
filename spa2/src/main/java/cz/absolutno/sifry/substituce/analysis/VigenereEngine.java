package cz.absolutno.sifry.substituce.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic Vigenère align/derive computation. Pure JVM; no Android dependencies.
 * <p>
 * The engine maps each normalized ciphertext letter to exactly one key slot by
 * {@code (letterIndex mod keyLength)}. Spaces and punctuation never consume a key
 * position: they are skipped for alignment and copied into the plaintext unchanged.
 * <p>
 * All input text is folded to uppercase A-Z; any other character is a passthrough.
 */
public final class VigenereEngine {

    private VigenereEngine() {
    }

    /** Per-letter slot assignment for the ciphertext (parallel to the input string). */
    public static final class Alignment {
        private final int[] slots;

        Alignment(int[] slots) {
            this.slots = slots;
        }

        public int length() {
            return slots.length;
        }

        /** Slot index for the given ciphertext position, or -1 when it is not a letter. */
        public int slotAt(int index) {
            return slots[index];
        }
    }

    /**
     * Computes the key slot for every ciphertext position. Letters receive
     * {@code (runningLetterIndex mod keyLength)}; non-letters receive -1.
     *
     * @throws IllegalArgumentException when keyLength <= 0
     */
    public static Alignment align(String ciphertext, int keyLength) {
        if (keyLength <= 0)
            throw new IllegalArgumentException("keyLength must be > 0");
        int[] slots = new int[ciphertext.length()];
        int letter = 0;
        for (int i = 0; i < ciphertext.length(); i++) {
            if (isLetter(ciphertext.charAt(i)))
                slots[i] = letter++ % keyLength;
            else
                slots[i] = -1;
        }
        return new Alignment(slots);
    }

    /**
     * Derives the full plaintext for the ciphertext using the given convention and the
     * key letters, one per key slot (row 0..keyLength-1).
     * Non-letters are copied verbatim (uppercase where applicable) and do not consume key.
     *
     * @param key a String of length keyLength; each char is the key letter for a slot
     */
    public static String derivePlain(String ciphertext, VigenereConvention convention, String key) {
        return derivePlainInternal(ciphertext, convention, key, -1);
    }

    /**
     * Derives plaintext for all ciphertext positions, but only positions governed by
     * {@code onlySlot} are recomputed; positions governed by any other slot keep their
     * existing value from {@code currentPlain}. Used for the per-column live update.
     * Non-letters always pass through unchanged.
     *
     * @param key          a String of length keyLength
     * @param onlySlot     slot to recompute, or -1 to recompute all slots
     * @param currentPlain current full plaintext used for unaffected positions
     */
    public static String derivePlainColumn(String ciphertext, VigenereConvention convention,
                                           String key, int onlySlot, String currentPlain) {
        StringBuilder dst = new StringBuilder();
        Alignment alignment = align(ciphertext, key.length());
        for (int i = 0; i < ciphertext.length(); i++) {
            char c = ciphertext.charAt(i);
            if (!isLetter(c)) {
                dst.append(toUpper(c));
                continue;
            }
            int slot = alignment.slotAt(i);
            if (onlySlot >= 0 && slot != onlySlot) {
                dst.append(currentPlain.charAt(i));
                continue;
            }
            int ord = ord(c);
            int plainOrd = convention.combine(ord, ord(key.charAt(slot)));
            dst.append((char) ('A' + plainOrd));
        }
        return dst.toString();
    }

    private static String derivePlainInternal(String ciphertext, VigenereConvention convention,
                                              String key, int onlySlot) {
        StringBuilder dst = new StringBuilder();
        Alignment alignment = align(ciphertext, key.length());
        for (int i = 0; i < ciphertext.length(); i++) {
            char c = ciphertext.charAt(i);
            int slot = alignment.slotAt(i);
            if (slot < 0) {
                dst.append(toUpper(c));
                continue;
            }
            if (onlySlot >= 0 && slot != onlySlot)
                throw new IllegalStateException("unexpected partial derivation");
            int plainOrd = convention.combine(ord(c), ord(key.charAt(slot)));
            dst.append((char) ('A' + plainOrd));
        }
        return dst.toString();
    }

    /**
     * Derives the key letters required to turn the given ciphertext into the given
     * plaintext under the convention. Returns a map of slot index -&gt; required key letter,
     * with slot {@code index mod keyLength} over the letter positions (spaces and
     * punctuation do not consume key slots, matching {@link #align}).
     * <p>
     * The two strings must have the same length; non-letter positions must coincide.
     * Positions where both are letters are compared for consistency: if the same slot
     * already requires a different letter, the conflict is reported in {@code conflicts}
     * rather than silently overwriting.
     *
     * @param keyLength    the candidate key length that groups positions into slots
     * @return a DerivedKey with a stable (ordered) slot-to-letter map and a conflict list
     * @throws IllegalArgumentException when the lengths differ or keyLength &lt;= 0
     */
    public static DerivedKey deriveRequiredKey(String ciphertext, String plaintext,
                                               VigenereConvention convention, int keyLength) {
        if (ciphertext.length() != plaintext.length())
            throw new IllegalArgumentException("ciphertext and plaintext lengths differ");
        if (keyLength <= 0)
            throw new IllegalArgumentException("keyLength must be > 0");
        LinkedHashMap<Integer, Character> required = new LinkedHashMap<Integer, Character>();
        List<Integer> conflicts = new ArrayList<Integer>();
        Alignment alignment = align(ciphertext, keyLength);
        for (int i = 0; i < ciphertext.length(); i++) {
            char cc = ciphertext.charAt(i);
            char pp = plaintext.charAt(i);
            if (!isLetter(cc)) {
                if (isLetter(pp))
                    throw new IllegalArgumentException("non-letter ciphertext vs letter plaintext at " + i);
                continue;
            }
            if (!isLetter(pp))
                continue; // plaintext non-letter: no key derived at this position
            int slot = alignment.slotAt(i);
            int keyOrd = convention.requiredKey(ord(cc), ord(pp));
            char keyChar = (char) ('A' + keyOrd);
            Character prev = required.get(slot);
            if (prev == null)
                required.put(slot, keyChar);
            else if (!prev.equals(keyChar) && !conflicts.contains(slot))
                conflicts.add(slot);
        }
        return new DerivedKey(required, conflicts);
    }

    /** Result of {@link #deriveRequiredKey}. */
    public static final class DerivedKey {
        private final Map<Integer, Character> required;
        private final List<Integer> conflicts;

        DerivedKey(Map<Integer, Character> required, List<Integer> conflicts) {
            this.required = Collections.unmodifiableMap(required);
            this.conflicts = Collections.unmodifiableList(conflicts);
        }

        /** Slot index -&gt; required key letter. Ordered by first slot appearance. */
        public Map<Integer, Character> getRequired() {
            return required;
        }

        /** Slots whose existing required letter conflicts with another required letter. */
        public List<Integer> getConflicts() {
            return conflicts;
        }

        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }
    }

    private static boolean isLetter(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static int ord(char c) {
        return c - 'A';
    }

    private static char toUpper(char c) {
        if (c >= 'a' && c <= 'z')
            return (char) (c - ('a' - 'A'));
        return c;
    }
}