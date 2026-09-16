package cz.absolutno.sifry.lookup;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.absolutno.sifry.R;
import cz.absolutno.sifry.Utils;
import cz.absolutno.sifry.common.activity.AbstractDFragment;
import cz.absolutno.sifry.common.datapack.DataPack;
import cz.absolutno.sifry.common.datapack.EntityResult;
import cz.absolutno.sifry.common.datapack.FieldDefinition;
import cz.absolutno.sifry.common.datapack.FilterSpec;
import cz.absolutno.sifry.common.datapack.NatoDataPack;
import cz.absolutno.sifry.common.datapack.Packs;
import cz.absolutno.sifry.common.datapack.SearchResult;

public final class LookupDFragment extends AbstractDFragment {

    private static final int MAX_DROPDOWN = 15;
    private static final String K_PACK = "pack";
    private static final String K_FIELD = "field";
    private static final String K_QUERY = "query";

    private Spinner spPack;
    private Spinner spField;
    private EditText etQuery;
    private Spinner spQuery;
    private TextView tvCount;
    private TextView tvNote;
    private TableLayout tlResults;
    private List<DataPack> packs = null;
    private List<EntityResult> rowData = null;
    private String sortField = null;
    private boolean sortAscending = true;
    private boolean dropdownActive = false;

    @Override
    protected int getMenuCaps() {
        return 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lookup_layout, container, false);
        spPack = v.findViewById(R.id.spLUPack);
        spField = v.findViewById(R.id.spLUField);
        etQuery = v.findViewById(R.id.etLUQuery);
        spQuery = v.findViewById(R.id.spLUQuery);
        tvCount = v.findViewById(R.id.tvLUPocet);
        tvNote = v.findViewById(R.id.tvLUNote);
        tlResults = v.findViewById(R.id.tlLUResults);

        packs = loadPacks(getActivity());
        if (packs.isEmpty()) {
            Utils.toast(R.string.tLUNoMatch);
            return v;
        }

        spPack.setAdapter(packAdapter());
        spPack.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                clearResults();
                rebuildFieldSpinner();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        rebuildFieldSpinner();
        spField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortField = sortKey();
                updateQueryControl();
                if (rowData != null)
                    renderTable();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spQuery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (dropdownActive && rowData != null)
                    search();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ((Button) v.findViewById(R.id.btLUGo)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                search();
            }
        });
        return v;
    }

    private List<DataPack> loadPacks(Context context) {
        List<DataPack> out = new ArrayList<DataPack>();
        AssetManager assets = context.getResources().getAssets();
        try {
            out.add(Packs.astronomy(assets));
            out.add(Packs.zodiac(assets));
            out.add(Packs.nato(assets));
            out.add(Packs.greek(assets));
        } catch (IOException e) {
            /* no user-facing failure here; the packed assets are part of the apk */
        }
        return out;
    }

    private ArrayAdapter<String> packAdapter() {
        List<String> names = new ArrayList<String>();
        for (DataPack p : packs)
            names.add(p.displayName());
        ArrayAdapter<String> a = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, names);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void rebuildFieldSpinner() {
        List<String> items = new ArrayList<String>();
        items.add(getString(R.string.tLUAllFields));
        DataPack pack = selectedPack();
        if (pack != null)
            for (FieldDefinition fd : pack.fields())
                items.add(fd.displayName());
        int keep = Math.max(0, spField.getSelectedItemPosition());
        ArrayAdapter<String> a = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spField.setAdapter(a);
        if (keep < a.getCount())
            spField.setSelection(keep);
        sortField = sortKey();
        updateQueryControl();
    }

    private DataPack selectedPack() {
        int i = spPack.getSelectedItemPosition();
        return (i >= 0 && i < packs.size()) ? packs.get(i) : null;
    }

    private String sortKey() {
        DataPack pack = selectedPack();
        if (pack == null)
            return null;
        int fieldIx = spField.getSelectedItemPosition();
        if (fieldIx <= 0)
            return null;
        return pack.fields().get(fieldIx - 1).id();
    }

    private void clearResults() {
        rowData = null;
        tvNote.setText("");
        tvNote.setVisibility(View.GONE);
        tlResults.removeAllViews();
        tvCount.setText("");
    }

    private void updateQueryControl() {
        DataPack pack = selectedPack();
        int fieldIx = spField.getSelectedItemPosition();
        if (pack == null || fieldIx <= 0) {
            dropdownActive = false;
            etQuery.setVisibility(View.VISIBLE);
            spQuery.setVisibility(View.GONE);
            return;
        }
        FieldDefinition fd = pack.fields().get(fieldIx - 1);
        List<String> vals = pack.distinctValues(fd.id());
        if (vals.size() > 1 && vals.size() <= MAX_DROPDOWN) {
            dropdownActive = true;
            List<String> choices = new ArrayList<String>();
            choices.add(getString(R.string.tLUAllValues));
            choices.addAll(vals);
            ArrayAdapter<String> a = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_spinner_item, choices);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spQuery.setAdapter(a);
            spQuery.setSelection(0);
            etQuery.setVisibility(View.GONE);
            spQuery.setVisibility(View.VISIBLE);
        } else {
            dropdownActive = false;
            spQuery.setVisibility(View.GONE);
            etQuery.setVisibility(View.VISIBLE);
        }
    }

    private void search() {
        DataPack pack = selectedPack();
        if (pack == null)
            return;
        int fieldIx = spField.getSelectedItemPosition();
        String q;
        FilterSpec spec;
        if (fieldIx <= 0) {
            q = etQuery.getText().toString().trim();
            spec = FilterSpec.identity(q);
        } else if (dropdownActive) {
            int sel = spQuery.getSelectedItemPosition();
            if (sel <= 0) {
                q = "";
                spec = FilterSpec.identity("");
            } else {
                String val = (String) spQuery.getItemAtPosition(sel);
                FieldDefinition fd = pack.fields().get(fieldIx - 1);
                spec = FilterSpec.on(fd.id(), FilterSpec.Op.EQ, val, null);
                q = val;
            }
        } else {
            q = etQuery.getText().toString().trim();
            FieldDefinition fd = pack.fields().get(fieldIx - 1);
            FilterSpec.Op op = fd.kind() == FieldDefinition.Kind.TEXT
                    ? FilterSpec.Op.CONTAINS : FilterSpec.Op.EQ;
            spec = FilterSpec.on(fd.id(), op, q, null);
        }
        SearchResult sr = pack.search(spec);
        rowData = new ArrayList<EntityResult>(sr.results());

        StringBuilder note = new StringBuilder();
        if (fieldIx <= 0 && sr.results().isEmpty() && pack instanceof NatoDataPack) {
            NatoDataPack nato = (NatoDataPack) pack;
            String spelled = nato.spell(q);
            if (!spelled.isEmpty())
                note.append(getString(R.string.tLUSpell)).append(' ').append(spelled).append('\n');
            String unspelled = nato.unspell(q);
            if (!unspelled.isEmpty())
                note.append(getString(R.string.tLUUnspell)).append(' ').append(unspelled).append('\n');
        }
        if (rowData.isEmpty() && note.length() == 0)
            note.append(getString(R.string.tLUNoMatch));
        tvNote.setText(note.toString().trim());
        tvNote.setVisibility(note.length() == 0 ? View.GONE : View.VISIBLE);
        tvCount.setText(String.valueOf(rowData.size()));
        renderTable();
    }

    private void renderTable() {
        tlResults.removeAllViews();
        if (rowData == null || rowData.isEmpty())
            return;
        DataPack pack = selectedPack();
        if (pack == null)
            return;

        Collections.sort(rowData, rowComparator());

        TableRow header = new TableRow(getActivity());
        addCell(header, getString(R.string.tLUName), true, null);
        for (FieldDefinition fd : pack.fields())
            addCell(header, fd.displayName(), true, fd.id());
        tlResults.addView(header);

        for (EntityResult e : rowData) {
            TableRow tr = new TableRow(getActivity());
            addCell(tr, e.displayText(), false, null);
            for (FieldDefinition fd : pack.fields()) {
                String value = e.property(fd.id());
                addCell(tr, value == null ? "" : value, false, null);
            }
            tlResults.addView(tr);
        }
    }

    private Comparator<EntityResult> rowComparator() {
        return new Comparator<EntityResult>() {
            public int compare(EntityResult a, EntityResult b) {
                int c = compareValues(columnValue(a), columnValue(b));
                return sortAscending ? c : -c;
            }
        };
    }

    private String columnValue(EntityResult e) {
        return sortField == null ? e.displayText() : e.property(sortField);
    }

    private int compareValues(String va, String vb) {
        if (va == null)
            va = "";
        if (vb == null)
            vb = "";
        Integer ia = tryInt(va);
        Integer ib = tryInt(vb);
        if (ia != null && ib != null)
            return ia.compareTo(ib);
        return va.compareToIgnoreCase(vb);
    }

    private static Integer tryInt(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void addCell(TableRow row, String text, boolean header, final String key) {
        TextView tv = new TextView(getActivity());
        tv.setText(text);
        tv.setPadding(dp(4), dp(4), dp(10), dp(4));
        if (header) {
            tv.setTypeface(null, Typeface.BOLD);
            if (key != null)
                tv.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        if (key.equals(sortField))
                            sortAscending = !sortAscending;
                        else {
                            sortField = key;
                            sortAscending = true;
                        }
                        renderTable();
                    }
                });
        }
        row.addView(tv);
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getActivity().getResources().getDisplayMetrics());
    }

    @Override
    public boolean saveData(Bundle data) {
        if (getView() == null)
            return false;
        data.putInt(K_PACK, spPack.getSelectedItemPosition());
        data.putInt(K_FIELD, spField.getSelectedItemPosition());
        if (dropdownActive)
            data.putInt(K_QUERY, spQuery.getSelectedItemPosition());
        else
            data.putString(K_QUERY, etQuery.getText().toString());
        return true;
    }

    @Override
    public void loadData(Bundle data) {
        if (getView() == null)
            return;
        int p = data.getInt(K_PACK, 0);
        if (p >= 0 && p < spPack.getCount())
            spPack.setSelection(p);
        int f = data.getInt(K_FIELD, 0);
        if (f >= 0 && f < spField.getCount())
            spField.setSelection(f);
        if (data.containsKey(K_QUERY)) {
            if (dropdownActive) {
                int q = data.getInt(K_QUERY, 0);
                if (q >= 0 && q < spQuery.getCount())
                    spQuery.setSelection(q);
            } else {
                etQuery.setText(data.getString(K_QUERY, ""));
            }
        }
        search();
    }
}