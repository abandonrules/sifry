package cz.absolutno.sifry.common.dictionary;

public final class DictionaryValidationOptions {

    private final String dictionaryFileName;
    private final boolean normalizeLowercase;

    public DictionaryValidationOptions(String dictionaryFileName, boolean normalizeLowercase) {
        this.dictionaryFileName = dictionaryFileName;
        this.normalizeLowercase = normalizeLowercase;
    }

    public static DictionaryValidationOptions defaultOptions() {
        return new DictionaryValidationOptions(DictionarySource.ENGLISH.getFileName(), true);
    }

    public String getDictionaryFileName() {
        return dictionaryFileName;
    }

    public boolean isNormalizeLowercase() {
        return normalizeLowercase;
    }

}