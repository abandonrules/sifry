package cz.absolutno.sifry.substituce.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Robolectric coverage of the key-wheel's interaction contract: the view must
 * forward taps, button presses and long presses to the workbench and redraw
 * itself from the result. The cipher maths itself is covered by
 * {@link VigenereWorkbenchTest}; here we only care that the Android skin is
 * wired correctly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class KeyWheelViewTest {

    private static final String CT = "LXFOPVEFRNHR";

    private VigenereWorkbench workbench;

    private KeyWheelView wheel(int slots) {
        workbench = new VigenereWorkbench(CT, VigenereConvention.APLUSB0, slots);
        KeyWheelView v = new KeyWheelView(RuntimeEnvironment.getApplication(), null);
        v.setWorkbench(workbench);
        return v;
    }

    private static LinearLayout cell(View wheel, int slot) {
        return (LinearLayout) ((KeyWheelView) wheel).getChildAt(slot);
    }

    private static TextView letter(View wheel, int slot) {
        return (TextView) cell(wheel, slot).getChildAt(1);
    }

    @Test
    public void rendersOneCellPerKeySlot() {
        assertEquals(3, wheel(3).getChildCount());
    }

    @Test
    public void tapSelectsSlotWithoutChangingItsLetter() {
        KeyWheelView v = wheel(3);
        cell(v, 2).performClick();
        assertEquals(2, workbench.getSelectedSlot());
        assertEquals("\u2013", letter(v, 2).getText().toString());
    }

    @Test
    public void upButtonRevealsAThenStepsUp() {
        KeyWheelView v = wheel(3);
        Button up = (Button) cell(v, 1).getChildAt(0);
        up.performClick();
        assertEquals("A", letter(v, 1).getText().toString());
        up.performClick();
        assertEquals("B", letter(v, 1).getText().toString());
    }

    @Test
    public void downButtonFromBlankRevealsZ() {
        KeyWheelView v = wheel(3);
        Button down = (Button) cell(v, 0).getChildAt(2);
        down.performClick();
        assertEquals("Z", letter(v, 0).getText().toString());
    }

    @Test
    public void longPressTogglesLock() {
        KeyWheelView v = wheel(3);
        cell(v, 1).performLongClick();
        assertTrue(workbench.getSlot(1).isLocked());
        assertTrue(cell(v, 1).getContentDescription().toString().contains("locked"));
        cell(v, 1).performLongClick();
        assertFalse(workbench.getSlot(1).isLocked());
    }

    @Test
    public void selectionAndChangeAreSpelledOutForAccessibility() {
        KeyWheelView v = wheel(3);
        cell(v, 2).performClick();
        assertTrue(cell(v, 2).getContentDescription().toString().contains("selected"));
        ((Button) cell(v, 2).getChildAt(0)).performClick();
        assertTrue(cell(v, 2).getContentDescription().toString().contains("changed"));
    }

    @Test
    public void everyInteractionNotifiesTheHost() {
        final AtomicBoolean notified = new AtomicBoolean(false);
        KeyWheelView v = wheel(2);
        v.setOnWorkbenchChanged(new KeyWheelView.OnWorkbenchChanged() {
            @Override
            public void onWorkbenchChanged(VigenereWorkbench wb) {
                notified.set(true);
            }
        });
        ((Button) cell(v, 0).getChildAt(0)).performClick();
        assertTrue(notified.get());
    }
}
