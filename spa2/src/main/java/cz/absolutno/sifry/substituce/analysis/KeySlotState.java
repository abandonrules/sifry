package cz.absolutno.sifry.substituce.analysis;

/**
 * One key slot (column) of a Vigenère table with its current state: the slot
 * index, the key letter currently assigned (nullable), and whether the slot is
 * "locked" to a hypothesis. Immutable; matches against a plaintext range via a
 * VigenereEngine derive call. Pure JVM value.
 */
public final class KeySlotState {

    private final int slot;
    private final Character keyLetter; // null = not yet assigned
    private final boolean locked;

    public KeySlotState(int slot, Character keyLetter, boolean locked) {
        if (slot < 0)
            throw new IllegalArgumentException("slot must be >= 0");
        this.slot = slot;
        this.keyLetter = keyLetter;
        this.locked = locked;
    }

    public KeySlotState(int slot) {
        this(slot, null, false);
    }

    public int getSlot() {
        return slot;
    }

    public Character getKeyLetter() {
        return keyLetter;
    }

    public boolean hasKeyLetter() {
        return keyLetter != null;
    }

    public boolean isLocked() {
        return locked;
    }

    public KeySlotState withKeyLetter(Character key) {
        return new KeySlotState(slot, key, locked);
    }

    public KeySlotState lockedCopy() {
        return new KeySlotState(slot, keyLetter, true);
    }

    public KeySlotState unlockedCopy() {
        return new KeySlotState(slot, keyLetter, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof KeySlotState))
            return false;
        KeySlotState that = (KeySlotState) o;
        return slot == that.slot && locked == that.locked
                && java.util.Objects.equals(keyLetter, that.keyLetter);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * slot + Boolean.hashCode(locked))
                + java.util.Objects.hashCode(keyLetter);
    }

    @Override
    public String toString() {
        return "KeySlotState[" + slot + ":" + (keyLetter == null ? "?" : keyLetter) + (locked ? " L" : "") + "]";
    }
}
