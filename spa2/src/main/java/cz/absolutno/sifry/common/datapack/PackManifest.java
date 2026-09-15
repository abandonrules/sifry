package cz.absolutno.sifry.common.datapack;

/**
 * Version metadata for one pack, so datasets can evolve without breaking the
 * fixtures: packId, schemaVersion, dataVersion, displayName, referenceDate.
 */
public final class PackManifest {

    private final String packId;
    private final int schemaVersion;
    private final int dataVersion;
    private final String displayName;
    private final String referenceDate;

    public PackManifest(String packId, int schemaVersion, int dataVersion,
                        String displayName, String referenceDate) {
        this.packId = packId;
        this.schemaVersion = schemaVersion;
        this.dataVersion = dataVersion;
        this.displayName = displayName;
        this.referenceDate = referenceDate;
    }

    public String packId() {
        return packId;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public int dataVersion() {
        return dataVersion;
    }

    public String displayName() {
        return displayName;
    }

    public String referenceDate() {
        return referenceDate;
    }
}