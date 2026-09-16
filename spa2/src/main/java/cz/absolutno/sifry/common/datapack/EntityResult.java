package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One lookup result: the record's display text plus its structured properties
 * (selectable for Keep / Send To... / Insert Previous / Logbook). Matches the
 * future {@code EntityValue} shape without depending on the puzzle-value model.
 */
public final class EntityResult {

    private final String id;
    private final String displayText;
    private final String category;
    private final Map<String, String> properties;

    EntityResult(String id, String displayText, String category, Map<String, String> properties) {
        this.id = id;
        this.displayText = displayText;
        this.category = category;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<String, String>(properties));
    }

    public String id() {
        return id;
    }

    public String displayText() {
        return displayText;
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

    @Override
    public String toString() {
        return displayText;
    }
}