package cz.absolutno.sifry.lookup;

import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;

import cz.absolutno.sifry.R;
import cz.absolutno.sifry.common.activity.BottomBarActivity;

public final class LookupPackActivity extends BottomBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getBBar().setEntries(null, 0);

            if (!restoreWorkbench()) {
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                trans.replace(R.id.content, new LookupDFragment(), "D");
                trans.commit();
            }
        }
    }

    @Override
    protected int getPrefID() {
        return 0;
    }

    @Override
    protected int getHelpID() {
        return R.string.tHelpLookupPacks;
    }

    @Override
    protected void onBottomBarChange(int curIx, int lastIx) {
    }
}