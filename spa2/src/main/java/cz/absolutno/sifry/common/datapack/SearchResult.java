package cz.absolutno.sifry.common.datapack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a {@link PackData#search(FilterSpec)}. A deterministic ordered
 * list of matching entities; no ranking.
 */
public final class SearchResult {

    private final List<EntityResult> results;
    private final int matched;
    private final int scanned;

    public SearchResult(List<EntityResult> results, int matched, int scanned) {
        this.results = Collections.unmodifiableList(new ArrayList<EntityResult>(results));
        this.matched = matched;
        this.scanned = scanned;
    }

    public List<EntityResult> results() {
        return results;
    }

    public boolean isEmpty() {
        return results.isEmpty();
    }

    public int matched() {
        return matched;
    }

    public int scanned() {
        return scanned;
    }
}