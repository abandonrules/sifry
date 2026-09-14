package cz.absolutno.sifry.common.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Base64;

import cz.absolutno.sifry.App;

/**
 * Durable per-tool workbench state: remembers each tool screen's active tab and
 * the input/progress on that screen so that leaving the tool and coming back to
 * it restores what the user was working on (see NAV requirements).
 * State is stored keyed by the activity class name.
 */
final class WorkbenchStateStore {

    private static final String PREFS = "workbench_state";

    static final class WorkbenchState {
        int barIx;
        String baseClass;
        Bundle baseData;
        String activeClass;
        String activeTag;
        boolean hasActiveData;
        final Bundle activeData = new Bundle();
    }

    private static SharedPreferences prefs() {
        return App.getContext().getSharedPreferences(PREFS, 0);
    }

    private WorkbenchStateStore() {
    }

    static void write(String key, WorkbenchState wbs) {
        Parcel p = Parcel.obtain();
        try {
            p.writeInt(wbs.barIx);
            p.writeString(wbs.baseClass);
            p.writeBundle(wbs.baseData);
            p.writeString(wbs.activeClass);
            p.writeString(wbs.activeTag);
            p.writeInt(wbs.hasActiveData ? 1 : 0);
            p.writeBundle(wbs.activeData);
            prefs().edit().putString(key, Base64.encodeToString(p.marshall(), Base64.NO_WRAP)).apply();
        } finally {
            p.recycle();
        }
    }

    static WorkbenchState read(String key) {
        String s = prefs().getString(key, null);
        if (s == null)
            return null;
        try {
            byte[] b = Base64.decode(s, Base64.NO_WRAP);
            Parcel p = Parcel.obtain();
            WorkbenchState wbs = new WorkbenchState();
            try {
                p.unmarshall(b, 0, b.length);
                p.setDataPosition(0);
                wbs.barIx = p.readInt();
                wbs.baseClass = p.readString();
                wbs.baseData = p.readBundle(App.getContext().getClassLoader());
                wbs.activeClass = p.readString();
                wbs.activeTag = p.readString();
                wbs.hasActiveData = p.readInt() != 0;
                Bundle ad = p.readBundle(App.getContext().getClassLoader());
                if (ad != null)
                    wbs.activeData.putAll(ad);
            } finally {
                p.recycle();
            }
            return wbs;
        } catch (Exception e) {
            return null;
        }
    }

    static void clear(String key) {
        prefs().edit().remove(key).apply();
    }

}