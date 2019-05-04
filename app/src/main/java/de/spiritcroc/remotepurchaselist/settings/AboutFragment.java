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

import android.os.Bundle;
import android.preference.Preference;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.spiritcroc.remotepurchaselist.BuildConfig;
import de.spiritcroc.remotepurchaselist.R;

public class AboutFragment extends BaseSettingsFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);

        Preference aboutAppVersion = findPreference("about_app");
        if (BuildConfig.DEBUG) {
            aboutAppVersion.setSummary(getString(R.string.about_app_debug_summary,
                    BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.GIT_DESCRIBE,
                    SimpleDateFormat.getDateTimeInstance().format(
                            new Date(BuildConfig.TIMESTAMP))));
        } else {
            aboutAppVersion.setSummary(getString(R.string.about_app_summary,
                    BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        }
    }
}
