package cz.absolutno.sifry.regexp;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import cz.absolutno.sifry.App;
import cz.absolutno.sifry.R;

final class RegExpExpListAdapter extends BaseExpandableListAdapter {

    private int matches = 0;
    private boolean verbose = false;
    private final RegExpNative re;
    private ArrayList<String> snap = null;

    RegExpExpListAdapter(RegExpNative re) {
        this.re = re;
    }

    public void update(int matches) {
        this.matches = matches;
        notifyDataSetChanged();
    }

    public void clear() {
        matches = 0;
        snap = null;
        notifyDataSetChanged();
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        notifyDataSetChanged();
    }

    public int getMatchCount() {
        return matches;
    }

    public void setSnapshot(ArrayList<String> snapshot) {
        snap = snapshot;
        matches = (snapshot == null) ? 0 : snapshot.size();
        notifyDataSetChanged();
    }

    public void snapshotTo(ArrayList<String> out) {
        if (matches == 0)
            return;
        for (int i = 0; i < matches; i++)
            out.add(raw(i));
    }

    private String raw(int ix) {
        return (snap != null) ? snap.get(ix) : re.getResult(ix);
    }

    public int getGroupCount() {
        return (matches + 99) / 100;
    }

    public String getGroup(int groupPosition) {
        int ub = groupPosition * 100 + 99;
        if (ub >= matches) ub = matches - 1;
        return String.format("%s – %s", displayOf(raw(groupPosition * 100)), displayOf(raw(ub)));
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = App.getInflater().inflate(R.layout.gen_group_item, parent, false);
        ((TextView) convertView).setText(getGroup(groupPosition));
        return convertView;
    }

    public int getChildrenCount(int groupPosition) {
        if (groupPosition >= getGroupCount()) return 0;
        return (groupPosition < getGroupCount() - 1) ? 100 : (((matches - 1) % 100) + 1);
    }

    public String getChild(int groupPosition, int childPosition) {
        int ix = groupPosition * 100 + childPosition;
        if (snap != null)
            return snap.get(ix);
        String result = re.getResult(ix);
        if (!verbose)
            return result;
        String source = re.getResultSource(ix).replaceFirst("^raw/", "").replaceFirst("\\.canon$", "");
        int p = result.indexOf(':');
        String extra = p < 0 ? result : result.substring(0, p) + " — " + result.substring(p + 1);
        return source.isEmpty() ? extra : extra + " (" + source + ")";
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = App.getInflater().inflate(R.layout.simple_list_item, parent, false);
        ((TextView) convertView).setText(getChild(groupPosition, childPosition));
        return convertView;
    }

    private String displayOf(String result) {
        int p = result.indexOf(':');
        return p < 0 ? result : result.substring(p + 1);
    }


    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return true;
    }

}
