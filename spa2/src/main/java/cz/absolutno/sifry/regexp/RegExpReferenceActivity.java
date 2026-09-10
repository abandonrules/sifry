package cz.absolutno.sifry.regexp;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.absolutno.sifry.App;
import cz.absolutno.sifry.R;

public final class RegExpReferenceActivity extends FragmentActivity {

    public static final String EXTRA_FILENAME = "cz.absolutno.sifry.regexp.filename";

    private static final Pattern DISPLAY_RE = Pattern.compile("^(.*) \\(([^)]+), (\\d+)\\)$");
    private static final Pattern DEX_RE = Pattern.compile("#(\\d+)$");

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(App.localizedContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.updateLocale();
        setContentView(R.layout.reference_layout);

        String filename = getIntent().getStringExtra(EXTRA_FILENAME);
        if (filename == null)
            filename = getString(R.string.pref_re_dictionary_default);
        List<String> lines = readLines(filename);

        ScrollView svGrid = findViewById(R.id.svGrid);
        ListView lvList = findViewById(R.id.lvList);
        TextView tvNote = findViewById(R.id.tvNote);

        if (filename.startsWith("periodic")) {
            fillGrid(lines);
            svGrid.setVisibility(View.VISIBLE);
        } else if (filename.startsWith("pokemon") || filename.startsWith("wordle")) {
            lvList.setAdapter(listAdapter(lines, filename.startsWith("pokemon")));
            lvList.setVisibility(View.VISIBLE);
        } else {
            tvNote.setText(R.string.tReferenceNote);
            tvNote.setVisibility(View.VISIBLE);
        }
    }

    private List<String> readLines(String filename) {
        List<String> lines = new ArrayList<String>();
        try {
            AssetManager am = getAssets();
            InputStream is = am.open("raw/" + filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty())
                    lines.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private ArrayAdapter<String> listAdapter(List<String> lines, boolean pokemon) {
        if (pokemon) {
            final Map<String, Integer> numbers = new HashMap<String, Integer>();
            for (String line : lines) {
                Matcher m = DEX_RE.matcher(display(line));
                if (m.find())
                    numbers.put(line, Integer.valueOf(m.group(1)));
            }
            lines.sort(new Comparator<String>() {
                public int compare(String a, String b) {
                    Integer na = numbers.get(a);
                    Integer nb = numbers.get(b);
                    if (na == null) return nb == null ? 0 : 1;
                    if (nb == null) return -1;
                    return na.compareTo(nb);
                }
            });
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, lines);
        return adapter;
    }

    private static final int[][] TABLE = {
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2},
            {3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 6, 7, 8, 9, 10},
            {11, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 14, 15, 16, 17, 18},
            {19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36},
            {37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54},
            {55, 56, 0, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86},
            {87, 88, 0, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118},
            {0, 0, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 0},
            {0, 0, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 0}
    };

    private void fillGrid(List<String> lines) {
        Map<Integer, String[]> elements = new HashMap<Integer, String[]>();
        for (String line : lines) {
            String d = display(line);
            Matcher m = DISPLAY_RE.matcher(d);
            if (m.matches()) {
                int z = Integer.parseInt(m.group(3));
                if (!elements.containsKey(z))
                    elements.put(z, new String[]{m.group(1), m.group(2)});
            }
        }
        LinearLayout llGrid = findViewById(R.id.llGrid);
        for (int[] row : TABLE) {
            LinearLayout rr = new LinearLayout(this);
            rr.setOrientation(LinearLayout.HORIZONTAL);
            for (int z : row) {
                String[] el = z == 0 ? null : elements.get(z);
                if (el == null) {
                    View spacer = new View(this);
                    rr.addView(spacer, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                } else {
                    TextView cell = new TextView(this);
                    cell.setGravity(Gravity.CENTER);
                    cell.setTextColor(0xff000000);
                    cell.setTextSize(9);
                    cell.setText(el[1] + "\n" + z + "\n" + el[0]);
                    rr.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                }
            }
            llGrid.addView(rr, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private static String display(String line) {
        int colon = line.indexOf(':');
        return colon < 0 ? line : line.substring(colon + 1);
    }
}