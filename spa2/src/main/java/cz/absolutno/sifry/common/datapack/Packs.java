package cz.absolutno.sifry.common.datapack;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Loads versioned packs from the bundled {@code assets/data/packs} tree. */
public final class Packs {

    private Packs() {
    }

    public static final String ROOT = "data/packs";

    public static NatoDataPack nato(AssetManager assets) throws IOException {
        return C.nato(assets);
    }

    public static GreekDataPack greek(AssetManager assets) throws IOException {
        return C.greek(assets);
    }

    public static ZodiacDataPack zodiac(AssetManager assets) throws IOException {
        return C.zodiac(assets);
    }

    public static AstronomyDataPack astronomy(AssetManager assets) throws IOException {
        return C.astronomy(assets);
    }

    private static final class C {
        static PackManifest manifest(AssetManager assets, String pack) throws IOException {
            try (InputStream in = assets.open(ROOT + '/' + pack + '/' + PackJsonLoader.MANIFEST)) {
                return PackJsonLoader.readManifest(in);
            }
        }

        static List<PackRecord> records(AssetManager assets, String pack) throws IOException {
            try (InputStream in = assets.open(ROOT + '/' + pack + "/records.json")) {
                return PackJsonLoader.readRecords(in);
            }
        }

        static NatoDataPack nato(AssetManager assets) throws IOException {
            return new NatoDataPack(manifest(assets, "nato"), records(assets, "nato"));
        }

        static GreekDataPack greek(AssetManager assets) throws IOException {
            return new GreekDataPack(manifest(assets, "greek"), records(assets, "greek"));
        }

        static ZodiacDataPack zodiac(AssetManager assets) throws IOException {
            return new ZodiacDataPack(manifest(assets, "zodiac"), records(assets, "zodiac"));
        }

        static AstronomyDataPack astronomy(AssetManager assets) throws IOException {
            return new AstronomyDataPack(manifest(assets, "astronomy"), records(assets, "astronomy"));
        }
    }
}