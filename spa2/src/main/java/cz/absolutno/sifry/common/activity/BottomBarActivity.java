package cz.absolutno.sifry.common.activity;

import static android.content.pm.PackageManager.GET_META_DATA;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import androidx.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentTransaction;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import cz.absolutno.sifry.App;
import cz.absolutno.sifry.R;
import cz.absolutno.sifry.Utils;
import cz.absolutno.sifry.common.widget.BottomBarView;

@SuppressWarnings("deprecation")
public abstract class BottomBarActivity extends FragmentActivity implements OnBackStackChangedListener {

    private static final int HAS_COPY = 0x1;
    private static final int HAS_PASTE = 0x2;
    private static final int HAS_CLEAR = 0x4;
    private static final int HAS_REFERENCE = 0x8;
    private static final int HAS_PACKS = 0x10;

    private BottomBarView bbar;

    protected abstract int getPrefID();

    protected abstract int getHelpID();

    protected String getStateKey() {
        return getClass().getName();
    }

    /**
     * Restores a previously saved workbench session (tab + input/progress) when the
     * activity is being created with no saved instance state. Must be called from the
     * subclass {@code onCreate} after the bottom bar entries have been set up; it
     * returns {@code true} when a session was restored, in which case the subclass
     * should skip creating its default screen.
     */
    protected final boolean restoreWorkbench() {
        WorkbenchStateStore.WorkbenchState wbs = WorkbenchStateStore.read(getStateKey());
        if (wbs == null)
            return false;

        FragmentManager fm = getSupportFragmentManager();
        boolean created = false;
        AbstractDFragment base = null;
        if (wbs.baseClass != null) {
            base = instantiate(wbs.baseClass);
            if (base != null) {
                fm.beginTransaction().replace(R.id.content, base, "D").commit();
                fm.executePendingTransactions();
                if (wbs.baseData != null && wbs.baseData.size() > 0 && !(base instanceof AbstractCFragment))
                    deliverData(base, wbs.baseData);
                created = true;
            }
        }

        boolean pushed = false;
        if (wbs.hasActiveData && wbs.activeClass != null && !wbs.activeClass.equals(wbs.baseClass)) {
            AbstractDFragment active = instantiate(wbs.activeClass);
            if (active != null) {
                FragmentTransaction trans = fm.beginTransaction();
                if (wbs.activeData.size() > 0 && active instanceof AbstractCFragment)
                    active.setArguments(wbs.activeData);
                trans.replace(R.id.content, active, wbs.activeTag != null ? wbs.activeTag : "D");
                trans.addToBackStack(null);
                trans.commit();
                fm.executePendingTransactions();
                if (wbs.activeData.size() > 0 && !(active instanceof AbstractCFragment))
                    deliverData(active, wbs.activeData);
                pushed = true;
                created = true;
            }
        }

        if (!created)
            return false;

        if (pushed) {
            String[] entries = bbar.getEntries();
            if (entries != null) {
                int ix = wbs.barIx;
                if (ix < 0 || ix >= entries.length)
                    ix = 0;
                bbar.setEntries(entries, ix);
            }
        }
        return true;
    }

    /**
     * Applies a fragment's saved state, waiting for the fragment's view to be
     * created if necessary. A newly instanced fragment's view may not exist yet
     * right after {@link FragmentManager#executePendingTransactions()} (its
     * {@code onCreateView} can run a frame later), and the D-fragments' own
     * {@code loadData} guards skip restoration while the view is still null.
     */
    private void deliverData(final AbstractDFragment f, final Bundle data) {
        if (f.getView() != null) {
            f.loadData(data);
            return;
        }
        f.getViewLifecycleOwnerLiveData().observeForever(new androidx.lifecycle.Observer<androidx.lifecycle.LifecycleOwner>() {
            @Override
            public void onChanged(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
                f.getViewLifecycleOwnerLiveData().removeObserver(this);
                if (f.getView() != null)
                    f.loadData(data);
            }
        });
    }

    private AbstractDFragment instantiate(String cls) {
        try {
            Class<?> c = Class.forName(cls, false, App.getContext().getClassLoader());
            Object o = c.getDeclaredConstructor().newInstance();
            if (o instanceof AbstractDFragment)
                return (AbstractDFragment) o;
        } catch (Exception e) {
            /* stale state from an older / different build, fall back to the default screen */
        }
        return null;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(App.localizedContext(newBase));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        App.updateLocale();
        setContentView(R.layout.bottom_bar_layout);
        bbar = findViewById(R.id.bottom_bar);
        bbar.setOnChangeListener(new BottomBarView.OnChangeListener() {
            public void onChange(int curIx, int lastIx) {
                onBottomBarChange(curIx, lastIx);
            }
        });

        try {
            setTitle(getPackageManager().getActivityInfo(getComponentName(), GET_META_DATA).labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            // leave title as it is
        }
    }

    public BottomBarView getBBar() {
        return bbar;
    }

    protected AbstractDFragment getCurrFragment() {
        return (AbstractDFragment) getSupportFragmentManager().findFragmentById(R.id.content);
    }

    protected abstract void onBottomBarChange(int curIx, int lastIx);

    public void onBackStackChanged() {
        ActivityCompat.invalidateOptionsMenu(this);
    }

    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.komponenty_menu, menu);

        AbstractDFragment currFragment = getCurrFragment();
        boolean hasCopy, hasPaste, hasClear, hasReference, hasPacks;
        if (currFragment != null) {
            int caps = currFragment.getMenuCaps();
            hasCopy = ((caps & HAS_COPY) != 0);
            hasPaste = ((caps & HAS_PASTE) != 0);
            hasClear = ((caps & HAS_CLEAR) != 0);
            hasReference = ((caps & HAS_REFERENCE) != 0);
            hasPacks = ((caps & HAS_PACKS) != 0);
        } else {
            hasCopy = false;
            hasPaste = false;
            hasClear = false;
            hasReference = false;
            hasPacks = false;
        }

        menu.findItem(R.id.mCtxSettings).setVisible(getPrefID() != 0);
        menu.findItem(R.id.mCtxReference).setVisible(hasReference);
        menu.findItem(R.id.mCtxPacks).setVisible(hasPacks);
        menu.findItem(R.id.mCtxCopy).setVisible(hasCopy);
        menu.findItem(R.id.mCtxPaste).setVisible(hasPaste);
        menu.findItem(R.id.mCtxClear).setVisible(hasClear);
        return true;
    }

    @SuppressLint("InlinedApi") /* EXTRA_*, using the value of EXTRA_SHOW_FRAGMENT_ARGUMENTS in SettingsActivity */
    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        AbstractDFragment currFragment = getCurrFragment(); /* Can't be null if we're in Copy, Cut, Paste */
        int id = item.getItemId();
        if(id == R.id.mCtxSettings) {
            intent = new Intent(this, SettingsActivity.class);
            Bundle b = new Bundle();
            b.putInt(App.SPEC, getPrefID());
            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            /* Class does not exist in Gingerbread ⇒ getName() results in a NoClassDefFoundError */
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, "cz.absolutno.sifry.common.activity.SettingsFragment");
            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, b);
            startActivity(intent);
            return true;
        } else if(id == R.id.mCtxReference) {
            AbstractDFragment f = getCurrFragment();
            if (f != null)
                f.onOpenReference();
            return true;
        } else if(id == R.id.mCtxPacks) {
            AbstractDFragment f = getCurrFragment();
            if (f != null)
                f.onOpenPacks();
            return true;
        } else if(id == R.id.mCtxHelp) {
            intent = new Intent(this, HelpActivity.class);
            intent.putExtra(App.SPEC, getHelpID());
            startActivity(intent);
            return true;
        } else if(id == R.id.mCtxCopy) {
            String s = currFragment.onCopy();
            if (s != null && !s.equals(""))
                //noinspection ConstantConditions
                ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setText(s.replace('·', ' '));
            else
                Utils.toast(R.string.tErrCopy);
            return true;
        } else if(id == R.id.mCtxPaste) {
            //noinspection ConstantConditions
            CharSequence cs = ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).getText();
            if (cs != null && cs.length() != 0)
                currFragment.onPaste(cs.toString());
            else
                Utils.toast(R.string.tErrPaste);
            return true;
        } else if(id == R.id.mCtxClear) {
            currFragment.onClear();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp == null) return;
        if (sp.getBoolean("pref_changed", false)) {
            onPreferencesChanged();
            getCurrFragment().onPreferencesChanged();
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("pref_changed", false);
        editor.apply();
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSupportFragmentManager().removeOnBackStackChangedListener(this);
        saveWorkbenchState();
    }

    protected void saveWorkbenchState() {
        if (bbar == null)
            return;
        FragmentManager fm = getSupportFragmentManager();
        AbstractDFragment active = getCurrFragment();
        if (active == null || active.getView() == null)
            return;
        WorkbenchStateStore.WorkbenchState wbs = new WorkbenchStateStore.WorkbenchState();
        wbs.activeClass = active.getClass().getName();
        wbs.activeTag = active.getTag();
        wbs.hasActiveData = active.saveData(wbs.activeData);
        AbstractDFragment base = (AbstractDFragment) fm.findFragmentByTag("D");
        if (base != null) {
            wbs.baseClass = base.getClass().getName();
            if (base != active) {
                Bundle bd = new Bundle();
                if (base.saveData(bd))
                    wbs.baseData = bd;
            } else {
                wbs.baseData = wbs.activeData;
                wbs.hasActiveData = true;
            }
        }
        wbs.barIx = bbar.getCurrent();
        WorkbenchStateStore.write(getStateKey(), wbs);
    }

    protected void onPreferencesChanged() {
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        AbstractDFragment currFragment = getCurrFragment();
        if (currFragment != null)
            currFragment.onResumeFragments();
    }

}
