package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared deterministic matching for packs whose records match on identity
 * (display name / aliases / extra identity tokens such as symbols) and on
 * numeric or textual properties. Concrete packs supply their record list and
 * identity token set.
 */
public abstract class BaseDataPack implements DataPack {

    private final PackManifest manifest;
    private final List<PackRecord> records;
    private final List<FieldDefinition> fields;

    protected BaseDataPack(PackManifest manifest, List<PackRecord> records,
                           List<FieldDefinition> fields) {
        this.manifest = manifest;
        this.records = new ArrayList<PackRecord>(records);
        this.fields = new ArrayList<FieldDefinition>(fields);
    }

    @Override
    public PackManifest manifest() {
        return manifest;
    }

    @Override
    public List<FieldDefinition> fields() {
        return fields;
    }

    protected List<PackRecord> records() {
        return records;
    }

    /**
     * Extra identity tokens for a record beyond its display name: symbols
     * (♃, π, σ) and any aliases. Matching is case-insensitive.
     */
    protected Iterable<String> identityTokens(PackRecord r) {
        List<String> tokens = new ArrayList<String>();
        tokens.add(r.displayName());
        tokens.addAll(r.aliases());
        String symbol = r.property("symbol");
        if (symbol != null && !symbol.isEmpty())
            tokens.add(symbol);
        String letter = r.property("letter");
        if (letter != null && !letter.isEmpty())
            tokens.add(letter);
        String numeral = r.property("numeral");
        if (numeral != null && !numeral.isEmpty())
            tokens.add(numeral);
        return tokens;
    }

    /**
     * Identity scan: every record whose display name, alias or symbol equals
     * the query (case-insensitive, trimmed). Returns a matched/result pair.
     */
    protected List<EntityResult> matchIdentity(String query) {
        List<EntityResult> out = new ArrayList<EntityResult>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty())
            return out;
        for (PackRecord r : records) {
            for (String token : identityTokens(r)) {
                if (token.toLowerCase(Locale.ROOT).equals(q)) {
                    out.add(toResult(r));
                    break;
                }
            }
        }
        return out;
    }

    protected EntityResult toResult(PackRecord r) {
        return new EntityResult(r.id(), r.displayName(), r.category(), r.properties());
    }

    /** Numeric property filter over a single integer-valued property. */
    protected List<EntityResult> matchNumericField(String field, FilterSpec spec) {
        List<EntityResult> out = new ArrayList<EntityResult>();
        Integer v = parseInt(spec.value());
        Integer v2 = parseInt(spec.value2());
        for (PackRecord r : records) {
            Integer p = parseInt(r.property(field));
            if (p == null)
                continue;
            if (compare(p, spec.op(), v, v2))
                out.add(toResult(r));
        }
        return out;
    }

    /** Text property filter (equals or contains, case-insensitive). */
    protected List<EntityResult> matchTextField(String field, FilterSpec spec) {
        List<EntityResult> out = new ArrayList<EntityResult>();
        String v = spec.value() == null ? "" : spec.value().trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty())
            return out;
        for (PackRecord r : records) {
            String p = r.property(field);
            if (p == null)
                continue;
            String pv = p.toLowerCase(Locale.ROOT);
            boolean hit = spec.op() == FilterSpec.Op.CONTAINS ? pv.contains(v) : pv.equals(v);
            if (hit)
                out.add(toResult(r));
        }
        return out;
    }

    /** Dispatches a property filter by the field's declared kind. */
    protected List<EntityResult> matchField(String field, FilterSpec spec) {
        FieldDefinition.Kind kind = kindOf(field);
        if (kind == FieldDefinition.Kind.NUMERIC)
            return matchNumericField(field, spec);
        return matchTextField(field, spec);
    }

    private FieldDefinition.Kind kindOf(String field) {
        for (FieldDefinition fd : fields)
            if (fd.id().equals(field))
                return fd.kind();
        return FieldDefinition.Kind.TEXT;
    }

    private static boolean compare(int p, FilterSpec.Op op, Integer v, Integer v2) {
        if (v == null)
            return false;
        switch (op) {
            case EQ: return p == v;
            case LT: return p < v;
            case GT: return p > v;
            case LE: return p <= v;
            case GE: return p >= v;
            case BETWEEN: return v2 != null && p >= v && p <= v2;
            case CONTAINS: return p == v;
            default: return false;
        }
    }
    
    static Integer parseInt(String s) {
        if (s == null)
            return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected SearchResult wrap(List<EntityResult> hits, int scanned) {
        return new SearchResult(hits, hits.size(), scanned);
    }
}