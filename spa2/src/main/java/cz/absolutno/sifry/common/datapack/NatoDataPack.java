package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * NATO/ICAO spelling alphabet. Canonical word per Latin letter (A-Z) plus
 * separate spoken digits (0-9). Alias normalization folds punctuation/space
 * variants (X-ray / Xray / X Ray) into the canonical record. Lookups are
 * bidirectional; whole strings spell out letter by letter (digits map to
 * their spoken table), and the reverse — spelling words back to letters — is
 * the inverse mapping on the same records.
 */
public final class NatoDataPack extends BaseDataPack {

    public static final String ID = "nato";

    private final Map<String, String> wordToLetter;
    private final Map<String, String> letterToWord;
    private final Map<String, String> digitWordToDigit;
    private final Map<String, String> digitToDigitWord;
    private final Map<String, PackRecord> byId;

    public NatoDataPack(PackManifest manifest, List<PackRecord> records) {
        super(manifest, records, Arrays.asList(
                new FieldDefinition("word", "Spelling word", FieldDefinition.Kind.TEXT),
                new FieldDefinition("property", "Spoken digit word", FieldDefinition.Kind.TEXT)));
        this.wordToLetter = new HashMap<String, String>();
        this.letterToWord = new HashMap<String, String>();
        this.digitWordToDigit = new HashMap<String, String>();
        this.digitToDigitWord = new HashMap<String, String>();
        this.byId = new HashMap<String, PackRecord>();

        for (PackRecord r : records) {
            byId.put(r.id(), r);
            String category = r.category();
            if ("digit".equals(category)) {
                String digit = r.id();
                String canonical = r.displayName();
                digitToDigitWord.put(digit, canonical);
                for (String t : spokenTokens(r))
                    digitWordToDigit.put(t, digit);
                continue;
            }
            String letter = r.id();
            letterToWord.put(letter, r.displayName());
            for (String token : wordTokensForLetter(r))
                wordToLetter.put(token, letter);
        }
    }

    /** Normalizes a spelling word for alias matching: lower, drop spaces and dashes. */
    public static String normalize(String s) {
        if (s == null)
            return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[ -]", "");
    }

    private static Iterable<String> wordTokensForLetter(PackRecord r) {
        List<String> tokens = new ArrayList<String>();
        String letter = r.id().toLowerCase(Locale.ROOT);
        tokens.add(normalize(r.displayName()));
        for (String a : r.aliases())
            tokens.add(normalize(a));
        tokens.add(letter);
        return tokens;
    }

    private static List<String> spokenTokens(PackRecord r) {
        List<String> tokens = new ArrayList<String>();
        tokens.add(normalize(r.displayName()));
        for (String a : r.aliases())
            tokens.add(normalize(a));
        return tokens;
    }

    /** Letter {@code F} -> {@code Foxtrot}. Input may be upper/lower case. */
    public EntityResult letterSpelling(String letter) {
        if (letter == null)
            return null;
        String l = letter.trim();
        if (l.length() != 1)
            return null;
        String up = l.toUpperCase(Locale.ROOT);
        String word = letterToWord.get(up);
        if (word == null)
            return null;
        return findRecord(word);
    }

    /** Word (canonical or alias, punctuation-insensitive) -> letter. */
    public EntityResult wordLetter(String word) {
        if (word == null)
            return null;
        String letter = wordToLetter.get(normalize(word));
        if (letter == null)
            return null;
        return findRecordById(letter);
    }

    /** Spoken digit {@code 4} -> {@code Four} (digits table). */
    public EntityResult digitSpelling(String digit) {
        if (digit == null)
            return null;
        String d = digit.trim();
        if (d.length() != 1 || !Character.isDigit(d.charAt(0)))
            return null;
        String word = digitToDigitWord.get(d);
        if (word == null)
            return null;
        return findCategory("digit", word);
    }

    /** {@code SIFRY} -> {@code Sierra India Foxtrot Romeo Yankee}. */
    public String spell(String text) {
        if (text == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String w;
            if (Character.isLetter(c))
                w = letterToWord.get(String.valueOf(Character.toUpperCase(c)));
            else if (Character.isDigit(c))
                w = digitToDigitWord.get(String.valueOf(c));
            else
                w = null;
            if (w == null)
                continue;
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(w);
        }
        return sb.toString();
    }

    /** Reverse: {@code Sierra India Foxrot...} -> {@code SIFRY}. */
    public String unspell(String words) {
        if (words == null)
            return "";
        StringBuilder sb = new StringBuilder();
        String[] parts = words.trim().split("\\s+");
        for (String p : parts) {
            String letter = wordToLetter.get(normalize(p));
            if (letter != null)
                sb.append(letter);
        }
        return sb.toString();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "NATO Phonetic Alphabet";
    }

    @Override
    public SearchResult search(FilterSpec spec) {
        List<EntityResult> hits = new ArrayList<EntityResult>();
        if (spec.isIdentity()) {
            String q = spec.value() == null ? "" : spec.value().trim();
            if (q.isEmpty())
                return wrap(hits, records().size());
            if (q.length() == 1 && Character.isDigit(q.charAt(0)))
                hits = single(digitSpelling(q));
            else if (q.length() == 1 && Character.isLetter(q.charAt(0)))
                hits = single(letterSpelling(q));
            else {
                String single = wordToLetter.get(normalize(q));
                hits = single != null ? single(letterSpelling(single))
                        : single(pathRecord(q));
            }
            return wrap(hits, records().size());
        }
        return wrap(matchTextField(spec.field(), spec), records().size());
    }

    private List<EntityResult> single(EntityResult r) {
        List<EntityResult> out = new ArrayList<EntityResult>();
        if (r != null)
            out.add(r);
        return out;
    }

    private EntityResult findRecordById(String id) {
        PackRecord r = byId.get(id);
        return r == null ? null : toResult(r);
    }

    private EntityResult findRecord(String displayName) {
        for (PackRecord r : records())
            if (r.displayName().equals(displayName))
                return toResult(r);
        return null;
    }

    private EntityResult findCategory(String category, String displayName) {
        for (PackRecord r : records())
            if (category.equals(r.category()) && r.displayName().equals(displayName))
                return toResult(r);
        return null;
    }

    /** Longer non-letter-input path: try a full spelling record by name first. */
    private EntityResult pathRecord(String q) {
        for (PackRecord r : records())
            if (r.displayName().equalsIgnoreCase(q.trim()))
                return toResult(r);
        return null;
    }
}