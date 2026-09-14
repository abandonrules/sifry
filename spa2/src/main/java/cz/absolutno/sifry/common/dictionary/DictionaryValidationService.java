package cz.absolutno.sifry.common.dictionary;

/**
 * Whether a token exists as an exact word in a chosen dictionary. The concrete
 * implementation decides per source in which way the token is compared.
 */
public interface DictionaryValidationService {

    DictionaryValidationResult validateWord(String word, DictionaryValidationOptions options);

}