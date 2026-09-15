package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Western tropical zodiac and month-based birthstones. A sign is a spanned
 * interval given as month/day inclusive (Capricorn crosses the year end, so
 * intervals are compared in day-of-year space and wrap). Matching is
 * structural: birthstone month order comes from the listener's month table,
 * not from the sign order.
 */
public final class ZodiacDataPack extends BaseDataPack {

    public static final String ID = "zodiac";

    private static final int[] MONTH_DAYS = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

    private final List<PackRecord> signs;
    private final List<PackRecord> birthstones;
    private final Map<String, String> symbolToSign;
    private final Map<String, PackRecord> monthToStone;

    public ZodiacDataPack(PackManifest manifest, List<PackRecord> records) {
        super(manifest, records, Arrays.asList(
                new FieldDefinition("date", "Birth date", FieldDefinition.Kind.DATE),
                new FieldDefinition("ordinal", "Sign order", FieldDefinition.Kind.NUMERIC),
                new FieldDefinition("element", "Element", FieldDefinition.Kind.TEXT),
                new FieldDefinition("month", "Month", FieldDefinition.Kind.NUMERIC)));
        this.signs = new ArrayList<PackRecord>();
        this.birthstones = new ArrayList<PackRecord>();
        this.symbolToSign = new HashMap<String, String>();
        this.monthToStone = new HashMap<String, PackRecord>();
        for (PackRecord r : records) {
            if ("birthstone".equals(r.category()))
                birthstones.add(r);
            else {
                signs.add(r);
                String symbol = r.property("symbol");
                if (symbol != null && !symbol.isEmpty())
                    symbolToSign.put(symbol, r.displayName());
            }
        }
        for (PackRecord b : birthstones) {
            String month = b.id();
            monthToStone.put(month.toLowerCase(Locale.ROOT), b);
            String monthName = b.property("monthName");
            if (monthName != null)
                monthToStone.put(monthName.toLowerCase(Locale.ROOT), b);
        }
    }

    static int dayOfYear(int month, int day) {
        int doy = day;
        for (int m = 1; m < month; m++)
            doy += MONTH_DAYS[m - 1];
        return doy;
    }

    /**
     * Sign for a calendar date, or null if the month/day is impossible. Dates
     * in the Capricorn interval wrap correctly.
     */
    public PackRecord signForDate(int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > MONTH_DAYS[month - 1])
            return null;
        int doy = dayOfYear(month, day);
        for (PackRecord r : signs) {
            int start = dayOfYear(intProp(r, "startMonth"), intProp(r, "startDay"));
            int end = dayOfYear(intProp(r, "endMonth"), intProp(r, "endDay"));
            if (start <= end) {
                if (doy >= start && doy <= end)
                    return r;
            } else {
                // year-crossing (Capricorn): before start-of-year or after it
                if (doy >= start || doy <= end)
                    return r;
            }
        }
        return null;
    }

    private static int intProp(PackRecord r, String key) {
        return Integer.parseInt(r.property(key).trim());
    }

    /** Birthstone record for a month name (case-insensitive) or numeric month. */
    public PackRecord birthstoneFor(String month) {
        if (month == null)
            return null;
        String m = month.trim().toLowerCase(Locale.ROOT);
        PackRecord byName = monthToStone.get(m);
        if (byName != null)
            return byName;
        try {
            int n = Integer.parseInt(m);
            if (n >= 1 && n <= 12)
                return monthToStone.get(Birthstones.MONTHS[n - 1].toLowerCase(Locale.ROOT));
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /** Reverse: stone name -> birthstone record(s); empty if none. */
    public List<PackRecord> monthsWithStone(String stone) {
        List<PackRecord> out = new ArrayList<PackRecord>();
        if (stone == null)
            return out;
        String need = stone.trim().toLowerCase(Locale.ROOT);
        for (PackRecord b : birthstones)
            if (b.displayName().toLowerCase(Locale.ROOT).equals(need))
                out.add(b);
        return out;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Zodiac & Birthday";
    }

    @Override
    public SearchResult search(FilterSpec spec) {
        if (spec.isIdentity()) {
            String q = spec.value() == null ? "" : spec.value().trim();
            List<EntityResult> hits = new ArrayList<EntityResult>();
            if (q.isEmpty())
                return wrap(hits, records().size());
            PackRecord bySymbol = signBySymbol(q);
            if (bySymbol != null) {
                hits.add(toResult(bySymbol));
                return wrap(hits, records().size());
            }
            PackRecord bySignName = signByName(q);
            if (bySignName != null) {
                hits.add(toResult(bySignName));
                return wrap(hits, records().size());
            }
            PackRecord stone = birthstoneFor(q);
            if (stone != null) {
                hits.add(toResult(stone));
                return wrap(hits, records().size());
            }
            for (PackRecord b : monthsWithStone(q)) {
                hits.add(toResult(b));
                return wrap(hits, records().size());
            }
            Integer month = parseInt(q);
            if (month != null && month >= 1 && month <= 12) {
                PackRecord byMonthStone = birthstoneFor(String.valueOf(month));
                if (byMonthStone != null) {
                    hits.add(toResult(byMonthStone));
                    return wrap(hits, records().size());
                }
            }
            return wrap(hits, records().size());
        }
        if ("date".equals(spec.field()) && spec.op() == FilterSpec.Op.EQ) {
            int[] md = parseMonthDay(spec.value());
            List<EntityResult> hits = new ArrayList<EntityResult>();
            if (md != null) {
                PackRecord sign = signForDate(md[0], md[1]);
                if (sign != null) {
                    hits.add(toResult(sign));
                    return wrap(hits, records().size());
                }
            }
            return wrap(hits, records().size());
        }
        return wrap(matchNumericField(spec.field(), spec), records().size());
    }

    private PackRecord signByName(String name) {
        String need = name.trim().toLowerCase(Locale.ROOT);
        for (PackRecord s : signs)
            if (s.displayName().toLowerCase(Locale.ROOT).equals(need))
                return s;
        return null;
    }

    private PackRecord signBySymbol(String symbol) {
        String name = symbolToSign.get(symbol.trim());
        return name == null ? null : signByName(name);
    }

    static int[] parseMonthDay(String s) {
        if (s == null)
            return null;
        String[] parts = s.trim().split("\\s+");
        if (parts.length != 2)
            return null;
        int month = Birthstones.monthNumber(parts[0]);
        if (month == 0)
            return null;
        try {
            int day = Integer.parseInt(parts[1]);
            return new int[] { month, day };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Month name/spoken form table shared with the birthstone loader. */
    public static final class Birthstones {
        public static final String[] MONTHS = { "January", "February", "March", "April", "May",
                "June", "July", "August", "September", "October", "November", "December" };

        public static int monthNumber(String name) {
            if (name == null)
                return 0;
            String n = name.trim();
            for (int i = 0; i < MONTHS.length; i++)
                if (MONTHS[i].equalsIgnoreCase(n)
                        || MONTHS[i].substring(0, 3).equalsIgnoreCase(n))
                    return i + 1;
            return 0;
        }
    }
}