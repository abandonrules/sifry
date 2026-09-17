package cz.absolutno.sifry.substituce.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable aggregate of one Vigenère analysis session: the normalized
 * ciphertext, the convention, the current key length, the per-slot key
 * states, and the ranked Kasiski period candidates. Pure JVM value; later
 * phases wire it into a Bundle. On key-length change the key letters and
 * lock flags of slots that still exist are kept; slots beyond the new
 * length are dropped.
 */
public final class VigenereAnalysisState {

    private final String ciphertext;
    private final VigenereConvention convention;
    private final int keyLength;
    private final List<KeySlotState> keySlots; // size == keyLength
    private final List<KasiskiAnalyzer.PeriodCandidate> periodCandidates;

    public VigenereAnalysisState(String ciphertext,
                                 VigenereConvention convention,
                                 int keyLength,
                                 List<KeySlotState> keySlots,
                                 List<KasiskiAnalyzer.PeriodCandidate> periodCandidates) {
        if (ciphertext == null)
            throw new IllegalArgumentException("ciphertext is required");
        if (convention == null)
            throw new IllegalArgumentException("convention is required");
        if (keyLength < 1)
            throw new IllegalArgumentException("keyLength must be >= 1");
        if (keySlots == null || keySlots.size() != keyLength)
            throw new IllegalArgumentException("keySlots must have exactly keyLength slots");
        this.ciphertext = ciphertext;
        this.convention = convention;
        this.keyLength = keyLength;
        this.keySlots = Collections.unmodifiableList(new ArrayList<>(keySlots));
        this.periodCandidates = periodCandidates == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(periodCandidates));
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

    public List<KeySlotState> getKeySlots() {
        return keySlots;
    }

    public KeySlotState getKeySlot(int slot) {
        return keySlots.get(slot);
    }

    public List<KasiskiAnalyzer.PeriodCandidate> getPeriodCandidates() {
        return periodCandidates;
    }

    /** Immutable copy with a different convention; key letters are ordinals and
     *  convention-independent, so they carry over unchanged. */
    public VigenereAnalysisState withConvention(VigenereConvention c) {
        return new VigenereAnalysisState(ciphertext, c, keyLength, keySlots, periodCandidates);
    }

    /** Immutable copy with a new key length; key letters and lock flags are
     *  kept for slots below the new length and dropped for slots at or above
     *  it. */
    public VigenereAnalysisState withKeyLength(int len) {
        if (len < 1)
            throw new IllegalArgumentException("keyLength must be >= 1");
        List<KeySlotState> reSlotted = new ArrayList<>(len);
        for (int s = 0; s < len; s++)
            reSlotted.add(new KeySlotState(s));
        for (KeySlotState ks : keySlots) {
            if (ks.hasKeyLetter() && ks.getSlot() < len) {
                KeySlotState prev = reSlotted.get(ks.getSlot());
                KeySlotState updated = prev.withKeyLetter(ks.getKeyLetter());
                if (ks.isLocked())
                    updated = updated.lockedCopy();
                reSlotted.set(ks.getSlot(), updated);
            }
        }
        return new VigenereAnalysisState(ciphertext, convention, len, reSlotted, periodCandidates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof VigenereAnalysisState))
            return false;
        VigenereAnalysisState that = (VigenereAnalysisState) o;
        return keyLength == that.keyLength
                && ciphertext.equals(that.ciphertext)
                && convention == that.convention
                && keySlots.equals(that.keySlots)
                && periodCandidates.equals(that.periodCandidates);
    }

    @Override
    public int hashCode() {
        int r = ciphertext.hashCode();
        r = 31 * r + convention.hashCode();
        r = 31 * r + keyLength;
        r = 31 * r + keySlots.hashCode();
        r = 31 * r + periodCandidates.hashCode();
        return r;
    }

    @Override
    public String toString() {
        return "VigenereAnalysisState[conv=" + convention
                + ",keyLen=" + keyLength
                + ",slots=" + keySlots
                + ",periods=" + periodCandidates + "]";
    }
}
