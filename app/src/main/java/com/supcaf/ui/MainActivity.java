package com.supcaf.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.supcaf.R;
import com.supcaf.ui.achats.AchatsFragment;
import com.supcaf.ui.articles.ArticlesFragment;
import com.supcaf.ui.dashboard.DashboardFragment;
import com.supcaf.ui.tiers.TiersFragment;
import com.supcaf.ui.ventes.VentesFragment;
import com.supcaf.ui.journal.JournalFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bottomNav = findViewById(R.id.bottom_navigation);
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if      (id == R.id.nav_dashboard) fragment = new DashboardFragment();
            else if (id == R.id.nav_achats)    fragment = new AchatsFragment();
            else if (id == R.id.nav_ventes)    fragment = new VentesFragment();
            else if (id == R.id.nav_tiers)     fragment = new TiersFragment();
            else if (id == R.id.nav_articles)  fragment = new ArticlesFragment();
            if (fragment != null) { loadFragment(fragment); return true; }
            return false;
        });
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).commit();
    }

    /** Naviguer vers le Journal depuis n'importe quel fragment */
    public void ouvrirJournal() {
        loadFragment(new JournalFragment());
    }

    public void setSelectedTab(int itemId) { bottomNav.setSelectedItemId(itemId); }
}
