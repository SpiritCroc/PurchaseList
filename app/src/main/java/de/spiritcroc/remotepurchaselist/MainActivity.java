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

package de.spiritcroc.remotepurchaselist;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

public class MainActivity extends BaseActivity {

    public static final String FRAGMENT_CLASS = MainActivity.class.getName() + ".FragmentClass";

    private static final String FRAGMENT_TAG = MainActivity.class.getName() + ".Fragment";
    private static final String DISPLAY_HOME_AS_UP = MainActivity.class.getName() + ".homeAsUp";

    private String mFragmentTag;
    private Fragment mFragment;
    private boolean mDisplayHomeAsUp = false;

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_toolbar);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (savedInstanceState != null) {
            mFragmentTag = savedInstanceState.getString(FRAGMENT_TAG);
            mDisplayHomeAsUp = savedInstanceState.getBoolean(DISPLAY_HOME_AS_UP);
        }
        mFragment = getFragmentManager().findFragmentByTag(mFragmentTag);
        if (mFragment == null) {
            mFragment = getNewFragment();
            mFragmentTag = getFragmentTag(mFragment);
            getFragmentManager().beginTransaction().replace(R.id.content, mFragment, mFragmentTag)
                    .commit();
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(mDisplayHomeAsUp);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(FRAGMENT_TAG, mFragmentTag);
        outState.putBoolean(DISPLAY_HOME_AS_UP, mDisplayHomeAsUp);
    }

    protected Fragment getNewFragment() {
        String fragmentClassExtra = getIntent().getStringExtra(FRAGMENT_CLASS);
        Fragment fragment = null;
        if (fragmentClassExtra != null && !TextUtils.isEmpty(fragmentClassExtra)) {
            try {
                Class<?> fragmentClass = Class.forName(fragmentClassExtra);
                fragment = (Fragment) fragmentClass.newInstance();
                mDisplayHomeAsUp = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fragment == null) {
            fragment = new ShowItemsFragment();
        }
        return fragment;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mToolbar.setTitle(title);
    }

    private static String getFragmentTag(Fragment fragment) {
        return MainActivity.class.getName() + "." + fragment.getClass().getName();
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

    @Override
    public void onBackPressed() {
        if (mFragment instanceof BackButtonHandler &&
                ((BackButtonHandler) mFragment).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}
