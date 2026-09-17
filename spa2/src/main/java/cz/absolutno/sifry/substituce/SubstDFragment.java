package cz.absolutno.sifry.substituce;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;

import cz.absolutno.sifry.App;
import cz.absolutno.sifry.R;
import cz.absolutno.sifry.Utils;
import cz.absolutno.sifry.common.activity.AbstractDFragment;
import cz.absolutno.sifry.common.alphabet.Alphabet;
import cz.absolutno.sifry.common.widget.FixedGridLayout;
import cz.absolutno.sifry.substituce.analysis.KeyWheelView;
import cz.absolutno.sifry.substituce.analysis.VigenereConvention;
import cz.absolutno.sifry.substituce.analysis.VigenereWorkbench;

public final class SubstDFragment extends AbstractDFragment {

    // Keys under which the Kasiski workbench persists its extra state in the
    // shared save bundle. They are private to this fragment because nothing else
    // needs to understand them.
    private static final String KAS_LEN = "kasiski.len";
    private static final String KAS_CONV = "kasiski.conv";
    private static final String KAS_LETTERS = "kasiski.letters";
    private static final String KAS_LOCKS = "kasiski.locks";
    private static final String KAS_SELECTED = "kasiski.selected";

    private int[] groupIDs;
    private AbstractSubstAdapter adapter;
    private Alphabet abc;
    private String patKoef;
    private int[] savedTr = null;
    private Bundle restored = null;

    /** Non-null only while the Kasiski analysis subtype is showing. */
    private VigenereWorkbench workbench;
    private KeyWheelView keyWheel;

    /**
     * Set while programmatically moving the convention spinner, so the resulting
     * item-selected callback does not immediately rebuild the workbench again.
     */
    private boolean suppressKasiskiEvents;

    @Override
    protected int getMenuCaps() {
        return HAS_COPY | HAS_PASTE | HAS_CLEAR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.subst_layout, container, false);
        ((Spinner) v.findViewById(R.id.spSDTyp)).setOnItemSelectedListener(itemSelectedListener);
        ((Spinner) v.findViewById(R.id.spSDAKoef)).setOnItemSelectedListener(itemSelectedListener);
        ((Spinner) v.findViewById(R.id.spSDKPokr)).setOnItemSelectedListener(itemSelectedListener);
        ((TextView) v.findViewById(R.id.etSDSifra)).setOnEditorActionListener(editorActionListener);
        ((TextView) v.findViewById(R.id.etSDKlic)).setOnEditorActionListener(editorActionListener);
        ((TextView) v.findViewById(R.id.etSDHeslo)).setOnEditorActionListener(editorActionListener);
        ((ListView) v.findViewById(R.id.lvSDRes)).setOnItemClickListener(Utils.copyItemClickListener);
        v.findViewById(R.id.ivGo).setOnClickListener(goListener);
        v.findViewById(R.id.etSDSifra).setOnTouchListener(interceptListener);

        groupIDs = Utils.getIdArray(R.array.iaSDTypy);
        patKoef = getString(R.string.patSDKoef);

        // The key-wheel and its two inputs exist in the layout for every subtype
        // (they are hidden unless the Kasiski entry is chosen), so they can be
        // wired once here.
        keyWheel = v.findViewById(R.id.kvSDKasiski);
        keyWheel.setOnWorkbenchChanged(kasiskiChangedListener);
        ((Spinner) v.findViewById(R.id.spSDKasiskiKonv)).setOnItemSelectedListener(itemSelectedListener);
        ((TextView) v.findViewById(R.id.etSDKasiskiDelka)).setOnEditorActionListener(editorActionListener);

        if (savedInstanceState != null)
            savedTr = savedInstanceState.getIntArray(App.DATA);

        return v;
    }

    private final OnTouchListener interceptListener = new OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        public boolean onTouch(View v, MotionEvent event) {
            v.getParent().requestDisallowInterceptTouchEvent(event.getActionMasked() != MotionEvent.ACTION_UP);
            return false;
        }
    };

    private final OnItemSelectedListener itemSelectedListener = new OnItemSelectedListener() {
        @SuppressWarnings("ConstantConditions")
        public void onItemSelected(AdapterView<?> parentView, View childView, int position, long id) {
            int id1 = parentView.getId();
            if(id1 == R.id.spSDTyp)
                    updateLayout();
            else if(id1 == R.id.spSDAKoef) {
                if (adapter instanceof AfinniAdapter)
                    ((AfinniAdapter) adapter).setCoeff(((KoefItem) parentView.getItemAtPosition(position)).koef);
            } else if(id1 == R.id.spSDKPokr) {
                if (adapter instanceof KlicAdapter)
                    ((KlicAdapter) adapter).setKlic(((EditText) getView().findViewById(R.id.etSDKlic)).getText().toString(), position);
            } else if (id1 == R.id.spSDKasiskiKonv) {
                // Changing the convention keeps the key letters (they are
                // ordinals) and only re-derives the plaintext.
                if (!suppressKasiskiEvents && workbench != null)
                    applyWorkbench(workbench.withConvention(conventionAt(position)));
            }
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    };

    private final OnEditorActionListener editorActionListener = new OnEditorActionListener() {
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            zpracuj();
            return true;
        }
    };

    private final OnClickListener goListener = new OnClickListener() {
        public void onClick(View v) {
            zpracuj();
        }
    };

    @SuppressWarnings("ConstantConditions")
    private void zpracuj() {
        if (adapter == null) {
            // Kasiski subtype: (re)build the workbench from the current inputs.
            buildWorkbench();
            return;
        }
        if (adapter != null)
            adapter.setInput(((TextView) getView().findViewById(R.id.etSDSifra)).getText().toString());
        if (adapter instanceof HesloAdapter)
            ((HesloAdapter) adapter).setKey(((TextView) getView().findViewById(R.id.etSDHeslo)).getText().toString());
        else if (adapter instanceof KlicAdapter)
            ((KlicAdapter) adapter).setKlic(((TextView) getView().findViewById(R.id.etSDKlic)).getText().toString(),
                    ((Spinner) getView().findViewById(R.id.spSDKPokr)).getSelectedItemPosition());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected String onCopy() {
        return ((EditText) getView().findViewById(R.id.etSDSifra)).getText().toString();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onPaste(String s) {
        ((EditText) getView().findViewById(R.id.etSDSifra)).setText(s);
        if (adapter != null)
            adapter.setInput(s);
        else
            buildWorkbench();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onClear() {
        ((EditText) getView().findViewById(R.id.etSDSifra)).setText("");
        ((EditText) getView().findViewById(R.id.etSDHeslo)).setText("");
        ((EditText) getView().findViewById(R.id.etSDKlic)).setText("");
        if (adapter != null) {
            adapter.clear();
            if (adapter instanceof TranslateAdapter)
                updateFGL();
        }
        // Drop the interactive workbench entirely; updateLayout will build a new
        // empty one if the Kasiski subtype is still showing.
        workbench = null;
        if (keyWheel != null)
            keyWheel.setWorkbench(null);
        ((TextView) getView().findViewById(R.id.tvSDKasiskiPlain)).setText("");
    }

    @SuppressWarnings("ConstantConditions")
    private void loadAbc() {
        abc = Alphabet.getPreferentialInstance();
        int cnt = abc.count();

        ArrayList<KoefItem> entries = new ArrayList<>();
        for (int i = 2; i < cnt - 1; i++) {
            if (gcd(i, cnt) != 1) continue;
            int j;
            for (j = 1; j < cnt; j++)
                if (i * j % cnt == 1) break;
            entries.add(new KoefItem(i, j));
        }
        ArrayAdapter<KoefItem> coeffAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, entries);
        coeffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ((Spinner) getView().findViewById(R.id.spSDAKoef)).setAdapter(coeffAdapter);

        LayoutInflater inflater = App.getInflater();
        FixedGridLayout fgl = getView().findViewById(R.id.fglSDVlastni);
        fgl.removeAllViews();
        for (int i = 0; i < cnt; i++) {
            View v = inflater.inflate(R.layout.subst_item, fgl, false);
            ((TextView) v.findViewById(R.id.puv)).setText(abc.chr(i));
            ((TextView) v.findViewById(R.id.nove)).setText(abc.chr(i));
            v.setTag(i);
            v.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!(adapter instanceof TranslateAdapter))
                        return;
                    int from = (Integer) v.getTag();
                    getView().findViewById(R.id.lvSDRes).requestFocus();
                    setSubs(from);
                }
            });
            fgl.addView(v);
        }
    }

    // ---- Interactive key analysis (Vigenère / Kasiski) ----------------------
    // The methods below are the thin Android glue around the pure-JVM
    // VigenereWorkbench: they build the controller from the screen inputs, render
    // its plaintext and persist it. All cipher logic stays in the analysis package.

    /** Re-renders the plaintext preview whenever the wheel changes the key. */
    private final KeyWheelView.OnWorkbenchChanged kasiskiChangedListener = new KeyWheelView.OnWorkbenchChanged() {
        public void onWorkbenchChanged(VigenereWorkbench wb) {
            ((TextView) getView().findViewById(R.id.tvSDKasiskiPlain)).setText(wb.getPlaintext());
        }
    };

    /** Builds a fresh workbench from the current ciphertext and inputs. */
    @SuppressWarnings("ConstantConditions")
    private void buildWorkbench() {
        workbench = new VigenereWorkbench(
                ((EditText) getView().findViewById(R.id.etSDSifra)).getText().toString(),
                conventionAt(((Spinner) getView().findViewById(R.id.spSDKasiskiKonv)).getSelectedItemPosition()),
                currentKeyLength());
        applyWorkbench(workbench);
    }

    /** Pushes a workbench into the wheel and refreshes the plaintext preview. */
    @SuppressWarnings("ConstantConditions")
    private void applyWorkbench(VigenereWorkbench wb) {
        workbench = wb;
        keyWheel.setWorkbench(wb);
        ((TextView) getView().findViewById(R.id.tvSDKasiskiPlain)).setText(wb.getPlaintext());
    }

    /** Reads the requested key length, defaulting to 1 and clamping to 1..64. */
    @SuppressWarnings("ConstantConditions")
    private int currentKeyLength() {
        int len;
        try {
            len = Integer.parseInt(((EditText) getView().findViewById(R.id.etSDKasiskiDelka)).getText().toString().trim());
        } catch (NumberFormatException e) {
            len = 1;
        }
        return Math.max(1, Math.min(64, len));
    }

    /** Maps the convention spinner position onto the enum. */
    private static VigenereConvention conventionAt(int position) {
        VigenereConvention[] values = VigenereConvention.values();
        if (position < 0 || position >= values.length)
            return VigenereConvention.APLUSB0;
        return values[position];
    }

    /** Encodes the key letters for persistence, using '-' for a blank slot. */
    private static String keyLetters(VigenereWorkbench wb) {
        StringBuilder sb = new StringBuilder(wb.getKeyLength());
        for (int i = 0; i < wb.getKeyLength(); i++) {
            Character c = wb.getSlot(i).getKeyLetter();
            sb.append(c != null ? c : '-');
        }
        return sb.toString();
    }

    /** Re-applies a previously saved workbench on top of the freshly built one. */
    @SuppressWarnings("ConstantConditions")
    private void restoreWorkbench(Bundle d) {
        if (workbench == null)
            return;
        int conv = d.getInt(KAS_CONV, workbench.getConvention().ordinal());
        suppressKasiskiEvents = true;
        ((Spinner) getView().findViewById(R.id.spSDKasiskiKonv)).setSelection(conv);
        suppressKasiskiEvents = false;
        VigenereWorkbench wb = workbench.withConvention(conventionAt(conv));
        String letters = d.getString(KAS_LETTERS);
        for (int i = 0; letters != null && i < letters.length() && i < wb.getKeyLength(); i++) {
            char c = letters.charAt(i);
            if (c >= 'A' && c <= 'Z')
                wb.setSlotLetter(i, c);
        }
        boolean[] locks = d.getBooleanArray(KAS_LOCKS);
        for (int i = 0; locks != null && i < locks.length && i < wb.getKeyLength(); i++)
            if (locks[i])
                wb.toggleLock(i);
        wb.select(Math.max(0, Math.min(wb.getKeyLength() - 1, d.getInt(KAS_SELECTED, 0))));
        applyWorkbench(wb);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateLayout() {
        String str = ((EditText) getView().findViewById(R.id.etSDSifra)).getText().toString();
        int selItem = groupIDs[((Spinner) getView().findViewById(R.id.spSDTyp)).getSelectedItemPosition()];

        getView().findViewById(R.id.llSDHeslo).setVisibility(selItem == R.id.idSDHeslo ? View.VISIBLE : View.GONE);
        getView().findViewById(R.id.llSDAfinni).setVisibility(selItem == R.id.idSDAffini ? View.VISIBLE : View.GONE);
        getView().findViewById(R.id.llSDKlic).setVisibility(selItem == R.id.idSDKlic ? View.VISIBLE : View.GONE);
        getView().findViewById(R.id.llSDVlastni).setVisibility(selItem == R.id.idSDVlastni ? View.VISIBLE : View.GONE);

        // The interactive key analysis replaces the result list with its own
        // key-wheel and plaintext preview, so it is handled before the adapter
        // dispatch below (which does not apply to it).
        boolean kasiski = selItem == R.id.idSDKasiski;
        getView().findViewById(R.id.llSDKasiski).setVisibility(kasiski ? View.VISIBLE : View.GONE);
        getView().findViewById(R.id.lvSDRes).setVisibility(kasiski ? View.GONE : View.VISIBLE);
        getView().findViewById(R.id.tvSDResLabel).setVisibility(kasiski ? View.GONE : View.VISIBLE);
        if (kasiski) {
            adapter = null;
            if (workbench == null)
                buildWorkbench();
            else
                applyWorkbench(workbench);
            return;
        }
        workbench = null;

        if (adapter instanceof TranslateAdapter)
            savedTr = ((TranslateAdapter) adapter).getTr();

        if (selItem == R.id.idSDHeslo) {
            adapter = new HesloAdapter(abc);
            ((HesloAdapter) adapter).setKey(((EditText) getView().findViewById(R.id.etSDHeslo)).getText().toString());
        } else if (selItem == R.id.idSDAutoKey)
            adapter = new AutokeyAdapter(abc);
        else if (selItem == R.id.idSDPozice)
            adapter = new PoziceAdapter(abc);
        else if (selItem == R.id.idSDCaesar)
            adapter = new PosunyAdapter(abc);
        else if (selItem == R.id.idSDAtbash)
            adapter = new AtbashAdapter(abc);
        else if (selItem == R.id.idSDAffini) {
            adapter = new AfinniAdapter(abc);
            ((AfinniAdapter) adapter).setCoeff(((KoefItem) ((Spinner) getView().findViewById(R.id.spSDAKoef)).getSelectedItem()).koef);
        } else if (selItem == R.id.idSDKlic) {
            adapter = new KlicAdapter(abc);
            ((KlicAdapter) adapter).setKlic(
                    ((EditText) getView().findViewById(R.id.etSDKlic)).getText()
                            .toString(),
                    ((Spinner) getView().findViewById(R.id.spSDKPokr))
                            .getSelectedItemPosition());
        } else if (selItem == R.id.idSDVlastni) {
            adapter = new TranslateAdapter(abc);
            if (savedTr != null)
                ((TranslateAdapter) adapter).setTr(savedTr);
            updateFGL();
        } else
            throw new IllegalArgumentException();
        ((ListView) getView().findViewById(R.id.lvSDRes)).setAdapter(adapter);
        adapter.setInput(str);
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private void setSubs(int from) {
        TranslateFragment dialog = new TranslateFragment();
        Bundle args = new Bundle();
        args.putSerializable(App.VSTUP1, from);
        args.putSerializable(App.VSTUP2,
                ((TranslateAdapter) adapter).getOne(from));
        dialog.setArguments(args);
        dialog.setOnItemClickListener(setSelectedListener);
        dialog.show(getFragmentManager(), "setSubs");
    }

    private final TranslateFragment.OnSelectedListener setSelectedListener = new TranslateFragment.OnSelectedListener() {
        @SuppressLint("DefaultLocale")
        public void onSelected(int from, int to) {
            ((TranslateAdapter) adapter).setOne(from, to);
            updateFGL();
        }
    };

    @SuppressWarnings("ConstantConditions")
    private void updateFGL() {
        FixedGridLayout fgl = getView().findViewById(R.id.fglSDVlastni);
        savedTr = ((TranslateAdapter) adapter).getTr();
        int cnt = abc.count();
        for (int i = 0; i < cnt; i++) {
            ((TextView) fgl.getChildAt(i).findViewById(R.id.nove)).setText(abc.chr(savedTr[i]));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (abc == null)
            loadAbc();
        updateLayout();
        applyRestored();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean saveData(Bundle data) {
        String sifra = ((EditText) getView().findViewById(R.id.etSDSifra)).getText().toString();
        if (sifra.length() == 0)
            return false;
        int selGroup = groupIDs[((Spinner) getView().findViewById(R.id.spSDTyp)).getSelectedItemPosition()];
        data.putInt(App.SPEC, selGroup);
        data.putString(App.VSTUP, sifra);
        data.putString(App.VSTUP1, ((EditText) getView().findViewById(R.id.etSDHeslo)).getText().toString());
        data.putString(App.VSTUP2, ((EditText) getView().findViewById(R.id.etSDKlic)).getText().toString());
        if (adapter instanceof TranslateAdapter)
            data.putIntArray(App.DATA, ((TranslateAdapter) adapter).getTr());
        if (selGroup == R.id.idSDKasiski && workbench != null) {
            data.putInt(KAS_LEN, workbench.getKeyLength());
            data.putInt(KAS_CONV, workbench.getConvention().ordinal());
            data.putString(KAS_LETTERS, keyLetters(workbench));
            boolean[] locks = new boolean[workbench.getKeyLength()];
            for (int i = 0; i < locks.length; i++)
                locks[i] = workbench.getSlot(i).isLocked();
            data.putBooleanArray(KAS_LOCKS, locks);
            data.putInt(KAS_SELECTED, workbench.getSelectedSlot());
        }
        return true;
    }

    @Override
    public void loadData(Bundle data) {
        restored = data;
        if (getView() != null && abc != null)
            getView().post(new Runnable() {
                @Override
                public void run() {
                    applyRestored();
                }
            });
    }

    @SuppressWarnings("ConstantConditions")
    private void applyRestored() {
        if (restored == null)
            return;
        final Bundle d = restored;
        restored = null;
        ((EditText) getView().findViewById(R.id.etSDSifra)).setText(d.getString(App.VSTUP));
        ((EditText) getView().findViewById(R.id.etSDHeslo)).setText(d.getString(App.VSTUP1));
        ((EditText) getView().findViewById(R.id.etSDKlic)).setText(d.getString(App.VSTUP2));
        int[] tr = d.getIntArray(App.DATA);
        if (tr != null)
            savedTr = tr;
        Spinner spTyp = getView().findViewById(R.id.spSDTyp);
        int selGroup = d.getInt(App.SPEC, groupIDs[0]);
        for (int i = 0; i < groupIDs.length; i++) {
            if (groupIDs[i] == selGroup) {
                spTyp.setSelection(i);
                break;
            }
        }
        // Restore the key length before updateLayout builds the workbench, then
        // overlay the saved letters/locks/selection on top of the fresh one.
        if (selGroup == R.id.idSDKasiski) {
            int klen = d.getInt(KAS_LEN, 0);
            if (klen > 0)
                ((EditText) getView().findViewById(R.id.etSDKasiskiDelka)).setText(String.valueOf(klen));
        }
        updateLayout();
        if (selGroup == R.id.idSDKasiski)
            restoreWorkbench(d);
    }

    @Override
    protected void onPreferencesChanged() {
        super.onPreferencesChanged();
        loadAbc();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        TranslateFragment fragment;
        fragment = (TranslateFragment) getFragmentManager().findFragmentByTag("setSubs");
        if (fragment != null)
            fragment.setOnItemClickListener(setSelectedListener);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putIntArray(App.DATA, savedTr);
    }

    private final class KoefItem {
        final int koef;
        final int inv;

        KoefItem(int koef, int inv) {
            this.koef = koef;
            this.inv = inv;
        }

        @Override
        public String toString() {
            return String.format(patKoef, koef, inv);
        }
    }

}
