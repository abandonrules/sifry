package cz.absolutno.sifry.common.datapack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reads versioned packs from JSON: a {@code manifest.json} plus one record
 * array per file (planets.json, moons.json, ...). Records keep the shared
 * shape from the SOA: {@code id, displayName, aliases[], category,
 * properties{}, validFrom, validTo, sourceDate}. Any unknown property is
 * preserved verbatim, so packs can carry pack-specific fields (symbol,
 * orderFromSun, parentBody, ...) without schema drift.
 */
public final class PackJsonLoader {

    public static final String MANIFEST = "manifest.json";

    private PackJsonLoader() {
    }

    public static PackManifest readManifest(InputStream in) throws IOException {
        try {
            JSONObject o = new JSONObject(readAll(in));
            return new PackManifest(
                    o.getString("packId"),
                    o.getInt("schemaVersion"),
                    o.getInt("dataVersion"),
                    o.getString("displayName"),
                    o.optString("referenceDate", null));
        } catch (JSONException e) {
            throw new IOException("invalid pack manifest: " + e.getMessage(), e);
        }
    }

    public static List<PackRecord> readRecords(InputStream in) throws IOException {
        try {
            JSONArray arr = new JSONArray(readAll(in));
            List<PackRecord> out = new ArrayList<PackRecord>(arr.length());
            for (int i = 0; i < arr.length(); i++)
                out.add(record(arr.getJSONObject(i)));
            return out;
        } catch (JSONException e) {
            throw new IOException("invalid pack records: " + e.getMessage(), e);
        }
    }

    private static PackRecord record(JSONObject o) throws JSONException {
        String displayName = o.has("displayName") ? o.getString("displayName") : o.getString("name");
        PackRecord.Builder b = PackRecord.builder(o.getString("id"), displayName);
        String category = o.optString("category", null);
        if (category != null)
            b.category(category);
        JSONArray aliases = o.optJSONArray("aliases");
        if (aliases != null)
            for (int i = 0; i < aliases.length(); i++)
                b.alias(aliases.getString(i));
        JSONObject props = o.optJSONObject("properties");
        if (props != null) {
            // time-window facts may live at top level or inside properties
            copyTop(props, o, "validFrom");
            copyTop(props, o, "validTo");
            copyTop(props, o, "sourceDate");
            Iterator<String> keys = props.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                Object v = props.get(k);
                b.property(k, v instanceof String ? (String) v : String.valueOf(v));
            }
        }
        b.valid(o.optString("validFrom", null), o.optString("validTo", null),
                o.optString("sourceDate", null));
        return b.build();
    }

    private static void copyTop(JSONObject props, JSONObject top, String key) throws JSONException {
        if (!props.has(key) && top.has(key))
            props.put(key, top.get(key));
    }

    private static String readAll(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) != -1)
            sb.append(buf, 0, n);
        return sb.toString();
    }
}