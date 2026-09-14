package cz.absolutno.sifry.common.dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One bundled canon dictionary and its UI toggle key. The searchable token is
 * the lowercase key before the first ':' on each canon line; WORD sources keep
 * ASCII letters only, PERIODIC and POKEMON keys carry extra metadata in the value.
 */
public final class DictionarySource {

    public enum Kind { WORD, PERIODIC, POKEMON }

    private final String fileName;
    private final Kind kind;

    private DictionarySource(String fileName, Kind kind) {
        this.fileName = fileName;
        this.kind = kind;
    }

    public static final DictionarySource CZECH = new DictionarySource("cs.canon", Kind.WORD);
    public static final DictionarySource ENGLISH = new DictionarySource("en.canon", Kind.WORD);
    public static final DictionarySource PERIODIC = new DictionarySource("periodic.canon", Kind.PERIODIC);
    public static final DictionarySource POKEMON = new DictionarySource("pokemon.canon", Kind.POKEMON);
    public static final DictionarySource WORDLE = new DictionarySource("wordle.canon", Kind.WORD);

    private static final List<DictionarySource> ALL = unmodifiable(
            CZECH, ENGLISH, PERIODIC, POKEMON, WORDLE);

    private static List<DictionarySource> unmodifiable(DictionarySource... sources) {
        List<DictionarySource> out = new ArrayList<DictionarySource>();
        for (DictionarySource s : sources)
            out.add(s);
        return Collections.unmodifiableList(out);
    }

    public static List<DictionarySource> all() {
        return ALL;
    }

    public static DictionarySource byFileName(String fileName) {
        for (DictionarySource s : ALL)
            if (s.fileName.equals(fileName))
                return s;
        return null;
    }

    public static List<DictionarySource> byFileNames(List<String> fileNames) {
        List<DictionarySource> out = new ArrayList<DictionarySource>();
        for (String f : fileNames) {
            DictionarySource s = byFileName(f);
            if (s != null)
                out.add(s);
        }
        return out;
    }

    public String getFileName() {
        return fileName;
    }

    public String getAssetPath() {
        return "raw/" + fileName;
    }

    public String getPreferenceKey() {
        return "pref_source_" + fileName.substring(0, fileName.indexOf('.'));
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isWordSource() {
        return kind == Kind.WORD;
    }

}