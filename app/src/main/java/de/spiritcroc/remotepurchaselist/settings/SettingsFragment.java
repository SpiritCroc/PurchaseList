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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;

import at.bitfire.cert4android.CustomCertManager;
import de.spiritcroc.remotepurchaselist.HttpPostOfflineCache;
import de.spiritcroc.remotepurchaselist.R;
import de.spiritcroc.remotepurchaselist.Settings;

public class SettingsFragment extends BaseSettingsFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener{

    private static final String DISCARD_CACHED_INSTRUCTIONS = "discard_cached_instructions";
    private static final String CLEAR_TRUSTED_CERTIFICATES = "clear_trusted_certificates";

    private EditTextPreference mWhoami;
    private EditTextPreference mServerUrl;
    private EditTextPreference mServerUsername;
    private EditTextPreference mServerPassword;
    private ListPreference mTheme;
    private Preference mDiscardCachedInstructions;
    private Preference mClearTrustedCertificates;

    private Snackbar mSnackbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mWhoami = (EditTextPreference) findPreference(Settings.WHOAMI);
        mServerUrl = (EditTextPreference) findPreference(Settings.SERVER_URL);
        mServerUsername = (EditTextPreference) findPreference(Settings.SERVER_LOGIN_USERNAME);
        mServerPassword = (EditTextPreference) findPreference(Settings.SERVER_LOGIN_PASSWORD);
        mTheme = (ListPreference) findPreference(Settings.THEME);
        mDiscardCachedInstructions = findPreference(DISCARD_CACHED_INSTRUCTIONS);
        mClearTrustedCertificates = findPreference(CLEAR_TRUSTED_CERTIFICATES);

        mDiscardCachedInstructions.setOnPreferenceClickListener(this);
        mClearTrustedCertificates.setOnPreferenceClickListener(this);
    }

    private void init() {
        setValueToSummary(mWhoami);
        setValueToSummary(mServerUrl);
        updateServerUsername();
        updateServerPassword();
        setValueToSummary(mTheme);
        updateDiscardCachedInstructions();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Settings.WHOAMI.equals(key)) {
            setValueToSummary(mWhoami);
        } else if (Settings.SERVER_URL.equals(key)) {
            updateServerUrl();
        } else if (Settings.SERVER_LOGIN_USERNAME.equals(key)) {
            updateServerUsername();
        } else if (Settings.SERVER_LOGIN_PASSWORD.equals(key)) {
            updateServerPassword();
        } else if (Settings.THEME.equals(key)) {
            getActivity().recreate();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mDiscardCachedInstructions) {
            HttpPostOfflineCache.clearCache(getActivity());
            updateDiscardCachedInstructions();
            return true;
        } else if (preference == mClearTrustedCertificates) {
            try {
                CustomCertManager.Companion.resetCertificates(getActivity());
                showSnackbar(getString(R.string.pref_clear_trusted_certificates_toast_success),
                        Snackbar.LENGTH_SHORT);
            } catch (Exception e) {
                e.printStackTrace();
                showSnackbar(getString(R.string.pref_clear_trusted_certificates_toast_failure),
                        Snackbar.LENGTH_SHORT);
            }
            return true;
        } else {
            return false;
        }
    }

    private void updateServerUrl() {
        String url = mServerUrl.getText();
        if (url.length() > 0 && url.charAt(url.length()-1) != '/') {
            // Ensure a trailing slash: needed for path concatenation as used later
            url += "/";
            mServerUrl.setText(url);
        }
        setValueToSummary(mServerUrl);
    }

    private void updateServerUsername() {
        mServerUsername.setSummary(getString(TextUtils.isEmpty(mServerUsername.getText())
                ? R.string.pref_server_login_username_unset_summary
                : R.string.pref_server_login_username_set_summary, mServerUsername.getText()));
    }

    private void updateServerPassword() {
        mServerPassword.setSummary(getString(TextUtils.isEmpty(mServerPassword.getText())
                ? R.string.pref_server_login_password_unset_summary
                : R.string.pref_server_login_password_set_summary));
    }

    private void updateDiscardCachedInstructions() {
        int count = HttpPostOfflineCache.getCachedInstructionsCount(getActivity());
        mDiscardCachedInstructions.setSummary(
                getString(R.string.pref_discard_cached_instructions_summary, count));
        mDiscardCachedInstructions.setEnabled(count != 0);
    }

    private void showSnackbar(CharSequence text, int duration) {
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
        mSnackbar = Snackbar.make(getView(), text, duration);
        mSnackbar.show();
    }

}
