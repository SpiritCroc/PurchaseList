/*
 * Copyright (C) 2017 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.remotepurchaselist.settings;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import de.spiritcroc.remotepurchaselist.BaseActivity;
import de.spiritcroc.remotepurchaselist.R;

public class SettingsActivity extends BaseActivity {

    private BaseSettingsFragment preferenceFragment;

    private static final String EXTRA_PREFERENCE_FRAGMENT =
            "de.spiritcroc.ownlog.extra.preference_fragment";
    private static final String EXTRA_PREFERENCE_POSITION =
            "de.spiritcroc.ownlog.extra.preference_position";
    private static final String EXTRA_PREFERENCE_POSITION_TOP =
            "de.spiritcroc.ownlog.extra.preference_position_top";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_toolbar);

        String preferenceKey = getIntent().getStringExtra(EXTRA_PREFERENCE_FRAGMENT);
        preferenceFragment = getNewPreferenceFragment(preferenceKey);
        getFragmentManager().beginTransaction().replace(R.id.content, preferenceFragment).commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(preferenceFragment.getPreferenceScreen().getTitle());
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof PreferenceScreen) {
            String fragmentClass = preference.getFragment();
            if (fragmentClass != null) {
                startActivity(new Intent(this, SettingsActivity.class)
                        .putExtra(EXTRA_PREFERENCE_FRAGMENT, fragmentClass));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private BaseSettingsFragment getNewPreferenceFragment(String preferenceFragment) {
        if (preferenceFragment != null) {
            try {
                return (BaseSettingsFragment) Class.forName(preferenceFragment).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                    ClassCastException e) {
                e.printStackTrace();
            }
        }
        return getDefaultFragment();
    }

    protected BaseSettingsFragment getDefaultFragment() {
        return new SettingsFragment();
    }

    @Override
    public void recreate() {
        // Try to remember position
        View v = findViewById(android.R.id.list);
        if (v instanceof ListView) {
            View child = ((ListView) v).getChildAt(0);
            getIntent().putExtra(EXTRA_PREFERENCE_POSITION_TOP, child == null ? 0 : child.getTop())
                    .putExtra(EXTRA_PREFERENCE_POSITION,
                            ((ListView) v).getFirstVisiblePosition());
        }
        super.recreate();
    }

}
