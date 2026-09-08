package cz.absolutno.sifry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;

import cz.absolutno.sifry.morse.MorseDecoder;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class InfraSmokeTest {

    @Test
    public void appInstanceIsInitialized() {
        assertNotNull(App.getContext());
        assertSame(App.getContext(), RuntimeEnvironment.getApplication());
    }

    @Test
    public void morseDecodesFromResources() {
        MorseDecoder md = new MorseDecoder();
        assertEquals("A", md.decode(5));
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(5, 24));
        assertEquals("AB", md.decode(list));
    }
}