package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Solar-system body and constellation lookups. Exposes identity (name, symbol,
 * alias), numeric fields (orderFromSun, knownMoonCount, diameterKm, ...), text
 * fields (planetType, parentBody — the reverse lookup from moon to planet), and
 * time-aware moon counts: each planet record carries the count that was valid
 * for its stated window, gated by the caller's reference date via
 * {@link #validAt(String)}.
 */
public final class AstronomyDataPack extends BaseDataPack {

    public static final String ID = "astronomy";

    private final Map<String, String> symbolToName;
    private final List<PackRecord> moons;

    public AstronomyDataPack(PackManifest manifest, List<PackRecord> records) {
        super(manifest, records, Arrays.asList(
                new FieldDefinition("orderFromSun", "Distance from Sun", FieldDefinition.Kind.NUMERIC),
                new FieldDefinition("knownMoonCount", "Known moons", FieldDefinition.Kind.NUMERIC),
                new FieldDefinition("diameterKm", "Diameter (km)", FieldDefinition.Kind.NUMERIC),
                new FieldDefinition("orbitalPeriodDays", "Orbital period (d)", FieldDefinition.Kind.NUMERIC),
                new FieldDefinition("parentBody", "Parent body", FieldDefinition.Kind.TEXT),
                new FieldDefinition("planetType", "Type", FieldDefinition.Kind.TEXT)));
        this.symbolToName = new HashMap<String, String>();
        this.moons = new ArrayList<PackRecord>();
        for (PackRecord r : records) {
            String symbol = r.property("symbol");
            if (symbol != null && !symbol.isEmpty())
                symbolToName.put(symbol, r.displayName());
            if ("moon".equals(r.category()))
                moons.add(r);
        }
    }

    /**
     * Records whose validity window (validFrom/validTo, ISO dates or null =
     * unbounded) contains the reference date. Without a date, only records
     * valid "now" (unbounded or with a sourceDate) are returned.
     */
    public List<PackRecord> validAt(String referenceDate) {
        List<PackRecord> out = new ArrayList<PackRecord>();
        for (PackRecord r : records()) {
            if (validAt(r, referenceDate))
                out.add(r);
        }
        return out;
    }

    private static boolean validAt(PackRecord r, String referenceDate) {
        String from = r.validFrom();
        String to = r.validTo();
        if (from == null && to == null)
            return true;
        if (referenceDate == null)
            return false;
        int ref = Integer.parseInt(referenceDate.replaceAll("-", ""));
        if (from != null && Integer.parseInt(from.replaceAll("-", "")) > ref)
            return false;
        if (to != null && Integer.parseInt(to.replaceAll("-", "")) < ref)
            return false;
        return true;
    }

    /** Moons of one planet by display name (Europa -> Jupiter). */
    public List<PackRecord> moonsOf(String parent) {
        List<PackRecord> out = new ArrayList<PackRecord>();
        if (parent == null)
            return out;
        String need = parent.trim();
        for (PackRecord m : moons)
            if (need.equalsIgnoreCase(m.property("parentBody")))
                out.add(m);
        return out;
    }

    /** Body by name (Mars) or symbol (♂). */
    public PackRecord byIdentity(String q) {
        if (q == null)
            return null;
        String name = symbolToName.get(q.trim());
        if (name == null)
            name = q.trim();
        for (PackRecord r : records()) {
            if (r.displayName().equalsIgnoreCase(name))
                return r;
            for (String a : r.aliases())
                if (a.equalsIgnoreCase(q.trim()))
                    return r;
        }
        return null;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Astronomy";
    }

    @Override
    public SearchResult search(FilterSpec spec) {
        List<EntityResult> hits = new ArrayList<EntityResult>();
        if (spec.isIdentity()) {
            String q = spec.value() == null ? "" : spec.value().trim();
            if (q.isEmpty())
                return wrap(hits, records().size());
            PackRecord byIdentity = byIdentity(q);
            if (byIdentity != null) {
                hits.add(toResult(byIdentity));
                return wrap(hits, records().size());
            }
            return wrap(hits, records().size());
        }
        if ("parentBody".equals(spec.field()) && spec.op() == FilterSpec.Op.EQ) {
            List<PackRecord> parents = moonsOf(spec.value());
            for (PackRecord m : parents)
                hits.add(toResult(m));
            return wrap(hits, records().size());
        }
        String field = spec.field();
        if (isNumericField(field))
            return wrap(matchNumericField(field, spec), records().size());
        return wrap(matchTextField(field, spec), records().size());
    }

    private static boolean isNumericField(String field) {
        return "orderFromSun".equals(field) || "knownMoonCount".equals(field)
                || "ringCount".equals(field) || "diameterKm".equals(field)
                || "orbitalPeriodDays".equals(field) || "rotationHours".equals(field);
    }
}