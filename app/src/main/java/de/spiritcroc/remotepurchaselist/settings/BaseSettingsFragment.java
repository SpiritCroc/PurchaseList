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

import android.app.Activity;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;

public abstract class BaseSettingsFragment extends PreferenceFragment {
    private static final String LOG_TAG = BaseSettingsFragment.class.getSimpleName();

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            return ((SettingsActivity) activity).onPreferenceClick(preference) ||
                    super.onPreferenceTreeClick(preferenceScreen, preference);
        } else {
            Log.w(LOG_TAG, "activity not instanceof SettingsActivity");
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }


    protected void setValueToSummary(ListPreference preference) {
        preference.setSummary(preference.getEntry());
    }

    protected void setValueToSummary(EditTextPreference preference) {
        preference.setSummary(preference.getText());
    }
}
