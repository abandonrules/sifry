package cz.absolutno.sifry.regexp;

import android.content.SharedPreferences;

/**
 * Registry of the searchable dictionary (canon) data sources bundled with the
 * app. Each source has a bundled asset {@code raw/<file>} and a matching
 * setting key whose checkbox turns it on/off. Unknown filenames are never
 * considered enabled.
 */
public final class DataSources {

    public static final String[] FILE_NAMES = {
            "cs.canon", "en.canon", "periodic.canon", "pokemon.canon", "wordle.canon"
    };

    public static final String[] PREF_KEYS = {
            "pref_source_cs", "pref_source_en", "pref_source_periodic",
            "pref_source_pokemon", "pref_source_wordle"
    };

    private DataSources() {
    }

    public static boolean isEnabled(SharedPreferences sp, String filename) {
        if (sp == null)
            return true;
        for (int i = 0; i < FILE_NAMES.length; i++)
            if (FILE_NAMES[i].equals(filename))
                return sp.getBoolean(PREF_KEYS[i], true);
        return false;
    }

    /** First source enabled by the user, in the fixed order above, or null. */
    public static String firstEnabled(SharedPreferences sp) {
        for (String file : FILE_NAMES)
            if (isEnabled(sp, file))
                return file;
        return null;
    }

    /**
     * Dictionary actually searched: keeps {@code selected} when its source is
     * enabled, otherwise falls back to the first enabled source, and to
     * {@code selected} again when nothing is enabled at all.
     */
    public static String resolve(SharedPreferences sp, String selected, String fallback) {
        if (selected == null || selected.isEmpty())
            selected = fallback;
        if (isEnabled(sp, selected))
            return selected;
        String first = firstEnabled(sp);
        return first != null ? first : selected;
    }

}