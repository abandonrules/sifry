package cz.absolutno.sifry.common.datapack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetManager;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class CalvinHobbesDataPackTest {

    private CalvinHobbesDataPack pack;

    @Before
    public void setUp() throws IOException {
        AssetManager assets = RuntimeEnvironment.getApplication().getApplicationContext().getAssets();
        pack = Packs.calvinhobbes(assets);
    }

    @Test
    public void manifestIsLoaded() {
        assertEquals("calvinhobbes", pack.manifest().packId());
        assertEquals("Calvin and Hobbes", pack.displayName());
    }

    @Test
    public void everyRegisteredPackLoads() throws IOException {
        AssetManager assets = RuntimeEnvironment.getApplication().getApplicationContext().getAssets();
        assertNotNull(Packs.astronomy(assets));
        assertNotNull(Packs.zodiac(assets));
        assertNotNull(Packs.nato(assets));
        assertNotNull(Packs.greek(assets));
        assertNotNull(Packs.calvinhobbes(assets));
    }

    @Test
    public void castIsPresent() {
        String[] names = { "Calvin", "Hobbes", "Susie Derkson", "Miss Wormwood", "Moe",
                "Rosalyn", "Mom", "Dad" };
        for (String n : names) {
            PackRecord r = pack.byIdentity(n);
            assertNotNull(n, r);
            assertEquals("character", r.category());
        }
    }

    @Test
    public void characterCount() {
        assertTrue(pack.byCategory("character").size() >= 15);
    }

    @Test
    public void bothSusieSpellingsResolve() {
        assertEquals("Susie Derkson", pack.byIdentity("Susie").displayName());
        assertNotNull(pack.byIdentity("susie derkins"));
    }

    @Test
    public void namedFirstAppearances() {
        assertEquals("1985-11-18", pack.byIdentity("Calvin").property("stripDate"));
        assertEquals("1985-11-26", pack.byIdentity("Mom").property("stripDate"));
        assertEquals("1985-12-05", pack.byIdentity("Susie Derkson").property("stripDate"));
        assertEquals("1986-01-30", pack.byIdentity("Moe").property("stripDate"));
        assertEquals("1986-05-15", pack.byIdentity("Rosalyn").property("stripDate"));
    }

    @Test
    public void recurringElementsArePresent() {
        String[] names = { "Calvin's cardboard box", "Transmogrifier", "Wagon and sled rides",
                "Calvinball", "G.R.O.S.S.", "Horrendous Space Kablooie", "Spaceman Spiff" };
        for (String n : names)
            assertNotNull(n, pack.byIdentity(n));
    }

    @Test
    public void themesArePresent() {
        assertTrue(pack.byCategory("theme").size() >= 10);
        assertTrue(pack.byCategory("device").size() >= 5);
        assertTrue(pack.byCategory("fact").size() >= 5);
    }

    @Test
    public void quotesCarryTheirStripDate() {
        EntityResult r = pack.search(FilterSpec.identity("the perks of being feral")).results().get(0);
        assertEquals("Hobbes", r.property("speaker"));
        assertEquals("1989-03-12", r.property("stripDate"));
        assertEquals("1989", r.property("year"));
        assertEquals("quote", r.category());
    }

    @Test
    public void quotesBySpeaker() {
        List<PackRecord> hobbes = pack.quotesBySpeaker("Hobbes");
        assertTrue(hobbes.size() >= 10);
        for (PackRecord r : hobbes)
            assertEquals("Hobbes", r.property("speaker"));
    }

    @Test
    public void identityPrefersTheCastOverTheLines() {
        EntityResult r = pack.search(FilterSpec.identity("Hobbes")).results().get(0);
        assertEquals("character", r.category());
        assertEquals("Hobbes", r.displayText());
    }

    @Test
    public void identityFallsBackToStripDate() {
        SearchResult r = pack.search(FilterSpec.identity("1993-05"));
        assertTrue(r.matched() >= 2);
        for (EntityResult e : r.results())
            assertTrue(e.property("stripDate").startsWith("1993-05"));
    }

    @Test
    public void finalStripIsPresent() {
        EntityResult r = pack.search(FilterSpec.identity("let's go exploring")).results().get(0);
        assertEquals("1995-12-31", r.property("stripDate"));
        assertEquals("1995", r.property("year"));
    }

    @Test
    public void quotesCoverTheWholeRun() {
        List<PackRecord> first = pack.stripsInYear(1985);
        List<PackRecord> last = pack.stripsInYear(1995);
        assertTrue(first.size() >= 3);
        assertTrue(last.size() >= 10);
    }

    @Test
    public void datePrefixLookup() {
        List<PackRecord> may1993 = pack.byDatePrefix("1993-05");
        assertTrue(may1993.size() >= 2);
        for (PackRecord r : may1993)
            assertTrue(r.property("stripDate").startsWith("1993-05"));
        assertTrue(pack.byDatePrefix("no-such-date").isEmpty());
    }

    @Test
    public void searchByTheme() {
        SearchResult r = pack.search(FilterSpec.on("theme", FilterSpec.Op.EQ, "Winter", null));
        assertTrue(r.matched() >= 5);
        for (EntityResult e : r.results())
            assertEquals("Winter", e.property("theme"));
    }

    @Test
    public void searchByYear() {
        SearchResult r = pack.search(FilterSpec.on("year", FilterSpec.Op.EQ, "1986", null));
        assertTrue(r.matched() > 0);
        for (EntityResult e : r.results())
            assertEquals("1986", e.property("year"));
    }

    @Test
    public void typeFieldMirrorsCategory() {
        for (PackRecord r : pack.byCategory("character"))
            assertEquals(r.category(), r.property("type"));
        for (PackRecord r : pack.byCategory("quote"))
            assertEquals(r.category(), r.property("type"));
    }

    @Test
    public void dropDownSizedFieldsStaySmall() {
        assertTrue(pack.distinctValues("speaker").size() <= 15);
        assertTrue(pack.distinctValues("theme").size() <= 15);
        assertTrue(pack.distinctValues("type").size() <= 15);
    }

    @Test
    public void distinctSpeakers() {
        assertEquals(5, pack.distinctValues("speaker").size());
    }

    @Test
    public void everyQuoteHasDateSpeakerAndTheme() {
        for (PackRecord r : pack.byCategory("quote")) {
            assertNotNull(r.property("stripDate"));
            assertNotNull(r.property("speaker"));
            assertNotNull(r.property("theme"));
            assertNotNull(r.property("year"));
            assertEquals(r.property("stripDate").substring(0, 4), r.property("year"));
            assertEquals(r.property("stripDate"), r.sourceDate());
        }
    }

    @Test
    public void stripDatesAreOrdered() {
        String prev = "";
        for (PackRecord r : pack.byCategory("quote")) {
            String date = r.property("stripDate");
            assertTrue(date + " after " + prev, date.compareTo(prev) >= 0);
            prev = date;
        }
    }

    @Test
    public void noRecordLivesOutsideTheRun() {
        for (PackRecord r : pack.byCategory("quote")) {
            String date = r.property("stripDate");
            assertTrue(date + " too early", date.compareTo("1985-11-18") >= 0);
            assertTrue(date + " too late", date.compareTo("1995-12-31") <= 0);
        }
    }

    @Test
    public void emptyQueryListsAll() {
        SearchResult r = pack.search(FilterSpec.identity(""));
        assertEquals(r.scanned(), r.matched());
        assertEquals(r.matched(), r.results().size());
        assertTrue(r.matched() > 200);
    }

    @Test
    public void absentFieldHasNoDistinctValues() {
        assertTrue(pack.distinctValues("noSuchField").isEmpty());
    }

    @Test
    public void unknownIdentityHasNoMatch() {
        assertTrue(pack.search(FilterSpec.identity("nothing like this exists")).results().isEmpty());
        assertNull(pack.byIdentity(null));
    }
}