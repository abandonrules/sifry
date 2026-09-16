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
public class ZodiacDataPackTest {

    private ZodiacDataPack pack;

    @Before
    public void setUp() throws IOException {
        AssetManager assets = RuntimeEnvironment.getApplication().getApplicationContext().getAssets();
        pack = Packs.zodiac(assets);
    }

    @Test
    public void ariesStartsMar21() {
        assertEquals("Pisces", pack.signForDate(3, 20).displayName());
        assertEquals("Aries", pack.signForDate(3, 21).displayName());
    }

    @Test
    public void capricornCrossesYearBoundary() {
        assertEquals("Capricorn", pack.signForDate(12, 22).displayName());
        assertEquals("Capricorn", pack.signForDate(12, 31).displayName());
        assertEquals("Capricorn", pack.signForDate(1, 1).displayName());
        assertEquals("Capricorn", pack.signForDate(1, 19).displayName());
        assertEquals("Aquarius", pack.signForDate(1, 20).displayName());
        assertEquals("Sagittarius", pack.signForDate(12, 21).displayName());
    }

    @Test
    public void boundaryPairsAreContinuous() {
        assertEquals("Leo", pack.signForDate(7, 23).displayName());
        assertEquals("Cancer", pack.signForDate(7, 22).displayName());
        assertEquals("Virgo", pack.signForDate(8, 23).displayName());
        assertEquals("Libra", pack.signForDate(9, 23).displayName());
        assertEquals("Scorpio", pack.signForDate(10, 23).displayName());
    }

    @Test
    public void monthsStillMatch() {
        assertEquals("Aries", pack.signForDate(4, 5).displayName());
        assertEquals("Gemini", pack.signForDate(5, 25).displayName());
        assertEquals("Cancer", pack.signForDate(6, 30).displayName());
        assertEquals("Sagittarius", pack.signForDate(11, 30).displayName());
    }

    @Test
    public void invalidDatesRejected() {
        assertNull(pack.signForDate(2, 30));
        assertNull(pack.signForDate(0, 1));
        assertNull(pack.signForDate(13, 1));
    }

    @Test
    public void birthstoneByMonthName() {
        assertEquals("Diamond", pack.birthstoneFor("April").displayName());
    }

    @Test
    public void birthstoneByMonthNumber() {
        assertEquals("Ruby", pack.birthstoneFor("7").displayName());
        assertEquals("Sapphire", pack.birthstoneFor("9").displayName());
    }

    @Test
    public void signSymbolIdentity() {
        assertEquals("Aries", pack.search(FilterSpec.identity("♈")).results().get(0).displayText());
    }

    @Test
    public void signNameIdentity() {
        assertEquals("Leo", pack.search(FilterSpec.identity("Leo")).results().get(0).displayText());
    }

    @Test
    public void dateFieldFilter() {
        assertEquals("Aries", pack.search(FilterSpec.identity("Mar 21"))
                .results().get(0).displayText());
    }

    @Test
    public void emptyQueryListsAll() {
        SearchResult r = pack.search(FilterSpec.identity(""));
        assertEquals(r.scanned(), r.matched());
        assertEquals(r.matched(), r.results().size());
        assertTrue(r.matched() > 0);
    }

    @Test
    public void distinctElementsSorted() {
        assertEquals(java.util.Arrays.asList("Air", "Earth", "Fire", "Water"),
                pack.distinctValues("element"));
    }

    @Test
    public void distinctMonthsNumericSorted() {
        List<String> v = pack.distinctValues("month");
        assertEquals(12, v.size());
        assertEquals("1", v.get(0));
        assertEquals("12", v.get(11));
    }
}