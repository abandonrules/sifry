package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The Calvin and Hobbes comic strip: its cast, its recurring devices and
 * running gags, its themes, and a dated quotation corpus. Quotes are one
 * record per spoken line, each carrying the strip's publication date, so a
 * line can be found by speaker, theme, year or date prefix. Matching is
 * mechanical: identity over display names and aliases, then date prefixes,
 * then a last-resort case-insensitive substring over display names, aliases
 * and notes (so a phrase such as "the perks of being feral" finds the quoted
 * line), then plain property comparison.
 */
public final class CalvinHobbesDataPack extends BaseDataPack {

    public static final String ID = "calvinhobbes";

    public CalvinHobbesDataPack(PackManifest manifest, List<PackRecord> records) {
        super(manifest, records, Arrays.asList(
                new FieldDefinition("type", "Type", FieldDefinition.Kind.TEXT),
                new FieldDefinition("speaker", "Speaker", FieldDefinition.Kind.TEXT),
                new FieldDefinition("theme", "Theme", FieldDefinition.Kind.TEXT),
                new FieldDefinition("year", "Year", FieldDefinition.Kind.NUMERIC),
                new FieldDefinition("stripDate", "Strip date", FieldDefinition.Kind.TEXT),
                new FieldDefinition("note", "Note", FieldDefinition.Kind.TEXT)));
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Calvin and Hobbes";
    }

    /** Every record of one category, e.g. {@code character}, {@code quote}, {@code theme}. */
    public List<PackRecord> byCategory(String category) {
        List<PackRecord> out = new ArrayList<PackRecord>();
        if (category == null)
            return out;
        String want = category.trim().toLowerCase(Locale.ROOT);
        for (PackRecord r : records())
            if (want.equals(r.category()))
                out.add(r);
        return out;
    }

    /** Record by display name or alias (case-insensitive), e.g. {@code Calvin}, {@code Susie}. */
    public PackRecord byIdentity(String query) {
        if (query == null)
            return null;
        String q = query.trim();
        if (q.isEmpty())
            return null;
        for (PackRecord r : records())
            if (r.displayName().equalsIgnoreCase(q))
                return r;
        for (PackRecord r : records())
            for (String a : r.aliases())
                if (a.equalsIgnoreCase(q))
                    return r;
        return null;
    }

    /** Lines spoken by one character, e.g. {@code Hobbes}. */
    public List<PackRecord> quotesBySpeaker(String speaker) {
        List<PackRecord> out = new ArrayList<PackRecord>();
        if (speaker == null)
            return out;
        String want = speaker.trim().toLowerCase(Locale.ROOT);
        for (PackRecord r : records()) {
            String s = r.property("speaker");
            if (s != null && want.equals(s.toLowerCase(Locale.ROOT)))
                out.add(r);
        }
        return out;
    }

    /** First record of each distinct strip date in a year, oldest first. */
    public List<PackRecord> stripsInYear(int year) {
        List<PackRecord> out = new ArrayList<PackRecord>();
        String seen = "";
        for (PackRecord r : records()) {
            String date = r.property("stripDate");
            if (date == null || !date.startsWith(String.valueOf(year)))
                continue;
            if (date.equals(seen))
                continue;
            seen = date;
            out.add(r);
        }
        return out;
    }

    /** Records from strips published on a date prefix, e.g. {@code 1993-05}. */
    public List<PackRecord> byDatePrefix(String datePrefix) {
        List<PackRecord> out = new ArrayList<PackRecord>();
        if (datePrefix == null)
            return out;
        String want = datePrefix.trim();
        if (want.isEmpty())
            return out;
        for (PackRecord r : records()) {
            String date = r.property("stripDate");
            if (date != null && date.startsWith(want))
                out.add(r);
        }
        return out;
    }

    @Override
    public SearchResult search(FilterSpec spec) {
        List<EntityResult> hits = new ArrayList<EntityResult>();
        if (spec.isIdentity()) {
            String q = spec.value() == null ? "" : spec.value().trim();
            if (q.isEmpty())
                return listAll();
            PackRecord hit = byIdentity(q);
            if (hit != null) {
                hits.add(toResult(hit));
                return wrap(hits, records().size());
            }
            for (PackRecord r : byDatePrefix(q))
                hits.add(toResult(r));
            if (!hits.isEmpty())
                return wrap(hits, records().size());
            String ql = q.toLowerCase(Locale.ROOT);
            for (PackRecord r : records())
                if (identityTextContains(r, ql))
                    hits.add(toResult(r));
            return wrap(hits, records().size());
        }
        return wrap(matchField(spec.field(), spec), records().size());
    }

    /** Case-insensitive substring over the record's display name, aliases and note. */
    private static boolean identityTextContains(PackRecord r, String ql) {
        if (r.displayName().toLowerCase(Locale.ROOT).contains(ql))
            return true;
        for (String a : r.aliases())
            if (a.toLowerCase(Locale.ROOT).contains(ql))
                return true;
        String note = r.property("note");
        return note != null && note.toLowerCase(Locale.ROOT).contains(ql);
    }
}