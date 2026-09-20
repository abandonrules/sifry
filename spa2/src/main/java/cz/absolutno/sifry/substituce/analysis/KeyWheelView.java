package cz.absolutno.sifry.substituce.analysis;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.absolutno.sifry.R;

/**
 * Horizontal wheel of interactive key slots for the Vigenère / Kasiski analysis
 * screen (issue #37, implementation step 2).
 *
 * <p>There is one cell per key position. Each cell exposes an explicit
 * increase/decrease button so the interaction is reachable without gestures,
 * and additionally accepts:</p>
 *
 * <ul>
 *   <li>a single tap to select the cell,</li>
 *   <li>a vertical swipe to cycle the key letter,</li>
 *   <li>a long press to lock or unlock the cell.</li>
 * </ul>
 *
 * <p>The view is a pure renderer. It owns no cipher logic and never decides a
 * key itself: every interaction is forwarded to a {@link VigenereWorkbench} and
 * the resulting state is read straight back out for display. That keeps the
 * whole interaction model testable on the JVM and this class a thin skin over
 * it.</p>
 *
 * <p>Every state is exposed both visually (colour/shape) and through a
 * content description, so it is never colour-only (accessibility).</p>
 */
public final class KeyWheelView extends LinearLayout {

    /** Notified after any interaction so the host can re-render the plaintext. */
    public interface OnWorkbenchChanged {
        void onWorkbenchChanged(VigenereWorkbench workbench);
    }

    /*
     * Rendering colours. They live here rather than in resources because they
     * are presentation hints, not localisable text; the semantic state is also
     * spelled out in the content description instead of relying on colour.
     */
    private static final int COLOR_NEUTRAL = 0x00000000; // transparent
    private static final int COLOR_SELECTED = 0xFF1565C0; // blue frame
    private static final int COLOR_CHANGED = 0xFFFFF176;  // yellow fill
    private static final int COLOR_MATCHING = 0xFF66BB6A; // green fill
    private static final int COLOR_LOCKED = 0xFF9E9E9E;   // grey frame

    /** Vertical drag distance, in pixels, that advances the letter one step. */
    private static final int SWIPE_STEP_PX = 90;

    private VigenereWorkbench workbench;
    private OnWorkbenchChanged listener;

    /** One cell per key slot, rebuilt whenever the key length changes. */
    private final List<Cell> cells = new ArrayList<Cell>();

    public KeyWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
    }

    /** Attaches (or replaces) the workbench this wheel renders and edits. */
    public void setWorkbench(VigenereWorkbench workbench) {
        this.workbench = workbench;
        rebuild();
    }

    /** Registers the listener notified after every interaction. */
    public void setOnWorkbenchChanged(OnWorkbenchChanged listener) {
        this.listener = listener;
    }

    /**
     * Redraws every cell from the current workbench state. Safe to call at any
     * time; the host calls it after loading saved state.
     */
    public void refresh() {
        if (workbench == null) {
            removeAllViews();
            cells.clear();
            return;
        }
        if (cells.size() != workbench.getKeyLength()) {
            rebuild();
            return;
        }
        Map<Integer, List<Integer>> matching = workbench.matchingKeys();
        for (Cell cell : cells)
            cell.render(workbench, matching);
    }

    /** Rebuilds the cell views from scratch; used when the key length changes. */
    private void rebuild() {
        removeAllViews();
        cells.clear();
        if (workbench == null)
            return;
        for (int slot = 0; slot < workbench.getKeyLength(); slot++) {
            Cell cell = new Cell(slot);
            cells.add(cell);
            addView(cell.root, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }
        refresh();
    }

    /** Pushes a completed interaction out to the host. */
    private void notifyChanged() {
        if (listener != null && workbench != null)
            listener.onWorkbenchChanged(workbench);
    }

    /** Short cut for the haptic tick that accompanies every letter change. */
    private void haptic() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    /** Converts dp to pixels for the touch/hit sizes. */
    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * One key position: an increase button, the letter itself and a decrease
     * button, plus the gesture handling shared by all three. Held as an object
     * so {@link #refresh()} can update it without rebuilding the view tree.
     */
    private final class Cell {

        final int slot;
        final LinearLayout root;
        final TextView letter;
        final GestureDetector gestures;

        Cell(final int slot) {
            this.slot = slot;

            root = new LinearLayout(getContext());
            root.setOrientation(VERTICAL);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            int pad = dp(4);
            root.setPadding(pad, pad, pad, pad);
            root.setFocusable(true);

            Button up = new Button(getContext());
            up.setText("\u25B2"); // BLACK UP-POINTING TRIANGLE
            up.setContentDescription(getContext().getString(R.string.cdKasiskiUp, slot + 1));
            up.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    stepBy(+1);
                }
            });

            letter = new TextView(getContext());
            letter.setGravity(Gravity.CENTER);
            letter.setTextSize(24f);
            letter.setMinWidth(dp(48));
            letter.setMinHeight(dp(48));

            Button down = new Button(getContext());
            down.setText("\u25BC"); // BLACK DOWN-POINTING TRIANGLE
            down.setContentDescription(getContext().getString(R.string.cdKasiskiDown, slot + 1));
            down.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    stepBy(-1);
                }
            });

            root.addView(up, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            root.addView(letter, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            root.addView(down, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            // A tap selects; a long click locks. Both are also wired as regular
            // click listeners (not only through the gesture detector) so they can
            // be triggered by accessibility services and by tests.
            root.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectSelf();
                }
            });
            root.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    toggleLockSelf();
                    return true;
                }
            });

            // Swiping is handled by the detector, which forwards to the same
            // three operations.
            gestures = new GestureDetector(getContext(), new SwipeListener(this));
            root.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    boolean handled = gestures.onTouchEvent(event);
                    if (event.getActionMasked() == MotionEvent.ACTION_UP)
                        v.performClick();
                    return handled;
                }
            });
        }

        /** Cycles this slot's letter by {@code delta} and republishes the state. */
        void stepBy(int delta) {
            workbench.step(slot, delta);
            haptic();
            refresh();
            notifyChanged();
        }

        /** Makes this slot the selected one. */
        void selectSelf() {
            workbench.select(slot);
            refresh();
            notifyChanged();
        }

        /** Toggles this slot's lock and republishes the state. */
        void toggleLockSelf() {
            workbench.toggleLock(slot);
            haptic();
            refresh();
            notifyChanged();
        }

        /** Redraws this cell from the given workbench snapshot. */
        void render(VigenereWorkbench wb, Map<Integer, List<Integer>> matching) {
            KeySlotState state = wb.getSlot(slot);
            Character key = state.getKeyLetter();

            // The letter, or a dash placeholder for an unassigned slot.
            letter.setText(key != null ? String.valueOf(key) : "\u2013");

            // Background priority: changed (user set it) beats matching, which
            // beats neutral. Locked and selected are drawn as frames so they
            // stack on top of the fill instead of replacing it.
            int fill = COLOR_NEUTRAL;
            if (wb.isChanged(slot))
                fill = COLOR_CHANGED;
            else if (matching.containsKey(slot))
                fill = COLOR_MATCHING;
            letter.setBackgroundColor(fill);

            int frame = COLOR_NEUTRAL;
            if (state.isLocked())
                frame = COLOR_LOCKED;
            if (wb.getSelectedSlot() == slot)
                frame = COLOR_SELECTED;
            root.setBackgroundColor(frame);

            // Spell the state out for screen readers: never rely on colour alone.
            List<String> states = new ArrayList<String>();
            if (key == null)
                states.add(getContext().getString(R.string.cdKasiskiStateEmpty));
            if (wb.isChanged(slot))
                states.add(getContext().getString(R.string.cdKasiskiStateChanged));
            if (state.isLocked())
                states.add(getContext().getString(R.string.cdKasiskiStateLocked));
            if (wb.getSelectedSlot() == slot)
                states.add(getContext().getString(R.string.cdKasiskiStateSelected));
            if (matching.containsKey(slot))
                states.add(getContext().getString(R.string.cdKasiskiStateMatching));
            StringBuilder desc = new StringBuilder();
            for (String s : states) {
                if (desc.length() > 0)
                    desc.append(", ");
                desc.append(s);
            }
            root.setContentDescription(getContext().getString(
                    R.string.patKasiskiSlot, slot + 1, key != null ? String.valueOf(key) : "\u2013", desc));
        }
    }

    /**
     * Turns vertical drags on a cell into letter steps and a long press into a
     * lock toggle. The distance is accumulated so that pulling far enough moves
     * through several letters, exactly like dragging a physical wheel.
     */
    private static final class SwipeListener extends GestureDetector.SimpleOnGestureListener {

        private final Cell cell;
        private int accumulated;

        SwipeListener(Cell cell) {
            this.cell = cell;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            accumulated = 0;
            return true; // required for the detector to report the other events
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Screen Y grows downwards, so an upward swipe has a negative
            // distanceY; map "up" to the next letter, "down" to the previous.
            accumulated -= (int) distanceY;
            while (accumulated <= -SWIPE_STEP_PX) {
                accumulated += SWIPE_STEP_PX;
                cell.stepBy(-1);
            }
            while (accumulated >= SWIPE_STEP_PX) {
                accumulated -= SWIPE_STEP_PX;
                cell.stepBy(+1);
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            cell.selectSelf();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            cell.toggleLockSelf();
        }
    }
}
