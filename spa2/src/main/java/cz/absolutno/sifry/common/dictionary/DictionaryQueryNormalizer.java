package cz.absolutno.sifry.common.dictionary;

import java.util.Locale;

/**
 * Case normalization for dictionary queries, applied at the shared search
 * boundary.
 *
 * <p>Cipher tools naturally produce uppercase text (and cipher workflows may
 * present it visually spaced, {@code N F O R M A T}) while the dictionary canon
 * is lowercase. Every caller of the shared Dictionary/Search service must get
 * identical behavior, so no UI screen calls {@code toLowerCase()} itself:
 * normalization happens here, once, before a pattern is built.
 *
 * <p>Canonical form is {@link Locale#ROOT} lowercase with presentation-only
 * whitespace removed. Callers that need source-aware behavior (for example a
 * pack that genuinely declares case-sensitive or space-sensitive semantics)
 * bypass this class.
 */
public final class DictionaryQueryNormalizer {

    private DictionaryQueryNormalizer() {
    }

    /**
     * Canonical contiguous form of a query fragment: {@link Locale#ROOT}
     * lowercase with all whitespace removed. {@code NFORMAT}, {@code nFoTmAt}
     * and {@code N F O R M A T} all yield {@code nformat}.
     */
    public static String canonicalFragment(String raw) {
        if (raw == null)
            return "";
        String lower = raw.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (!Character.isWhitespace(c))
                sb.append(c);
        }
        return sb.toString();
    }

    /** Canonical single word: {@link #canonicalFragment} trimmed at the edges. */
    public static String canonicalWord(String raw) {
        if (raw == null)
            return "";
        return canonicalFragment(raw.trim());
    }
}