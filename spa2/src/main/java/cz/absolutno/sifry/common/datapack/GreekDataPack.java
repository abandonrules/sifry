package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The 24-letter Greek alphabet. Lookup by symbol (upper or lower, including the
 * final-sigma ς alias), by name, or by ordinal position. Exposes name, ordinal,
 * uppercase/lowercase (plus alternate lowercase forms), and transliterations.
 */
public final class GreekDataPack extends BaseDataPack {

    public static final String ID = "greek";

    private final Map<String, String> symbolToName;
    private final Map<Integer, PackRecord> ordinalToRecord;
    private final Map<String, String> nameKeyToSymbol;

    public GreekDataPack(PackManifest manifest, List<PackRecord> records) {
        super(manifest, records, Arrays.asList(
                new FieldDefinition("ordinal", "Position", FieldDefinition.Kind.NUMERIC),
                new FieldDefinition("uppercase", "Uppercase", FieldDefinition.Kind.TEXT),
                new FieldDefinition("lowercase", "Lowercase", FieldDefinition.Kind.TEXT),
                new FieldDefinition("transliterations", "Transliteration", FieldDefinition.Kind.TEXT)));
        this.symbolToName = new HashMap<String, String>();
        this.ordinalToRecord = new HashMap<Integer, PackRecord>();
        this.nameKeyToSymbol = new HashMap<String, String>();

        for (PackRecord r : records) {
            if (r.property("ordinal") != null)
                ordinalToRecord.put(ordinal(r), r);
            nameKeyToSymbol.put(normalizeName(r.displayName()), r.property("lowercase"));
            putSymbol(symbolToName, r.property("uppercase"), r.displayName());
            putSymbol(symbolToName, r.property("lowercase"), r.displayName());
            for (String alt : alternateLowerCase(r))
                putSymbol(symbolToName, alt, r.displayName());
            for (String a : r.aliases())
                nameKeyToSymbol.put(normalizeName(a), r.property("lowercase"));
        }
    }

    private static void putSymbol(Map<String, String> m, String symbol, String name) {
        if (symbol != null && !symbol.isEmpty())
            put(m, symbol, name);
    }

    private static void put(Map<String, String> m, String k, String v) {
        String key = k.trim();
        if (!key.isEmpty())
            m.put(key, v);
    }

    private static List<String> alternateLowerCase(PackRecord r) {
        List<String> out = new ArrayList<String>();
        String alts = r.property("alternateLowercase");
        if (alts != null)
            for (String s : alts.split(","))
                if (!s.trim().isEmpty())
                    out.add(s.trim());
        return out;
    }

    private static int ordinal(PackRecord r) {
        return Integer.parseInt(r.property("ordinal").trim());
    }

    /** Returns the pinching name key used for name lookups. */
    static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
    }

    /** Symbol (e.g. {@code π}, {@code Π}, {@code ς}) -> letter record. */
    public EntityResult bySymbol(String symbol) {
        if (symbol == null)
            return null;
        String name = symbolToName.get(symbol.trim());
        return name != null ? findByName(name) : null;
    }

    /** Name (e.g. {@code Pi}, {@code pi}) -> letter record. */
    public EntityResult byName(String name) {
        if (name == null)
            return null;
        String canonical = nameKeyToSymbol.get(normalizeName(name));
        if (canonical == null)
            return null;
        return bySymbol(canonical);
    }

    /** Ordinal (1-24) -> letter record, e.g. 16 -> Pi. */
    public EntityResult byOrdinal(int ordinal) {
        return toResultIfPresent(ordinalToRecord.get(ordinal));
    }

    @Override
    public SearchResult search(FilterSpec spec) {
        List<EntityResult> hits = new ArrayList<EntityResult>();
        if (spec.isIdentity()) {
            String q = spec.value() == null ? "" : spec.value().trim();
            if (q.isEmpty())
                return listAll();
            EntityResult bySymbol = bySymbol(q);
            if (bySymbol != null) {
                hits.add(bySymbol);
                return wrap(hits, records().size());
            }
            EntityResult byName = byName(q);
            if (byName != null) {
                hits.add(byName);
                return wrap(hits, records().size());
            }
            Integer ord = parseInt(q);
            if (ord != null) {
                EntityResult byOrd = byOrdinal(ord);
                if (byOrd != null) {
                    hits.add(byOrd);
                    return wrap(hits, records().size());
                }
            }
            // transliteration match
            for (PackRecord r : records()) {
                String tl = r.property("transliterations");
                if (tl == null)
                    continue;
                for (String t : tl.split(","))
                    if (t.trim().equalsIgnoreCase(q)) {
                        hits.add(toResult(r));
                        return wrap(hits, records().size());
                    }
            }
            return wrap(hits, records().size());
        }
        return wrap(matchField(spec.field(), spec), records().size());
    }

    private EntityResult findByName(String name) {
        for (PackRecord r : records())
            if (r.displayName().equalsIgnoreCase(name))
                return toResult(r);
        return null;
    }

    private EntityResult toResultIfPresent(PackRecord r) {
        return r == null ? null : toResult(r);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Greek Letters";
    }
}