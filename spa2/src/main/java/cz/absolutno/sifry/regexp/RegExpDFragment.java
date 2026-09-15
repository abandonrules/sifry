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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.absolutno.sifry.App;
import cz.absolutno.sifry.R;
import cz.absolutno.sifry.Utils;
import cz.absolutno.sifry.common.activity.AbstractDFragment;
import cz.absolutno.sifry.common.dictionary.DictionaryQueryCompiler;
import cz.absolutno.sifry.common.dictionary.WordPatternQuery;
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
    private List<String> pendingResults = null;

    @Override
    protected int getMenuCaps() {
        return HAS_CLEAR | HAS_REFERENCE;
    }

    private static final String SEP = "\u001F";
    private static final String K_RESULTS = "res";

    @Override
    public boolean saveData(Bundle data) {
        if (getView() == null)
            return false;
        ArrayList<String> rows = new ArrayList<String>();
        boolean meaningful = false;
        for (View row : filterRows) {
            String v1;
            if (kindAt(row) == Kind.DATASET)
                v1 = selectedDataset(row);
            else
                v1 = ((EditText) row.findViewById(R.id.etRDFiltr)).getText().toString();
            String v2 = ((EditText) row.findViewById(R.id.et2RDFiltr)).getText().toString();
            int st = statePosToDegree(((Spinner) row.findViewById(R.id.cbRDFiltr)).getSelectedItemPosition());
            if (v1.length() > 0 || v2.length() > 0 || st != FilterRule.ST_YES)
                meaningful = true;
            rows.add(kindAt(row).ordinal() + SEP + opAt(row).ordinal() + SEP + v1 + SEP + v2 + SEP + st);
        }
        boolean hasResults = adapter != null && adapter.getMatchCount() > 0;
        if (!meaningful && !hasResults)
            return false;
        if (meaningful)
            data.putStringArrayList(App.VSTUP2, rows);
        if (hasResults) {
            ArrayList<String> res = new ArrayList<String>();
            adapter.snapshotTo(res);
            data.putStringArrayList(K_RESULTS, res);
        }
        return true;
    }

    @Override
    public void loadData(Bundle data) {
        if (getView() == null)
            return;
        ArrayList<String> rows = data.getStringArrayList(App.VSTUP2);
        if (rows == null || rows.isEmpty())
            return;
        ViewGroup ll = (ViewGroup) getView().findViewById(R.id.llRDFilters);
        while (filterRows.size() < rows.size()) {
            View row = buildRow(getView());
            filterRows.add(row);
            ll.addView(row);
            bindRow(row);
        }
        while (filterRows.size() > rows.size()) {
            View row = filterRows.get(filterRows.size() - 1);
            filterRows.remove(row);
            ll.removeView(row);
        }
        spinnerGuard = true;
        for (int i = 0; i < rows.size(); i++) {
            String[] parts = rows.get(i).split(SEP, -1);
            if (parts.length < 5)
                continue;
            try {
                View row = filterRows.get(i);
                Spinner sp = (Spinner) row.findViewById(R.id.spRDFiltr);
                int ki = Integer.parseInt(parts[0]);
                if (ki >= 0 && ki < sp.getAdapter().getCount())
                    sp.setSelection(ki);
                Spinner op = (Spinner) row.findViewById(R.id.opRDFiltr);
                int oi = Integer.parseInt(parts[1]);
                if (oi >= 0 && oi < op.getAdapter().getCount())
                    op.setSelection(oi);
                if (kindAt(row) == Kind.DATASET)
                    setSelectedDataset(row, parts[2]);
                else
                    ((EditText) row.findViewById(R.id.etRDFiltr)).setText(parts[2]);
                ((EditText) row.findViewById(R.id.et2RDFiltr)).setText(parts[3]);
                int st = 1;
                try {
                    st = Integer.parseInt(parts[4]);
                } catch (NumberFormatException e) {
                    /* stale/corrupt state, default to Yes */
                }
                if (st != FilterRule.ST_NO && st != FilterRule.ST_OR)
                    st = FilterRule.ST_YES;
                ((Spinner) row.findViewById(R.id.cbRDFiltr)).setSelection(degreeToStatePos(st));
            } catch (NumberFormatException e) {
                /* stale/corrupt row, keep defaults */
            }
        }
        spinnerGuard = false;
        for (View row : filterRows)
            updateRow(row);
        if (data.containsKey(K_RESULTS)) {
            pendingResults = data.getStringArrayList(K_RESULTS);
            applyResults();
        }
    }

    private void applyResults() {
        if (adapter == null || pendingResults == null)
            return;
        adapter.setSnapshot(new ArrayList<String>(pendingResults));
        if (tvProgress != null)
            tvProgress.setText(String.valueOf(pendingResults.size()));
        pendingResults = null;
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
            View row = buildRow(v);
            filterRows.add(row);
            ((ViewGroup) v.findViewById(R.id.llRDFilters)).addView(row);
            bindRow(row);
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
        applyResults();
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
                View row = buildRow(getView());
                filterRows.add(row);
                ((ViewGroup) getView().findViewById(R.id.llRDFilters)).addView(row);
                bindRow(row);
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
        final Spinner cb = (Spinner) row.findViewById(R.id.cbRDFiltr);
        final Spinner ds = (Spinner) row.findViewById(R.id.spRDDataset);
        spinnerGuard = true;
        int spPos = sp.getSelectedItemPosition();
        int opPos = op.getSelectedItemPosition();
        int cbPos = cb.getSelectedItemPosition();
        int dsPos = ds.getSelectedItemPosition();
        ArrayAdapter<Kind> ka = kindAdapter(kinds());
        sp.setAdapter(ka);
        ArrayAdapter<CharSequence> oa = new ArrayAdapter<CharSequence>(getActivity(),
                android.R.layout.simple_spinner_item, opLabels());
        oa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        op.setAdapter(oa);
        ArrayAdapter<String> ca = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, stateLabels());
        ca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cb.setAdapter(ca);
        ArrayAdapter<String> da = datasetAdapter(buildDatasetEntries());
        ds.setAdapter(da);
        sp.setSelection(spPos >= 0 && spPos < ka.getCount() ? spPos : 0);
        op.setSelection(opPos >= 0 && opPos < oa.getCount() ? opPos : 0);
        cb.setSelection(cbPos >= 0 && cbPos < ca.getCount() ? cbPos : 0);
        ds.setSelection(dsPos >= 0 && dsPos < da.getCount() ? dsPos : 0);
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

    private int statePosToDegree(int pos) {
        if (pos == 2)
            return FilterRule.ST_OR;
        return pos == 1 ? FilterRule.ST_NO : FilterRule.ST_YES;
    }

    private int degreeToStatePos(int degree) {
        if (degree == FilterRule.ST_OR)
            return 2;
        return degree == FilterRule.ST_NO ? 1 : 0;
    }

    private String[] stateLabels() {
        return new String[]{
                getString(R.string.tRDYes),
                getString(R.string.tRDNo),
                getString(R.string.tRDOr)
        };
    }

    private String[] opLabels() {
        return getResources().getStringArray(R.array.saRDOpLabels);
    }

    private static final class DatasetEntry implements Comparable<DatasetEntry> {
        private static final java.text.Collator COLLATOR = java.text.Collator.getInstance();
        final String file;
        final String display;
        DatasetEntry(String file, String display) { this.file = file; this.display = display; }
        public int compareTo(DatasetEntry other) { return COLLATOR.compare(display, other.display); }
    }

    private List<DatasetEntry> buildDatasetEntries() {
        String[] files = getResources().getStringArray(R.array.saREDictionaryFilenames);
        String[] names = getResources().getStringArray(R.array.saREDictionaries);
        List<DatasetEntry> entries = new ArrayList<DatasetEntry>();
        for (int i = 1; i < Math.min(files.length, names.length); i++)
            if (files[i] != null && files[i].length() > 0)
                entries.add(new DatasetEntry(files[i], names[i] + " (" + files[i] + ")"));
        Collections.sort(entries);
        return entries;
    }

    private ArrayAdapter<String> datasetAdapter(List<DatasetEntry> entries) {
        final String[] fileNames = new String[entries.size()];
        final String[] displays = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            fileNames[i] = entries.get(i).file;
            displays[i] = entries.get(i).display;
        }
        return new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, fileNames) {
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
                tv.setText(displays[pos]);
                float density = getResources().getDisplayMetrics().density;
                tv.setPadding(0, (int) (12 * density), 0, (int) (12 * density));
                return tv;
            }
        };
    }

    private String selectedDataset(View row) {
        Spinner ds = (Spinner) row.findViewById(R.id.spRDDataset);
        if (ds.getAdapter() == null || ds.getAdapter().getCount() == 0)
            return "";
        Object item = ds.getAdapter().getItem(ds.getSelectedItemPosition());
        return item == null ? "" : item.toString();
    }

    private void setSelectedDataset(View row, String filename) {
        Spinner ds = (Spinner) row.findViewById(R.id.spRDDataset);
        if (ds.getAdapter() == null)
            return;
        for (int i = 0; i < ds.getAdapter().getCount(); i++) {
            Object item = ds.getAdapter().getItem(i);
            if (item != null && item.toString().equals(filename)) {
                ds.setSelection(i);
                return;
            }
        }
    }

    private static final int TEXT_INPUT = android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    private static final int NUMERIC_INPUT = android.text.InputType.TYPE_CLASS_NUMBER;

    @SuppressWarnings("ConstantConditions")
    private void updateRow(View row) {
        Spinner op = (Spinner) row.findViewById(R.id.opRDFiltr);
        Kind kind = kindAt(row);
        boolean numeric = FilterRule.isNumeric(kind);
        boolean dataset = (kind == Kind.DATASET);
        op.setVisibility(numeric ? View.VISIBLE : View.GONE);
        Op o = opAt(row);
        boolean two = FilterRule.needsSecondValue(kind, o);
        EditText value = (EditText) row.findViewById(R.id.etRDFiltr);
        EditText value2 = (EditText) row.findViewById(R.id.et2RDFiltr);
        Spinner ds = (Spinner) row.findViewById(R.id.spRDDataset);
        value2.setVisibility(two && !dataset ? View.VISIBLE : View.GONE);
        value.setVisibility(dataset ? View.GONE : View.VISIBLE);
        ds.setVisibility(dataset ? View.VISIBLE : View.GONE);
        value.setInputType(numeric ? NUMERIC_INPUT : TEXT_INPUT);
        value2.setInputType(numeric ? NUMERIC_INPUT : TEXT_INPUT);
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
        List<String> pats = new ArrayList<String>();
        List<Integer> states = new ArrayList<Integer>();
        List<String> narrowed = new ArrayList<String>();
        for (View row : filterRows) {
            Kind kind = kindAt(row);
            Op o = opAt(row);
            int st = statePosToDegree(((Spinner) row.findViewById(R.id.cbRDFiltr)).getSelectedItemPosition());
            if (kind == Kind.DATASET) {
                String file = selectedDataset(row);
                if (file.length() > 0 && !narrowed.contains(file))
                    narrowed.add(file);
                pats.add(null);
                states.add(st);
                continue;
            }
            String v1 = ((EditText) row.findViewById(R.id.etRDFiltr)).getText().toString();
            String v2 = ((EditText) row.findViewById(R.id.et2RDFiltr)).getText().toString();
            pats.add(buildPattern(kind, o, v1, v2));
            states.add(st);
        }
        List<String> folded = FilterRule.foldPatterns(pats, states);
        String zad[] = folded.toArray(new String[folded.size()]);
        adapter.clear();
        List<String> fns = enabledFilenames();
        if (!narrowed.isEmpty())
            fns = narrowed;
        String rawFns[] = new String[fns.size()];
        for (int i = 0; i < fns.size(); i++)
            rawFns[i] = "raw/" + fns.get(i);
        currentMaxResults = showAll ? RegExpNative.ShowAllResults : RegExpNative.MaxListResults;
        re.startThread(getContext().getAssets(), rawFns, zad, showAll, currentMaxResults);
        adapter.setVerbose(showAll);
        launchRefresh();
    }

    private String buildPattern(Kind kind, Op o, String v1, String v2) {
        if (kind == Kind.EQUALS || kind == Kind.STARTS || kind == Kind.ENDS) {
            String a = v1 == null ? "" : v1.trim().toLowerCase();
            if (a.isEmpty())
                return "";
            if (kind == Kind.EQUALS)
                return DictionaryQueryCompiler.exactKeyPattern(a);
            WordPatternQuery.Builder q = WordPatternQuery.builder();
            if (kind == Kind.STARTS)
                q.prefix(a);
            else
                q.suffix(a);
            return DictionaryQueryCompiler.compile(q.build());
        }
        return FilterRule.pattern(kind, o, v1, v2);
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
        pendingResults = null;
        re.free();
        adapter.clear();
        for (View row : filterRows) {
            ((EditText) row.findViewById(R.id.etRDFiltr)).setText("");
            ((EditText) row.findViewById(R.id.et2RDFiltr)).setText("");
            Spinner ds = (Spinner) row.findViewById(R.id.spRDDataset);
            if (ds.getAdapter() != null && ds.getAdapter().getCount() > 0)
                ds.setSelection(0);
            ((Spinner) row.findViewById(R.id.cbRDFiltr)).setSelection(0);
        }
        tvProgress.setText("");
        updateGoButton();
    }
}