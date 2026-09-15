package cz.absolutno.sifry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.TableLayout;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import cz.absolutno.sifry.braille.BrailleActivity;
import cz.absolutno.sifry.cisla.CislaActivity;
import cz.absolutno.sifry.frekvence.FrekvActivity;
import cz.absolutno.sifry.kalendar.KalendarActivity;
import cz.absolutno.sifry.lookup.LookupPackActivity;
import cz.absolutno.sifry.mainscreen.MainActivity;
import cz.absolutno.sifry.mainscreen.SplashActivity;
import cz.absolutno.sifry.morse.MorseActivity;
import cz.absolutno.sifry.regexp.RegExpActivity;
import cz.absolutno.sifry.semafor.SemaforActivity;
import cz.absolutno.sifry.substituce.SubstActivity;
import cz.absolutno.sifry.tabulky.TabulkyActivity;
import cz.absolutno.sifry.transpozice.TransActivity;
import cz.absolutno.sifry.vlajky.VlajkyActivity;
import cz.absolutno.sifry.zapisnik.ZapisnikActivity;

@RunWith(AndroidJUnit4.class)
public class AppSmokeTest {

    private static final Class<?>[] CIPHER_ACTIVITIES = {
        MorseActivity.class,
        BrailleActivity.class,
        CislaActivity.class,
        SemaforActivity.class,
        TabulkyActivity.class,
        VlajkyActivity.class,
        SubstActivity.class,
        TransActivity.class,
        FrekvActivity.class,
        KalendarActivity.class,
        ZapisnikActivity.class,
        RegExpActivity.class,
    };

    @Test
    public void splashToMain() throws Exception {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        Context ctx = ApplicationProvider.getApplicationContext();
        Instrumentation.ActivityMonitor monitor =
                inst.addMonitor(MainActivity.class.getName(), null, false);
        try {
            Intent splashIntent = new Intent(ctx, SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity splash = inst.startActivitySync(splashIntent);
            assertEquals("SplashActivity did not resume", "mainscreen.SplashActivity",
                    splash.getLocalClassName());
            Activity main = inst.waitForMonitorWithTimeout(monitor, 15000);
            assertNotNull("MainActivity did not open after SplashActivity", main);
            main.finish();
            splash.finish();
        } finally {
            inst.removeMonitor(monitor);
        }
    }

    @Test
    public void eachCipherActivityRenders() {
        Context ctx = ApplicationProvider.getApplicationContext();
        for (Class<?> cls : CIPHER_ACTIVITIES) {
            try (ActivityScenario<?> scenario = ActivityScenario.launch(new Intent(ctx, cls))) {
                scenario.moveToState(Lifecycle.State.RESUMED);
                assertEquals(cls.getSimpleName() + " did not reach RESUMED",
                        Lifecycle.State.RESUMED, scenario.getState());
            }
        }
    }

    @Test
    public void lookupPacksRenders() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<?> scenario =
                     ActivityScenario.launch(new Intent(ctx, LookupPackActivity.class))) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals("LookupPackActivity did not reach RESUMED",
                    Lifecycle.State.RESUMED, scenario.getState());
            inst.waitForIdleSync();
            final Throwable[] failure = {null};
            scenario.onActivity(activity -> {
                try {
                    Button go = activity.findViewById(R.id.btLUGo);
                    assertNotNull("Go button missing", go);
                    go.performClick();
                    TableLayout table = activity.findViewById(R.id.tlLUResults);
                    assertNotNull("Results table missing", table);
                    assertTrue("blank query did not render a data table (rows="
                            + table.getChildCount() + ')', table.getChildCount() > 1);
                } catch (Throwable t) {
                    failure[0] = t;
                }
            });
            if (failure[0] != null)
                throw new AssertionError("lookup table render failed", failure[0]);
        }
    }
}