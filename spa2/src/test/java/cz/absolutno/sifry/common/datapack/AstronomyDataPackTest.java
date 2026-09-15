package cz.absolutno.sifry.common.datapack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class AstronomyDataPackTest {

    private AstronomyDataPack pack;

    @Before
    public void setUp() throws IOException {
        AssetManager assets = RuntimeEnvironment.getApplication().getApplicationContext().getAssets();
        pack = Packs.astronomy(assets);
    }

    @Test
    public void sunIsOrderZero() {
        PackRecord r = pack.byIdentity("Sun");
        assertNotNull(r);
        assertEquals("0", r.property("orderFromSun"));
    }

    @Test
    public void mercuryIsOrderOne() {
        assertEquals("1", pack.byIdentity("Mercury").property("orderFromSun"));
    }

    @Test
    public void marsIsFourthPlanet() {
        assertEquals("4", pack.byIdentity("Mars").property("orderFromSun"));
    }

    @Test
    public void europaBelongsToJupiter() {
        PackRecord r = pack.byIdentity("Europa");
        assertNotNull(r);
        assertEquals("Jupiter", r.property("parentBody"));
        assertEquals("moon", r.category());
    }

    @Test
    public void titanBelongsToSaturn() {
        assertEquals("Saturn", pack.byIdentity("Titan").property("parentBody"));
    }

    @Test
    public void charonBelongsToPluto() {
        assertEquals("Pluto", pack.byIdentity("Charon").property("parentBody"));
    }

    @Test
    public void moonsOfPlanet() {
        List<PackRecord> moons = pack.moonsOf("Jupiter");
        assertTrue(moons.size() >= 4);
        boolean europa = false;
        for (PackRecord m : moons)
            if (m.displayName().equals("Europa"))
                europa = true;
        assertTrue(europa);
    }

    @Test
    public void symbolLookup() {
        assertEquals("Mars", pack.byIdentity("♂").displayName());
        assertEquals("Jupiter", pack.byIdentity("♃").displayName());
    }

    @Test
    public void greekMoonCountIsTimeAware() {
        PackRecord jupiter = pack.byIdentity("Jupiter");
        assertNotNull(jupiter.property("knownMoonCount"));
        assertEquals("95", jupiter.property("knownMoonCount"));
        // date-sensitive facts state their window on the same record
        assertNotNull(jupiter.validFrom());
        assertNotNull(jupiter.sourceDate());
    }

    @Test
    public void validAtFiltersByReferenceDate() {
        List<PackRecord> now = pack.validAt("2024-06-01");
        boolean hasJupiter = false;
        for (PackRecord r : now)
            if (r.displayName().equals("Jupiter"))
                hasJupiter = true;
        assertTrue(hasJupiter);
    }

    @Test
    public void validNowIncludesUnboundedRecords() {
        List<PackRecord> now = pack.validAt(null);
        boolean hasEarth = false;
        for (PackRecord r : now)
            if (r.displayName().equals("Earth"))
                hasEarth = true;
        assertTrue(hasEarth);
    }

    @Test
    public void moonsAreOnlyOfTheirParent() {
        // Europa is a Jupiter moon, cannot belong to Saturn
        List<PackRecord> saturnMoons = pack.moonsOf("Saturn");
        for (PackRecord m : saturnMoons)
            assertTrue(!m.displayName().equals("Europa"));
    }

    @Test
    public void dwarfPlanetsIncluded() {
        assertNotNull(pack.byIdentity("Ceres"));
        assertNotNull(pack.byIdentity("Pluto"));
        assertEquals("dwarf", pack.byIdentity("Eris").category());
    }

    @Test
    public void constellationIdentity() {
        assertEquals("constellation", pack.byIdentity("Orion").category());
        assertNotNull(pack.byIdentity("Ursa Major"));
    }

    @Test
    public void emptyQueryListsAll() {
        SearchResult r = pack.search(FilterSpec.identity(""));
        assertEquals(r.scanned(), r.matched());
        assertEquals(r.matched(), r.results().size());
        assertTrue(r.matched() > 0);
    }

    @Test
    public void distinctParentBodies() {
        List<String> v = pack.distinctValues("parentBody");
        assertEquals(Arrays.asList("Earth", "Jupiter", "Mars", "Neptune", "Pluto", "Saturn"), v);
    }

    @Test
    public void distinctPlanetTypesSorted() {
        assertEquals(Arrays.asList("Dwarf Planet", "Gas Giant", "Ice Giant", "Rocky", "Star"),
                pack.distinctValues("planetType"));
    }

    @Test
    public void absentFieldHasNoDistinctValues() {
        assertTrue(pack.distinctValues("noSuchField").isEmpty());
    }
}