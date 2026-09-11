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

import java.util.ArrayList;
import java.util.Collections;
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
    private int currentMaxResults = RegExpNative.MaxListResults;
    private final List<View> filterRows = new ArrayList<View>();

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
        v.findViewById(R.id.btRDAdd).setOnClickListener(addListener);
        v.findViewById(R.id.btRDShowAll).setOnClickListener(showAllListener);
        filterRows.clear();
        for (int i = 0; i < 3; i++) {
            filterRows.add(buildRow(v));
            bindRow(filterRows.get(filterRows.size() - 1));
        }
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvProgress = null;
        pbProgress = null;
        filterRows.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        rebuildRows();
        re = ((ReferenceFragment) getFragmentManager().findFragmentByTag("ref")).getRE();
        adapter = new RegExpExpListAdapter(re);
        ((ExpandableListView) getView().findViewById(R.id.elRDResults)).setAdapter(adapter);
        if (re.isRunning())
            launchRefresh();
        final Report report = re.getProgress();
        if (report.matches > 0) {
            int matches = Math.min(report.matches, currentMaxResults);
            tvProgress.setText(String.valueOf(report.matches));
            adapter.update(matches);
        }
    }

    @Override
    protected void onPreferencesChanged() {
        rebuildRows();
    }

    @Override
    protected void onOpenReference() {
        if (getActivity() == null)
            return;
        Intent i = new Intent(getActivity(), RegExpReferenceActivity.class);
        i.putExtra(RegExpReferenceActivity.EXTRA_FILENAME, sourceFilename());
        startActivity(i);
    }

    private void rebuildRows() {
        if (getView() == null)
            return;
        if (filterRows.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                filterRows.add(buildRow(getView()));
                bindRow(filterRows.get(filterRows.size() - 1));
            }
        } else {
            for (View row : filterRows)
                bindRow(row);
        }
    }

    private View buildRow(View root) {
        return getActivity().getLayoutInflater()
                .inflate(R.layout.filter_row, root.findViewById(R.id.llRDFilters), false);
    }

    private void removeRow(final View row) {
        if (filterRows.size() <= 1)
            return;
        filterRows.remove(row);
        ((ViewGroup) getView().findViewById(R.id.llRDFilters)).removeView(row);
    }

    private void bindRow(final View row) {
        final Spinner sp = (Spinner) row.findViewById(R.id.spRDFiltr);
        final Spinner op = (Spinner) row.findViewById(R.id.opRDFiltr);
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
        updateRow(row);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!spinnerGuard)
                    updateRow(row);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        op.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (!spinnerGuard)
                    updateRow(row);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        row.findViewById(R.id.btRDRemove).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                removeRow(row);
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
        return FilterRule.kindsForAll(enabledFilenames());
    }

    @SuppressWarnings("ConstantConditions")
    private Kind kindAt(View row) {
        Spinner sp = (Spinner) row.findViewById(R.id.spRDFiltr);
        return (Kind) sp.getAdapter().getItem(sp.getSelectedItemPosition());
    }

    private Op opAt(View row) {
        Spinner op = (Spinner) row.findViewById(R.id.opRDFiltr);
        return Op.values()[op.getSelectedItemPosition()];
    }

    private String[] opLabels() {
        return getResources().getStringArray(R.array.saRDOpLabels);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateRow(View row) {
        Spinner op = (Spinner) row.findViewById(R.id.opRDFiltr);
        Kind kind = kindAt(row);
        boolean numeric = FilterRule.isNumeric(kind);
        op.setVisibility(numeric ? View.VISIBLE : View.GONE);
        Op o = opAt(row);
        boolean two = FilterRule.needsSecondValue(kind, o);
        row.findViewById(R.id.et2RDFiltr).setVisibility(two ? View.VISIBLE : View.GONE);
    }

    private final OnClickListener goListener = new OnClickListener() {
        public void onClick(View v) {
            runSearch(false);
        }
    };

    private final OnClickListener showAllListener = new OnClickListener() {
        public void onClick(View v) {
            runSearch(true);
        }
    };

    private final OnClickListener addListener = new OnClickListener() {
        public void onClick(View v) {
if (getView() == null)
            return;
            filterRows.add(buildRow(getView()));
            ((ViewGroup) getView().findViewById(R.id.llRDFilters)).addView(filterRows.get(filterRows.size() - 1));
            bindRow(filterRows.get(filterRows.size() - 1));
        }
    };

    private void runSearch(final boolean showAll) {
        if (re.isRunning()) {
            re.stopThread();
            updateGoButton();
            return;
        }
        String zad[] = new String[filterRows.size()];
        for (int i = 0; i < filterRows.size(); i++) {
            View row = filterRows.get(i);
            Kind kind = kindAt(row);
            Op o = opAt(row);
            String v1 = ((EditText) row.findViewById(R.id.etRDFiltr)).getText().toString();
            String v2 = ((EditText) row.findViewById(R.id.et2RDFiltr)).getText().toString();
            String pat = FilterRule.pattern(kind, o, v1, v2);
            if (pat == null)
                zad[i] = "";
            else
                zad[i] = (((ToggleButton) row.findViewById(R.id.cbRDFiltr)).isChecked() ? "" : "!") + pat;
        }
        adapter.clear();
        List<String> fns = enabledFilenames();
        String rawFns[] = new String[fns.size()];
        for (int i = 0; i < fns.size(); i++)
            rawFns[i] = "raw/" + fns.get(i);
        currentMaxResults = showAll ? RegExpNative.ShowAllResults : RegExpNative.MaxListResults;
        re.startThread(getContext().getAssets(), rawFns, zad, showAll, currentMaxResults);
        adapter.setVerbose(showAll);
        launchRefresh();
    }

    private List<String> enabledFilenames() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        List<String> fns = (sp == null) ? new ArrayList<String>() : DataSources.enabledFilenames(sp);
        if (fns.isEmpty())
            return Collections.singletonList(sourceFilename());
        return fns;
    }

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
                int matches = Math.min(report.matches, currentMaxResults);
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
        for (View row : filterRows) {
            ((EditText) row.findViewById(R.id.etRDFiltr)).setText("");
            ((EditText) row.findViewById(R.id.et2RDFiltr)).setText("");
            ((ToggleButton) row.findViewById(R.id.cbRDFiltr)).setChecked(true);
        }
        tvProgress.setText("");
        updateGoButton();
    }
}