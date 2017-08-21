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
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class HttpPostOfflineCache {

    private static final String TAG = HttpPostOfflineCache.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String PREF_CACHED_INSTRUCTIONS_COUNT = "http_post_cache_count";
    private static final String PREF_CACHED_INSTRUCTION_SITE_ = "http_post_cache_site_";
    private static final String PREF_CACHED_INSTRUCTION_PARAMS_ = "http_post_cache_params_";
    private static final String PREF_CACHED_PREVIEW_ = "http_post_cache_preview_";
    private static final String PREF_CACHED_REMOVE_PREVIEW = "http_post_cache_remove_preview";

    // Don't instantiate
    private HttpPostOfflineCache() {}

    public static void clearCache(Context context) {
        clearItemCache(context);
        clearRemoveCache(context);
    }

    public static void addItemToCache(Context context, String site, String params,
                                      Item cachePreview) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int count = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        if (DEBUG) Log.d(TAG, "Adding request to " + site + " with params " + params + " - "
                + count);
        SharedPreferences.Editor e = sp.edit();
        e.putString(PREF_CACHED_INSTRUCTION_SITE_ + count, site)
                .putString(PREF_CACHED_INSTRUCTION_PARAMS_ + count, params);
        if (cachePreview != null) {
            cachePreview.saveToCachePreferences(e, PREF_CACHED_PREVIEW_ + count);
        }
        e.putInt(PREF_CACHED_INSTRUCTIONS_COUNT, ++count).apply();
    }

    private static void clearItemCache(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = sp.edit();
        int count = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        for (int i = 0; i < count; i++) {
            e.remove(PREF_CACHED_INSTRUCTION_SITE_ + i);
            e.remove(PREF_CACHED_INSTRUCTION_PARAMS_ + i);
            Item.deleteCachePreference(e, PREF_CACHED_PREVIEW_ + i);
        }
        e.putInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        e.apply();
    }

    public static void addItemsToRemoveCache(Context context, ArrayList<Long> removedIds) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String cache = sp.getString(PREF_CACHED_REMOVE_PREVIEW, "");
        if (TextUtils.isEmpty(cache)) {
            cache = "";
        }
        for (Long removedId: removedIds) {
            cache += ((long) removedId) + ";";
        }
        sp.edit().putString(PREF_CACHED_REMOVE_PREVIEW, cache).apply();
    }

    private static void clearRemoveCache(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(PREF_CACHED_REMOVE_PREVIEW).apply();
    }

    public static void executePending(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int count = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        if (DEBUG) Log.d(TAG, "Cached items: " + count);
        ArrayList<Integer> stillInCache = new ArrayList<>();
        // Execute pending
        for (int i = 0; i < count; i++) {
            String site = sp.getString(PREF_CACHED_INSTRUCTION_SITE_ + i, null);
            if (site == null) {
                Log.e(TAG, "Pending " + i + " is broken, discarding execution");
                continue;
            }
            String params = sp.getString(PREF_CACHED_INSTRUCTION_PARAMS_ + i, null);
            JSONObject result = ServerCommunicator.requestHttp(context, site, params, null, null);
            if (result == null) {
                stillInCache.add(i);
            }
        }
        // Update cache
        SharedPreferences.Editor e = sp.edit();
        for (int i = 0; i < count; i++) {
            if (i < stillInCache.size()) {
                int from = stillInCache.get(i);
                if (from == i) {
                    if (DEBUG) Log.d(TAG, "Keep " + from + " in cache");
                } else {
                    if (DEBUG) Log.d(TAG, "Move cache " + from + " -> " + i);
                    e.putString(PREF_CACHED_INSTRUCTION_SITE_ + i,
                            sp.getString(PREF_CACHED_INSTRUCTION_SITE_ + from, null));
                    e.putString(PREF_CACHED_INSTRUCTION_PARAMS_ + i,
                            sp.getString(PREF_CACHED_INSTRUCTION_PARAMS_ + from, null));
                    Item moveItem = Item.loadFromCachePreferences(sp, PREF_CACHED_PREVIEW_ + from);
                    if (moveItem != null) {
                        moveItem.saveToCachePreferences(e, PREF_CACHED_PREVIEW_ + i);
                    }
                }
            } else {
                if (DEBUG) Log.d(TAG, "Delete cache " + i);
                e.remove(PREF_CACHED_INSTRUCTION_SITE_ + i);
                e.remove(PREF_CACHED_INSTRUCTION_PARAMS_ + i);
                Item.deleteCachePreference(e, PREF_CACHED_PREVIEW_ + i);
            }
        }
        e.putInt(PREF_CACHED_INSTRUCTIONS_COUNT, stillInCache.size());
        e.apply();
    }

    public static int getCachedInstructionsCount(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
    }

    public static Item[] previewCache(Context context, Item[] onlineItems) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int cacheCount = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        if (DEBUG) Log.d(TAG, "previewCache: overlay " + cacheCount + " items");
        if (cacheCount == 0) {
            return onlineItems;
        }
        ArrayList<Item> items = new ArrayList<>();
        items.addAll(Arrays.asList(onlineItems));

        for (int i = 0; i < cacheCount; i++) {
            Item overlay = Item.loadFromCachePreferences(sp, PREF_CACHED_PREVIEW_ + i);
            if (overlay == null) {
                if (DEBUG) Log.d(TAG, "previewCache: instruction " + i + " does not feature a " +
                        "preview item, skipping");
                continue;
            }
            if (items.contains(overlay)) {
                // Already an item in the list with the same ID, remove it for the preview
                items.remove(overlay);
            }
            // Add it at the beginning
            items.add(0, overlay);
        }

        String removedPreviewsString = sp.getString(PREF_CACHED_REMOVE_PREVIEW, "");
        if (!TextUtils.isEmpty(removedPreviewsString)) {
            String[] removedPreviews = sp.getString(PREF_CACHED_REMOVE_PREVIEW, "").split(";");
            if (DEBUG)
                Log.d(TAG, "previewCache: remove " + removedPreviews.length + " items");
            for (int i = 0; i < removedPreviews.length; i++) {
                try {
                    long id = Long.parseLong(removedPreviews[i]);
                    Item removeMe = new Item();
                    removeMe.id = id;
                    if (DEBUG) Log.d(TAG, "previewCache: removing item with id " + id);
                    items.remove(removeMe);
                } catch (Exception e) {
                    Log.e(TAG, "previewCache: removed items list contains invalid item "
                            + removedPreviews[i] + ": " + e);
                }
            }
        }

        return items.toArray(new Item[items.size()]);
    }
}
