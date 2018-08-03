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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public abstract class Settings {

    private static final String TAG = Settings.class.getSimpleName();

    // Don't instantiate
    private Settings() {}

    /**
     * Name of the user, used for the 'CREATOR' field when creating new entries
     */
    public static final String WHOAMI = "whoami";

    /**
     * Remote server url that hosts the purchase list
     */
    public static final String SERVER_URL = "server_url";

    /**
     * Username used for server access if required
     */
    public static final String SERVER_LOGIN_USERNAME = "server_login_username";

    /**
     * Password used for server access if required
     */
    public static final String SERVER_LOGIN_PASSWORD = "server_login_password";

    /**
     * Which app theme to use
     */
    public static final String THEME = "theme";

    /**
     * SimpleDateFormat for the creation date displayed in the list
     */
    public static final String LIST_ITEM_CREATION_DATE_FORMAT = "list_item_creation_date_format";

    /**
     * SimpleDateFormat for the last update time
     */
    public static final String UPDATE_TIME_FORMAT = "update_time_format";

    /**
     * Dino
     */
    public static final String DINO = "dino";

    /**
     * Sort order
     * 0: Latest first
     * 1: Oldest first
     * 2: Alphabetically
     */
    public static final String SORT_ORDER = "sort_order";

    /**
     * Sort order for completed items, analog to SORT_ORDER
     */
    public static final String SORT_ORDER_COMPLETED = "sort_order_completed";

    /**
     * Simulate slow internet
     */
    public static final String SIMULATE_SLOW_INTERNET = "simulate_slow_internet";

    /**
     * Show a demo list instead of connecting to the server
     */
    public static final String DEMO_LIST = "demo_list";



    public static class ThemeNoActionBar {
        public static final int LIGHT = R.style.AppThemeLight_NoActionBar;
        public static final int DARK = R.style.AppThemeDark_NoActionBar;
    }

    public static final int[] themesNoActionBar = new int[] {
            ThemeNoActionBar.LIGHT,
            ThemeNoActionBar.DARK,
    };

    public static int getThemeNoActionBarRes(int theme) {
        if (theme < 0 || theme >= themesNoActionBar.length) {
            theme = 0;
        }
        return themesNoActionBar[theme];
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getInt(Context context, String key) {
        switch (key) {
            case THEME:
                return getIntFromStringPref(context, key, 0);
            case SORT_ORDER:
            case SORT_ORDER_COMPLETED:
                return getSharedPreferences(context).getInt(key, 0);
            default:
                Log.e(TAG, "getInt: unknown key " + key);
                return 0;
        }
    }

    private static int getIntFromStringPref(Context context, String key, int defaultValue) {
        try {
            return Integer.parseInt(getSharedPreferences(context)
                    .getString(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            Log.e(TAG, "getIntFromStringPref for key \"" + key + "\": " + e);
            return defaultValue;
        }
    }

    public static void putInt(Context context, String key, int value) {
        getSharedPreferences(context).edit().putInt(key, value).apply();
    }

    public static String getString(Context context, String key) {
        switch (key) {
            case WHOAMI:
            case SERVER_URL:
            case SERVER_LOGIN_USERNAME:
            case SERVER_LOGIN_PASSWORD:
            case LIST_ITEM_CREATION_DATE_FORMAT:
            case UPDATE_TIME_FORMAT:
                return getSharedPreferences(context).getString(key, "");
            default:
                Log.e(TAG, "getString: unknown key " + key);
                return null;
        }
    }

    public static boolean getBoolean(Context context, String key) {
        switch (key) {
            case DINO:
            case SIMULATE_SLOW_INTERNET:
            case DEMO_LIST:
                return getSharedPreferences(context).getBoolean(key, false);
            default:
                Log.e(TAG, "getBoolean: unknown key " + key);
                return false;
        }
    }
}
