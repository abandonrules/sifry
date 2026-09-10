package cz.absolutno.sifry.regexp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

import cz.absolutno.sifry.R;
import cz.absolutno.sifry.Utils;
import cz.absolutno.sifry.common.activity.AbstractDFragment;
import cz.absolutno.sifry.regexp.FilterRule.Kind;
import cz.absolutno.sifry.regexp.FilterRule.Op;
import cz.absolutno.sifry.regexp.RegExpNative.Report;

public final class RegExpDFragment extends AbstractDFragment {

    private static final int progressDelay = 30;
    private static final int matchesDelay = 100;

    private boolean spinnerGuard = false;

    private RegExpNative re;
    private TextView tvProgress;
    private ProgressBar pbProgress;
    private RegExpExpListAdapter adapter;

    @Override
    protected int getMenuCaps() {
        return HAS_CLEAR | HAS_REFERENCE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.regexp_layout, container, false);
        tvProgress = v.findViewById(R.id.tvRDPocet);
        pbProgress = v.findViewById(R.id.pbRDProgress);
        ((ExpandableListView) v.findViewById(R.id.elRDResults)).setOnChildClickListener(Utils.copyChildClickListener);
        v.findViewById(R.id.btRDGo).setOnClickListener(goListener);
        for (int i = 0; i < 3; i++)
            initRow(i);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvProgress = null;
        pbProgress = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        for (int i = 0; i < 3; i++)
            initRow(i);
        re = ((ReferenceFragment) getFragmentManager().findFragmentByTag("ref")).getRE();
        adapter = new RegExpExpListAdapter(re);
        ((ExpandableListView) getView().findViewById(R.id.elRDResults)).setAdapter(adapter);
        if (re.isRunning())
            launchRefresh();
        final Report report = re.getProgress();
        if (report.matches > 0) {
            int matches = Math.min(report.matches, RegExpNative.MaxListResults);
            tvProgress.setText(String.valueOf(report.matches));
            adapter.update(matches);
        }
    }

    @Override
    protected void onPreferencesChanged() {
        for (int i = 0; i < 3; i++)
            initRow(i);
    }

    @Override
    protected void onOpenReference() {
        if (getActivity() == null)
            return;
        Intent i = new Intent(getActivity(), RegExpReferenceActivity.class);
        i.putExtra(RegExpReferenceActivity.EXTRA_FILENAME, sourceFilename());
        startActivity(i);
    }

    private void initRow(final int i) {
        if (getView() == null)
            return;
        final Spinner sp = (Spinner) getView().findViewById(idSP[i]);
        final Spinner op = (Spinner) getView().findViewById(idOP[i]);
        spinnerGuard = true;
        ArrayAdapter<Kind> ka = kindAdapter(kinds());
        sp.setAdapter(ka);
        ArrayAdapter<CharSequence> oa = new ArrayAdapter<CharSequence>(getActivity(),
                android.R.layout.simple_spinner_item, opLabels());
        oa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        op.setAdapter(oa);
        sp.setSelection(0);
        op.setSelection(0);
        spinnerGuard = false;
        updateRow(i);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!spinnerGuard)
                    updateRow(i);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        op.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!spinnerGuard)
                    updateRow(i);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private ArrayAdapter<Kind> kindAdapter(final List<Kind> kinds) {
        final String[] allKinds = getResources().getStringArray(R.array.saRDKindLabels);
        return new ArrayAdapter<Kind>(getActivity(), android.R.layout.simple_spinner_item, kinds) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                return labelView(pos, convertView, parent);
            }

            @Override
            public View getDropDownView(int pos, View convertView, ViewGroup parent) {
                return labelView(pos, convertView, parent);
            }

            private View labelView(int pos, View convertView, ViewGroup parent) {
                TextView tv;
                if (convertView != null)
                    tv = (TextView) convertView;
                else {
                    tv = new TextView(getActivity());
                    tv.setTextColor(getResources().getColor(android.R.color.white));
                }
                tv.setText(allKinds[((Kind) getItem(pos)).ordinal()]);
                return tv;
            }
        };
    }

    private List<Kind> kinds() {
        return FilterRule.kindsFor(sourceFilename());
    }

    @SuppressWarnings("ConstantConditions")
    private Kind kindAt(int i) {
        Spinner sp = (Spinner) getView().findViewById(idSP[i]);
        return (Kind) sp.getAdapter().getItem(sp.getSelectedItemPosition());
    }

    @SuppressWarnings("ConstantConditions")
    private Op opAt(int i) {
        Spinner op = (Spinner) getView().findViewById(idOP[i]);
        return Op.values()[op.getSelectedItemPosition()];
    }

    private String[] opLabels() {
        return getResources().getStringArray(R.array.saRDOpLabels);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateRow(int i) {
        Spinner op = (Spinner) getView().findViewById(idOP[i]);
        Kind kind = kindAt(i);
        boolean numeric = FilterRule.isNumeric(kind);
        op.setVisibility(numeric ? View.VISIBLE : View.GONE);
        Op o = opAt(i);
        boolean two = FilterRule.needsSecondValue(kind, o);
        getView().findViewById(idET2[i]).setVisibility(two ? View.VISIBLE : View.GONE);
    }

    private final OnClickListener goListener = new OnClickListener() {
        @SuppressWarnings("ConstantConditions")
        public void onClick(View v) {
            if (re.isRunning()) {
                re.stopThread();
                updateGoButton();
                return;
            }
            String zad[] = new String[3];
            for (int i = 0; i < 3; i++) {
                Kind kind = kindAt(i);
                Op o = opAt(i);
                String v1 = ((EditText) getView().findViewById(idET[i])).getText().toString();
                String v2 = ((EditText) getView().findViewById(idET2[i])).getText().toString();
                String pat = FilterRule.pattern(kind, o, v1, v2);
                if (pat == null)
                    zad[i] = "";
                else
                    zad[i] = (((ToggleButton) getView().findViewById(idCB[i])).isChecked() ? "" : "!") + pat;
            }
            adapter.clear();
            re.startThread(getContext().getAssets(), getFilename(), zad);
            launchRefresh();
        }
    };

    private String sourceFilename() {
        return getFilename().replaceFirst("^raw/", "");
    }

    private String getFilename() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String selected = "";
        if (sp != null)
            selected = sp.getString("pref_re_dictionary", "");
        String fallback = getString(R.string.pref_re_dictionary_default);
        String filename = (sp != null) ? DataSources.resolve(sp, selected, fallback)
                : (selected.isEmpty() ? fallback : selected);
        return "raw/" + filename;
    }

    @SuppressWarnings("ConstantConditions")
    private void updateGoButton() {
        ((Button) getView().findViewById(R.id.btRDGo)).setText(re.isRunning() ? R.string.tRDStop : R.string.tRDHledej);
    }

    private void launchRefresh() {
        final Handler h = new Handler();
        Runnable rMain = new Runnable() {
            public void run() {
                if (!isVisible()) {
                    h.postDelayed(this, matchesDelay);
                    return;
                }
                final Report report = re.getProgress();
                if (report.error) {
                    Utils.toast(re.getError());
                    tvProgress.setText("");
                    re.stopThread();
                    updateGoButton();
                    return;
                }
                int matches = Math.min(report.matches, RegExpNative.MaxListResults);
                if (report.running || matches > 0 || tvProgress.length() > 0)
                    tvProgress.setText(String.valueOf(report.matches));
                if (report.running)
                    h.postDelayed(this, matchesDelay);
                adapter.update(matches);
                updateGoButton();
            }
        };
        Runnable rProgress = new Runnable() {
            public void run() {
                if (!isVisible()) {
                    h.postDelayed(this, matchesDelay);
                    return;
                }
                final Report report = re.getProgress();
                pbProgress.setProgress((int) (report.progress * 1000));
                if (report.running)
                    h.postDelayed(this, progressDelay);
                else
                    pbProgress.setVisibility(View.GONE);
            }
        };
        h.postDelayed(rMain, matchesDelay);
        h.post(rProgress);
        pbProgress.setProgress(0);
        pbProgress.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onClear() {
        re.free();
        adapter.clear();
        for (int i = 0; i < 3; i++) {
            ((EditText) getView().findViewById(idET[i])).setText("");
            ((EditText) getView().findViewById(idET2[i])).setText("");
            ((ToggleButton) getView().findViewById(idCB[i])).setChecked(true);
        }
        tvProgress.setText("");
        updateGoButton();
    }

    private final int[] idET = {R.id.etRDFiltr1, R.id.etRDFiltr2, R.id.etRDFiltr3};
    private final int[] idET2 = {R.id.et2RDFiltr1, R.id.et2RDFiltr2, R.id.et2RDFiltr3};
    private final int[] idSP = {R.id.spRDFiltr1, R.id.spRDFiltr2, R.id.spRDFiltr3};
    private final int[] idOP = {R.id.opRDFiltr1, R.id.opRDFiltr2, R.id.opRDFiltr3};
    private final int[] idCB = {R.id.cbRDFiltr1, R.id.cbRDFiltr2, R.id.cbRDFiltr3};
}