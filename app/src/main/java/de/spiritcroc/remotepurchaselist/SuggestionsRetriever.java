/*
 * Copyright (C) 2018-2019 SpiritCroc
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
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SuggestionsRetriever {

    private static final String TAG = SuggestionsRetriever.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final AtomicBoolean sUpdateNeeded = new AtomicBoolean(true);
    private static final ReentrantLock sPrefLock = new ReentrantLock();

    public static String[] getCachedSuggestions(Context context) {
        return Settings.getStringArray(context, Settings.NAME_SUGGESTIONS);
    }

    public static void updateSuggestions(Context context) {
        if (sUpdateNeeded.compareAndSet(true, false)) {
            new UpdateTask(context).execute();
        }
    }

    private static class UpdateTask extends AsyncTask<Void, Void, JSONObject> {
        Context mContext;
        public UpdateTask(Context context) {
            mContext = context;
        }
        @Override
        protected JSONObject doInBackground(Void... args) {
            int maxSuggestionCount = Settings.getInt(mContext, Settings.NAME_SUGGESTION_LIMIT);
            String requestParameters = ServerCommunicator.initializeParameter(mContext);
            requestParameters = ServerCommunicator.addParameter(requestParameters,
                    Constants.JSON.LIMIT, maxSuggestionCount + "");
            try {
                return ServerCommunicator.requestHttp(mContext, Constants.SITE.GET_SUGGESTIONS,
                        requestParameters);
            } catch (Exception e) {
                Log.w(TAG, "Error during http request", e);
                return null;
            }
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            try {
                if (result == null || result.getInt(Constants.JSON.SUCCESS) == 0) {
                    // Not successful
                    Log.d(TAG, "Update task failed");
                    sUpdateNeeded.set(true);
                    return;
                }
                if (!setCachedSuggestions(mContext, result)) {
                    sUpdateNeeded.set(true);
                }/* else if (DEBUG_VERBOSE) {
                    String[] debug = getCachedSuggestions(mContext);
                    Log.d(TAG, "Cached suggestions: " + debug.length);
                    for (String d: debug) {
                        Log.d(TAG, "\t" + d);
                    }
                }*/
            } catch (Exception e) {
                Log.e(TAG, "Update task failed", e);
                sUpdateNeeded.set(true);
            }
        }
    }

    private static boolean setCachedSuggestions(Context context, JSONObject json) {
        try {
            JSONArray jItems = json.getJSONArray(Constants.JSON.ITEMS);
            int size = jItems.length();
            String[] names = new String[size];
            String[] infos = new String[size];
            String[] pictureUrls = new String[size];
            for (int i = 0; i < size; i++) {
                JSONObject jItem = jItems.getJSONObject(i);
                names[i] = jItem.getString(Constants.JSON.NAME);
                if (jItem.has(Constants.JSON.INFO)) {
                    infos[i] = jItem.getString(Constants.JSON.INFO);
                } else {
                    infos[i] = "";
                }
                if (jItem.has(Constants.JSON.PICTURE_URL)) {
                    pictureUrls[i] = jItem.getString(Constants.JSON.PICTURE_URL);
                } else {
                    pictureUrls[i] = "";
                }
            }
            sPrefLock.lock();
            try {
                Settings.putStringArray(context, Settings.NAME_SUGGESTIONS, names);
                Settings.putStringArray(context, Settings.INFO_SUGGESTIONS, infos);
                Settings.putStringArray(context, Settings.PICTURE_URL_SUGGESTIONS, pictureUrls);
            } finally {
                sPrefLock.unlock();
            }
            if (DEBUG) {
                Log.d(TAG, "Retrieved " + size + " suggestions");
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Loading suggestions failed", e);
            return false;
        }
    }

    /**
     * @return
     * An item containing some suggestion auto-completion information (info, pictureUrl).
     * Do not use for other purposes than auto-completion, since item has no complete information!
     */
    public static Item getSuggestionItemInfosByName(Context context, String name) {
        Item item = new Item();
        item.name = name;
        if (TextUtils.isEmpty(name)) {
            return item;
        }
        if (sPrefLock.tryLock()) {
            String[] names;
            String[] infos;
            String[] pictureUrls;
            try {
                names = getCachedSuggestions(context);
                infos = Settings.getStringArray(context, Settings.INFO_SUGGESTIONS);
                pictureUrls = Settings.getStringArray(context, Settings.PICTURE_URL_SUGGESTIONS);
            } finally {
                sPrefLock.unlock();
            }
            if (names.length != infos.length) {
                Log.e(TAG, "Inconsistent suggestions: names " + names.length + ", infos: " +
                        infos.length);
                return item;
            }
            if (names.length != pictureUrls.length) {
                Log.e(TAG, "Inconsistent suggestions: names " + names.length + ", pictureUrls: " +
                        pictureUrls.length);
                return item;
            }
            for (int i = 0; i < names.length; i++) {
                if (name.equals(names[i])) {
                    item.info = infos[i];
                    item.pictureUrl = pictureUrls[i];
                    return item;
                }
            }
        } else {
            Log.d(TAG, "skip returning corresponding info suggestion to avoid lock");
        }
        return item;
    }
}
