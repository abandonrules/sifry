package cz.absolutno.sifry.common.dictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DictionaryValidationResult {

    private final String normalizedWord;
    private final boolean match;
    private final List<String> matchedSourceFileNames;

    public DictionaryValidationResult(String normalizedWord, boolean match, List<String> matchedSourceFileNames) {
        this.normalizedWord = normalizedWord;
        this.match = match;
        List<String> copy = new ArrayList<String>();
        if (matchedSourceFileNames != null)
            copy.addAll(matchedSourceFileNames);
        this.matchedSourceFileNames = Collections.unmodifiableList(copy);
    }

    public static DictionaryValidationResult noMatch(String normalizedWord) {
        return new DictionaryValidationResult(normalizedWord, false, Collections.<String>emptyList());
    }

    public boolean isMatch() {
        return match;
    }

    public String getNormalizedWord() {
        return normalizedWord;
    }

    public List<String> getMatchedSourceFileNames() {
        return matchedSourceFileNames;
    }

    public DictionaryMatchState getState() {
        return match ? DictionaryMatchState.MATCH : DictionaryMatchState.NO_MATCH;
    }

}