/*
 * Copyright (C) 2019 SpiritCroc
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
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class UsageSuggestionsRetriever {

    private static final String TAG = UsageSuggestionsRetriever.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final AtomicBoolean sUpdateNeeded = new AtomicBoolean(true);
    private static final ReentrantLock sPrefLock = new ReentrantLock();

    public static String[] getCachedSuggestions(Context context) {
        return Settings.getStringArray(context, Settings.USAGE_SUGGESTIONS);
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
            String requestParameters = ServerCommunicator.initializeParameter(mContext);
            try {
                return ServerCommunicator.requestHttp(mContext, Constants.SITE.GET_AVAILABLE_USAGES,
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
            String[] usages = new String[size];
            for (int i = 0; i < size; i++) {
                JSONObject jItem = jItems.getJSONObject(i);
                usages[i] = jItem.getString(Constants.JSON.USAGE);
            }
            sPrefLock.lock();
            try {
                Settings.putStringArray(context, Settings.USAGE_SUGGESTIONS, usages);
            } finally {
                sPrefLock.unlock();
            }
            if (DEBUG) {
                Log.d(TAG, "Retrieved " + size + " usage suggestions");
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Loading usage suggestions failed", e);
            return false;
        }
    }

}
