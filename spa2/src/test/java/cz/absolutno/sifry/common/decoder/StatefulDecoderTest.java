package cz.absolutno.sifry.common.decoder;

import static org.junit.Assert.assertEquals;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cz.absolutno.sifry.App;
import cz.absolutno.sifry.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class StatefulDecoderTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        prefs.edit().clear().commit();
    }

    @After
    public void tearDown() {
        prefs.edit().clear().commit();
    }

    private void setPref(String key, boolean value) {
        prefs.edit().putBoolean(key, value).commit();
    }

    private static ArrayList<Integer> codes(int... x) {
        ArrayList<Integer> l = new ArrayList<>(x.length);
        for (int i : x) l.add(i);
        return l;
    }

    private static String decode(Decoder d, int... x) {
        return d.decode(codes(x));
    }

    private static List<String> statesSeen(StatefulDecoder d) {
        final List<String> seen = new ArrayList<>();
        d.setOnStateChangedListener(state -> seen.add(state == null ? null : state));
        return seen;
    }

    private static List<String> statesOf(String... s) {
        return Arrays.asList(s);
    }

    // ------------------------------------------------------------------ semafor

    @Test
    public void semaforLetters() {
        StatefulDecoder d = new StatefulDecoder(R.xml.semafor_decoder);
        assertEquals("ABCD", decode(d, 3, 5, 9, 17));
    }

    @Test
    public void semaforNumbersViaSwitch() {
        StatefulDecoder d = new StatefulDecoder(R.xml.semafor_decoder);
        assertEquals("1234", decode(d, 48, 3, 5, 9, 17));
    }

    @Test
    public void semaforSwitchToLettersAgain() {
        StatefulDecoder d = new StatefulDecoder(R.xml.semafor_decoder);
        assertEquals("A1A", decode(d, 3, 48, 3, 80, 3));
    }

    @Test
    public void semaforStatesTraced() {
        StatefulDecoder d = new StatefulDecoder(R.xml.semafor_decoder);
        List<String> seen = statesSeen(d);
        decode(d, 3, 48, 3, 80, 3);
        assertEquals(statesOf("", "", "Num", "Num", "", ""), seen);
    }

    @Test
    public void semaforNumbersDisabled() {
        setPref("pref_sem_num", false);
        StatefulDecoder d = new StatefulDecoder(R.xml.semafor_decoder);
        // 48 no longer switches to digits: it is undecodable ("?"), and 3 stays a letter
        assertEquals("A?A", decode(d, 3, 48, 3));
        // code 80 is the letter J (flag + digit-on mapping in saSmDPismena), never a
        // "back to letters" switch while numbers are disabled
        assertEquals("J", decode(d, 80));
        assertEquals("A", decode(d, 3));
        // no Num state ever enters
        List<String> seen = statesSeen(d);
        decode(d, 3, 48, 3);
        assertEquals(statesOf("", "", ""), seen);
    }

    // ------------------------------------------------------------------ braille

    @Test
    public void brailleLetters() {
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        assertEquals("ABW", decode(d, 1, 3, 58));
    }

    @Test
    public void brailleNumbersViaPrefix() {
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        assertEquals("12", decode(d, 60, 1, 3));
    }

    @Test
    public void brailleNumberPrefixTogglesInWord() {
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        assertEquals("A1A", decode(d, 1, 60, 1, 48, 1));
    }

    @Test
    public void brailleStatesTraced() {
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        List<String> seen = statesSeen(d);
        decode(d, 1, 60, 1, 48, 1);
        assertEquals(statesOf("", "", "Num", "Num", "Num", "", ""), seen);
    }

    @Test
    public void brailleLowerCaseFormat() {
        setPref("pref_brl_fmt", true);
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        assertEquals("ab", decode(d, 1, 3));
    }

    @Test
    public void braillePunctuationViaInterp() {
        setPref("pref_brl_itp", true);
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        assertEquals("AB.", decode(d, 1, 3, 50));
    }

    @Test
    public void brailleSpaceWhenEnabled() {
        setPref("pref_brl_spc", true);
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        assertEquals("A\u00b7B", decode(d, 1, 0, 3));
    }

    @Test
    public void brailleSpaceWhenDisabled() {
        StatefulDecoder d = new StatefulDecoder(R.xml.braille_decoder);
        assertEquals("A?B", decode(d, 1, 0, 3));
    }
}