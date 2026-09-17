package cz.absolutno.sifry.substituce.analysis;

/**
 * The Vigenère letter-combination conventions the analysis screen supports.
 * <p>
 * Sifry's existing Vigenère handling is authoritative (see {@code HesloAdapter});
 * this enum mirrors its A(n) ± B(n) forms with 0- or 1-based alphabet numbering.
 * The keyword restarts at each {@code keyLength} boundary (denoted "B" in the app:
 * after the last key letter is used, processing starts over with the first one).
 * The screen never silently picks a convention: the solver selects one explicitly.
 * <p>
 * Pure JVM value; no Android dependencies.
 */
public enum VigenereConvention {

    /** Plaintext = Cipher + Key, letters numbered 0-based (A = 0). */
    APLUSB0(true, false),

    /** Plaintext = Cipher - Key, letters numbered 0-based (A = 0). */
    AMINUSB0(false, false),

    /** Plaintext = Cipher + Key, letters numbered 1-based (A = 1). */
    APLUSB1(true, true),

    /** Plaintext = Cipher - Key, letters numbered 1-based (A = 1). */
    AMINUSB1(false, true);

    private static final int MOD = 26;

    private final boolean plus;
    private final boolean oneBased;

    VigenereConvention(boolean plus, boolean oneBased) {
        this.plus = plus;
        this.oneBased = oneBased;
    }

    public boolean isPlus() {
        return plus;
    }

    public boolean isOneBased() {
        return oneBased;
    }

    /**
     * Produces the plaintext letter ordinal (0-based) for a ciphertext letter ordinal
     * combined with a key letter ordinal under this convention.
     *
     * @param cipherOrd  ciphertext letter ordinal, 0..25
     * @param keyOrd     key letter ordinal, 0..25
     * @return plaintext letter ordinal, 0..25
     */
    public int combine(int cipherOrd, int keyOrd) {
        int shift;
        if (plus)
            shift = cipherOrd + keyOrd;
        else
            shift = cipherOrd - keyOrd;
        if (oneBased)
            shift += plus ? 1 : -1;
        int c = shift % MOD;
        if (c < 0)
            c += MOD;
        return c;
    }

    /**
     * The key letter ordinal (0-based) that, under this convention, turns the given
     * ciphertext letter ordinal into the given plaintext letter ordinal.
     */
    public int requiredKey(int cipherOrd, int plainOrd) {
        int shift;
        if (plus)
            shift = plainOrd - cipherOrd;
        else
            shift = cipherOrd - plainOrd;
        if (oneBased)
            shift -= 1;
        int b = shift % MOD;
        if (b < 0)
            b += MOD;
        return b;
    }

}