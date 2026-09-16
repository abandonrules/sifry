package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One underlying record of a data pack: canonical id + display name, aliases
 * preserved separately, a category, and any structured properties. Optional
 * time bounds for date-sensitive facts (e.g. knownMoonCount).
 */
public final class PackRecord {

    private final String id;
    private final String displayName;
    private final List<String> aliases;
    private final String category;
    private final Map<String, String> properties;
    private final String validFrom;
    private final String validTo;
    private final String sourceDate;

    private PackRecord(String id, String displayName, List<String> aliases, String category,
                       Map<String, String> properties, String validFrom, String validTo,
                       String sourceDate) {
        this.id = id;
        this.displayName = displayName;
        this.aliases = Collections.unmodifiableList(new ArrayList<String>(aliases));
        this.category = category;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<String, String>(properties));
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.sourceDate = sourceDate;
    }

    public static Builder builder(String id, String displayName) {
        return new Builder(id, displayName);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> aliases() {
        return aliases;
    }

    public String category() {
        return category;
    }

    public Map<String, String> properties() {
        return properties;
    }

    public String property(String key) {
        return properties.get(key);
    }

    public String validFrom() {
        return validFrom;
    }

    public String validTo() {
        return validTo;
    }

    public String sourceDate() {
        return sourceDate;
    }

    public static final class Builder {
        private final String id;
        private final String displayName;
        private final List<String> aliases = new ArrayList<String>();
        private String category;
        private final Map<String, String> properties = new LinkedHashMap<String, String>();
        private String validFrom;
        private String validTo;
        private String sourceDate;

        Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder alias(String a) {
            if (a != null && !a.isEmpty())
                aliases.add(a);
            return this;
        }

        public Builder category(String c) {
            this.category = c;
            return this;
        }

        public Builder property(String k, String v) {
            if (v != null)
                properties.put(k, v);
            return this;
        }

        public Builder valid(String from, String to, String source) {
            this.validFrom = from;
            this.validTo = to;
            this.sourceDate = source;
            return this;
        }

        public PackRecord build() {
            return new PackRecord(id, displayName, aliases, category, properties,
                    validFrom, validTo, sourceDate);
        }
    }
}