package cz.absolutno.sifry.common.datapack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class NatoDataPackTest {

    private NatoDataPack pack;

    @Before
    public void setUp() throws IOException {
        AssetManager assets = RuntimeEnvironment.getApplication().getApplicationContext().getAssets();
        pack = Packs.nato(assets);
    }

    @Test
    public void letterToWord() {
        assertEquals("Foxtrot", pack.letterSpelling("F").displayText());
        assertEquals("Zulu", pack.letterSpelling("z").displayText());
        assertNotNull(pack.letterSpelling("A"));
    }

    @Test
    public void wordToLetter() {
        assertEquals("F", pack.wordLetter("Foxtrot").id());
        assertEquals("X", pack.wordLetter("X-ray").id());
        assertEquals("X", pack.wordLetter("Xray").id());
        assertEquals("X", pack.wordLetter("X Ray").id());
        assertNotNull(pack.letterSpelling("I"));
        assertEquals("Juliett", pack.letterSpelling("J").displayText());
    }

    @Test
    public void aliasNormalization() {
        assertEquals("A", pack.wordLetter("Alpha").id());
        assertEquals("A", pack.wordLetter("Alfa").id());
        assertEquals("J", pack.wordLetter("Juliet").id());
    }

    @Test
    public void spellWords() {
        assertEquals("Sierra India Foxtrot Romeo Yankee", pack.spell("SIFRY"));
        assertEquals("Sierra India Foxtrot Romeo Yankee", pack.spell("sifry"));
        assertEquals("Zero Nine", pack.spell("09"));
    }

    @Test
    public void unspellWords() {
        assertEquals("SIFRY", pack.unspell("Sierra India Foxtrot Romeo Yankee"));
    }

    @Test
    public void fullAlphabetRoundTrip() {
        for (String s : new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" }) {
            EntityResult word = pack.letterSpelling(s);
            assertNotNull(word);
            assertEquals(s, pack.wordLetter(word.displayText()).id());
        }
    }

    @Test
    public void searchIdentityFindsLetter() {
        EntityResult r = pack.search(FilterSpec.identity("F")).results().get(0);
        assertEquals("Foxtrot", r.displayText());
    }

    @Test
    public void searchIdentityFindsWord() {
        EntityResult r = pack.search(FilterSpec.identity("Foxtrot")).results().get(0);
        assertEquals("F", r.id());
    }

    @Test
    public void digitsSpelledSeparately() {
        assertNotNull(pack.digitSpelling("4"));
        assertEquals("Four", pack.digitSpelling("4").displayText());
        assertTrue(pack.search(FilterSpec.identity("4")).results().get(0).displayText()
                .equals("Four"));
    }

    @Test
    public void emptyQueryListsAll() {
        SearchResult r = pack.search(FilterSpec.identity(""));
        assertEquals(r.scanned(), r.matched());
        assertEquals(r.matched(), r.results().size());
        assertTrue(r.matched() > 0);
    }
}