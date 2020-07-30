/*
 * Copyright (C) 2017-2019 SpiritCroc
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class HttpPostOfflineCache {

    private static final String TAG = HttpPostOfflineCache.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String PREF_CACHED_INSTRUCTIONS_COUNT = "http_post_cache_count";
    private static final String PREF_CACHED_INSTRUCTION_SITE_ = "http_post_cache_site_";
    private static final String PREF_CACHED_INSTRUCTION_PARAMS_ = "http_post_cache_params_";
    private static final String PREF_CACHED_PREVIEWS_COUNT = "http_post_cache_preview_count";
    private static final String PREF_CACHED_PREVIEW_ = "http_post_cache_preview_";
    private static final String PREF_CACHED_REMOVE_PREVIEW = "http_post_cache_remove_preview";
    private static final String PREF_CACHED_REMOVE_PREVIEW_COUNT =
            "http_post_cache_remove_preview_count";
    private static final String PREF_CACHED_REMOVE_PREVIEW_ = "http_post_cache_remove_preview_";
    private static final String PREF_CACHED_DELETE_PREVIEW= "http_post_cache_deleted_preview";
    private static final String PREF_CACHED_INSTRUCTION_MULTIPARAM_KIND_ =
            "http_post_cache_multiparam_kind_";
    private static final String PREF_CACHED_INSTRUCTION_MULTIPARAM_KEY_ =
            "http_post_cache_multiparam_key_";
    private static final String PREF_CACHED_INSTRUCTION_MULTIPARAM_VALUE_ =
            "http_post_cache_multiparam_value_";
    private static final String CACHED_INSTRUCTION_PARAM_MULTIPART_INDICATOR = "䷼";
    private static final int CACHED_INSTRUCTION_MULTIPARAM_KIND_STRING = 0;
    private static final int CACHED_INSTRUCTION_MULTIPARAM_KIND_FILE = 1;

    // Don't instantiate
    private HttpPostOfflineCache() {}

    public static void clearCache(Context context) {
        clearInstructionsCache(context);
        clearItemCache(context);
        clearRemoveCache(context);
        clearDeleteCache(context);
        clearPictureCache(context);
    }

    public static void clearPreviewCacheIfInstructionsEmpty(Context context) {
        if (getCachedInstructionsCount(context) == 0) {
            // No instructions left -> empty preview cache
            clearItemCache(context);
            clearRemoveCache(context);
            clearDeleteCache(context);
            // Actually, we can't be sure that pictures are unused. Consider following scenario:
            // 1. Import picture locally
            // 2. Reload list in background
            // 3. Add instruction to upload picture
            // In this case, we have to ensure 2. doesn't delete the picture cache.
            /*
            clearPictureCache(context);
            */
        }
    }
    private static void addItemPreviewToCache(Item cachePreview, SharedPreferences sp,
                                              SharedPreferences.Editor e, int instructionCount) {
        if (DEBUG) Log.d(TAG, "Adding preview " + cachePreview.toString());
        if (!cachePreview.isCompleted()) {
            // Normal (open tasks) cache
            int previewCount = sp.getInt(PREF_CACHED_PREVIEWS_COUNT, instructionCount);
            cachePreview.saveToCachePreferences(e, PREF_CACHED_PREVIEW_ + previewCount);
            e.putInt(PREF_CACHED_PREVIEWS_COUNT, ++previewCount);
        } else {
            // Completed tasks cache
            int previewCount = sp.getInt(PREF_CACHED_REMOVE_PREVIEW_COUNT, instructionCount);
            cachePreview.saveToCachePreferences(e, PREF_CACHED_REMOVE_PREVIEW_ + previewCount);
            e.putInt(PREF_CACHED_REMOVE_PREVIEW_COUNT, ++previewCount);
        }
    }

    public static void addItemToCache(Context context, String site, String params,
                                      Item cachePreview) {
        if (Settings.getBoolean(context, Settings.DEMO_LIST)) {
            // Don't modify demo list
            return;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int instructionCount = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        if (DEBUG) Log.d(TAG, "Adding request to " + site + " with params " + params + " - "
                + instructionCount);
        SharedPreferences.Editor e = sp.edit();
        // Add instructions to cache
        e.putString(PREF_CACHED_INSTRUCTION_SITE_ + instructionCount, site)
                .putString(PREF_CACHED_INSTRUCTION_PARAMS_ + instructionCount, params);
        // Add preview to cache
        if (cachePreview != null) {
            addItemPreviewToCache(cachePreview, sp, e, instructionCount);
        }
        e.putInt(PREF_CACHED_INSTRUCTIONS_COUNT, ++instructionCount).apply();
    }

    private static String getMultipartParamKindKey(int instructionCount, int paramCount) {
        return PREF_CACHED_INSTRUCTION_MULTIPARAM_KIND_ + instructionCount + "_" + paramCount;
    }

    private static String getMultipartParamKeyKey(int instructionCount, int paramCount) {
        return PREF_CACHED_INSTRUCTION_MULTIPARAM_KEY_ + instructionCount + "_" + paramCount;
    }

    private static String getMultipartParamValueKey(int instructionCount, int paramCount) {
        return PREF_CACHED_INSTRUCTION_MULTIPARAM_VALUE_ + instructionCount + "_" + paramCount;
    }

    public static void addItemToCache(Context context, String site,
                                      List<MultiPartRequestParameter> params, Item cachePreview) {
        if (Settings.getBoolean(context, Settings.DEMO_LIST)) {
            // Don't modify demo list
            return;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int instructionCount = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        if (DEBUG) Log.d(TAG, "Adding request to " + site + " with multipart parameters");
        SharedPreferences.Editor e = sp.edit();
        // Add instructions to cache
        e.putString(PREF_CACHED_INSTRUCTION_SITE_ + instructionCount, site)
                .putString(PREF_CACHED_INSTRUCTION_PARAMS_ + instructionCount,
                        CACHED_INSTRUCTION_PARAM_MULTIPART_INDICATOR);
        MultiPartRequestParameter.store(e, instructionCount, params);
        // Add preview to cache
        if (cachePreview != null) {
            addItemPreviewToCache(cachePreview, sp, e, instructionCount);
        }
        e.putInt(PREF_CACHED_INSTRUCTIONS_COUNT, ++instructionCount).apply();
    }


    public static void addItemsToRemoveCache(Context context, String site, String params,
                                             List<Item> removeItems) {
        if (Settings.getBoolean(context, Settings.DEMO_LIST)) {
            // Don't modify demo list
            return;
        }
        // Add instructions to cache
        addItemToCache(context, site, params, null);
        // Add preview to cache
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String cache = sp.getString(PREF_CACHED_REMOVE_PREVIEW, "");
        if (TextUtils.isEmpty(cache)) {
            cache = "";
        }
        // Handle ids separately / additionally to full entry for backwards compatibility
        StringBuilder ids = new StringBuilder(cache);
        int previewCount = sp.getInt(PREF_CACHED_REMOVE_PREVIEW_COUNT, 0);
        SharedPreferences.Editor e = sp.edit();
        for (Item removeItem: removeItems) {
            ids.append(removeItem.id);
            ids.append(';');
            removeItem.saveToCachePreferences(e, PREF_CACHED_REMOVE_PREVIEW_ + previewCount);
            previewCount++;
        }

        e.putInt(PREF_CACHED_REMOVE_PREVIEW_COUNT, previewCount);

        cache = ids.toString();
        if (DEBUG) Log.d(TAG, "Updated remove cache to " + cache);
        e.putString(PREF_CACHED_REMOVE_PREVIEW, cache);
        e.apply();
    }

    public static void addItemToDeleteCache(Context context, String site, String params, long id) {
        if (Settings.getBoolean(context, Settings.DEMO_LIST)) {
            // Don't modify demo list
            return;
        }
        // Add instructions to cache
        addItemToCache(context, site, params, null);
        // Add preview to cache
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String cache = sp.getString(PREF_CACHED_DELETE_PREVIEW, "");
        if (TextUtils.isEmpty(cache)) {
            cache = "";
        }
        StringBuilder ids = new StringBuilder(cache);
        SharedPreferences.Editor e = sp.edit();
        ids.append(id);
        ids.append(';');

        cache = ids.toString();
        if (DEBUG) Log.d(TAG, "Updated delete cache to " + cache);
        e.putString(PREF_CACHED_DELETE_PREVIEW, cache);
        e.apply();
    }

    private static void clearInstructionsCache(Context context) {
        if (DEBUG) Log.d(TAG, "Clearing instructions cache");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = sp.edit();
        int instructionCount = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        for (int i = 0; i < instructionCount; i++) {
            e.remove(PREF_CACHED_INSTRUCTION_SITE_ + i);
            e.remove(PREF_CACHED_INSTRUCTION_PARAMS_ + i);
            clearMultipartParams(sp, e, instructionCount);
        }
        e.putInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        e.apply();
    }

    private static void clearMultipartParams(SharedPreferences sp, SharedPreferences.Editor e,
                                             int instructionCount) {
        int paramCount = 0;
        while (sp.contains(getMultipartParamKindKey(instructionCount, paramCount))) {
            e.remove(getMultipartParamKindKey(instructionCount, paramCount));
            e.remove(getMultipartParamKeyKey(instructionCount, paramCount));
            e.remove(getMultipartParamValueKey(instructionCount, paramCount));
            paramCount++;
        }
    }

    private static void clearItemCache(Context context) {
        if (DEBUG) Log.d(TAG, "Clearing item cache");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = sp.edit();
        // For versions < 2, cachedPreviewsCount = cachedInstructionsCount
        int previewCount = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        previewCount = sp.getInt(PREF_CACHED_PREVIEWS_COUNT, previewCount);
        for (int i = 0; i < previewCount; i++) {
            Item.deleteCachePreference(e, PREF_CACHED_PREVIEW_ + i);
        }
        e.putInt(PREF_CACHED_PREVIEWS_COUNT, 0);
        previewCount = sp.getInt(PREF_CACHED_REMOVE_PREVIEW_COUNT, 0);
        for (int i = 0; i < previewCount; i++) {
            Item.deleteCachePreference(e, PREF_CACHED_REMOVE_PREVIEW_ + i);
        }
        e.putInt(PREF_CACHED_REMOVE_PREVIEW_COUNT, 0);
        e.apply();
    }

    private static void clearRemoveCache(Context context) {
        if (DEBUG) Log.d(TAG, "Clearing remove cache");
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(PREF_CACHED_REMOVE_PREVIEW).apply();
    }

    private static void clearDeleteCache(Context context) {
        if (DEBUG) Log.d(TAG, "Clearing delete cache");
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .remove(PREF_CACHED_DELETE_PREVIEW).apply();
    }

    private static void clearPictureCache(Context context) {
        LocalPictureHandler.clear(context);
    }

    public static void executePending(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int instructionsCount = sp.getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
        if (DEBUG) Log.d(TAG, "Cached instructions: " + instructionsCount);
        ArrayList<Integer> stillInCache = new ArrayList<>();
        ServerCommunicator.OnFileUploadListener fileUploadListener =
                new ServerCommunicator.OnFileUploadListener() {
                    private ArrayList<File> removeFiles = new ArrayList<>();
                    @Override
                    public void onAttemptFileUpload(File file) {
                        removeFiles.add(file);
                    }
                    @Override
                    public void onUploadDone() {
                        for (File file: removeFiles) {
                            LocalPictureHandler.removeLocalPicture(file.toURI().toString());
                        }
                    }
        };
        // Execute pending
        for (int i = 0; i < instructionsCount; i++) {
            String site = sp.getString(PREF_CACHED_INSTRUCTION_SITE_ + i, null);
            if (site == null) {
                Log.e(TAG, "Pending instruction " + i + " is broken, discarding execution");
                continue;
            }
            String params = sp.getString(PREF_CACHED_INSTRUCTION_PARAMS_ + i, null);
            JSONObject result;
            if (CACHED_INSTRUCTION_PARAM_MULTIPART_INDICATOR.equals(params)) {
                List<MultiPartRequestParameter> multipartParams =
                        MultiPartRequestParameter.restore(sp, i);
                result = ServerCommunicator.requestHttp(context, site, multipartParams,
                        fileUploadListener);
            } else {
                result = ServerCommunicator.requestHttp(context, site, params, fileUploadListener);
            }
            if (result == null) {
                stillInCache.add(i);
            } else if (DEBUG) {
                // Check if success, keep if failed for further inspection
                try {
                    if (result.getInt(Constants.JSON.SUCCESS) == 0) {
                        Log.e(TAG, "That was not a success! Keeping in cache for DEBUG purposes " +
                                "(keep in mind this will be discarded on debug builds!)");
                        stillInCache.add(i);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // Update cache
        SharedPreferences.Editor e = sp.edit();
        for (int i = 0; i < instructionsCount; i++) {
            if (i < stillInCache.size()) {
                int from = stillInCache.get(i);
                if (from == i) {
                    if (DEBUG) Log.d(TAG, "Keep " + from + " in cache");
                } else {
                    if (DEBUG) Log.d(TAG, "Move instruction cache " + from + " -> " + i);
                    e.putString(PREF_CACHED_INSTRUCTION_SITE_ + i,
                            sp.getString(PREF_CACHED_INSTRUCTION_SITE_ + from, null));
                    e.putString(PREF_CACHED_INSTRUCTION_PARAMS_ + i,
                            sp.getString(PREF_CACHED_INSTRUCTION_PARAMS_ + from, null));
                    // MultipartParams from
                    List<MultiPartRequestParameter> multipartParams =
                            MultiPartRequestParameter.restore(sp, from);
                    // MultipartParams to
                    MultiPartRequestParameter.store(e, i, multipartParams);
                }
            } else {
                if (DEBUG) Log.d(TAG, "Delete instruction cache " + i);
                e.remove(PREF_CACHED_INSTRUCTION_SITE_ + i);
                e.remove(PREF_CACHED_INSTRUCTION_PARAMS_ + i);
                clearMultipartParams(sp, e, i);
            }
        }
        e.putInt(PREF_CACHED_INSTRUCTIONS_COUNT, stillInCache.size());
        e.apply();
    }

    public static int getCachedInstructionsCount(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_CACHED_INSTRUCTIONS_COUNT, 0);
    }

    static Item[] previewCache(Context context, Item[] onlineItems) {
        return previewCache(context, onlineItems, PREF_CACHED_INSTRUCTIONS_COUNT,
                PREF_CACHED_PREVIEWS_COUNT, PREF_CACHED_PREVIEW_, PREF_CACHED_REMOVE_PREVIEW);
    }

    static Item[] previewCompletedCache(Context context, Item[] onlineItems) {
        return previewCache(context, onlineItems, PREF_CACHED_REMOVE_PREVIEW_COUNT, null,
                PREF_CACHED_REMOVE_PREVIEW_, PREF_CACHED_DELETE_PREVIEW);
    }

    private static Item[] previewCache(Context context, Item[] onlineItems, String countPref,
                                      String newerCountPref, String cachePrefPrefix,
                                      String excludeIdsPref) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        // For versions < 2, cachedPreviewsCount = cachedInstructionsCount
        int previewCount = sp.getInt(countPref, 0);
        if (newerCountPref != null) {
            previewCount = sp.getInt(newerCountPref, previewCount);
        }
        if (DEBUG) Log.d(TAG, "previewCache: overlay " + previewCount + " items");
        ArrayList<Item> items = new ArrayList<>();
        items.addAll(Arrays.asList(onlineItems));

        for (int i = 0; i < previewCount; i++) {
            Item overlay = Item.loadFromCachePreferences(sp, cachePrefPrefix + i);
            if (overlay == null) {
                Log.w(TAG, "previewCache: preview " + i + " is broken; skipping");
                continue;
            }
            if (items.contains(overlay)) {
                if (DEBUG) Log.d(TAG, "previewCache: remove old " + overlay);
                // Already an item in the list with the same ID, remove it for the preview
                items.remove(overlay);
            }
            if (DEBUG) Log.d(TAG, "previewCache: add " + overlay);
            // Add it at the beginning
            items.add(0, overlay);
        }

        if (excludeIdsPref != null) {
            String removedPreviewsString = sp.getString(excludeIdsPref, "");
            if (!TextUtils.isEmpty(removedPreviewsString)) {
                String[] removedPreviews = removedPreviewsString.split(";");
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
        }

        return items.toArray(new Item[items.size()]);
    }

    public static class MultiPartRequestParameter
            extends ServerCommunicator.MultiPartRequestParameter {
        public MultiPartRequestParameter(String key, Object value) {
            super(key, value);
        }
        private void store(SharedPreferences.Editor e, int instructionCount, int paramCount) {
            String persistVal;
            if (value instanceof String) {
                persistVal = (String) value;
                e.putInt(getMultipartParamKindKey(instructionCount, paramCount),
                        CACHED_INSTRUCTION_MULTIPARAM_KIND_STRING);
            } else if (value instanceof File) {
                persistVal = ((File) value).toURI().toString();
                e.putInt(getMultipartParamKindKey(instructionCount, paramCount),
                        CACHED_INSTRUCTION_MULTIPARAM_KIND_FILE);
            } else {
                persistVal = String.valueOf(value);
                e.putInt(getMultipartParamKindKey(instructionCount, paramCount),
                        CACHED_INSTRUCTION_MULTIPARAM_KIND_STRING);
            }
            e.putString(getMultipartParamKeyKey(instructionCount, paramCount), key);
            e.putString(getMultipartParamValueKey(instructionCount, paramCount), persistVal);
        }
        public static void store(SharedPreferences.Editor e, int instructionCount,
                                 List<MultiPartRequestParameter> params) {
            int paramCount = 0;
            for (MultiPartRequestParameter param: params) {
                param.store(e, instructionCount, paramCount++);
            }
            // null-terminate
            e.remove(getMultipartParamKindKey(instructionCount, paramCount));
            e.remove(getMultipartParamKeyKey(instructionCount, paramCount));
            e.remove(getMultipartParamValueKey(instructionCount, paramCount));
        }
        public static List<MultiPartRequestParameter> restore(SharedPreferences sp,
                                                              int instructionCount) {
            int paramCount = 0;
            ArrayList<MultiPartRequestParameter> result = new ArrayList<>();
            MultiPartRequestParameter next = restore(sp, instructionCount, paramCount);
            while (next != null) {
                result.add(next);
                next = restore(sp, instructionCount, ++paramCount);
            }
            return result;
        }
        private static MultiPartRequestParameter restore(SharedPreferences sp, int instructionCount,
                                                 int paramCount) {
            int kind = sp.getInt(getMultipartParamKindKey(instructionCount, paramCount), -1);
            String persistVal = sp.getString(getMultipartParamValueKey(instructionCount, paramCount),
                    "");
            String key = sp.getString(getMultipartParamKeyKey(instructionCount, paramCount), null);
            if (TextUtils.isEmpty(key)) {
                return null;
            }
            Object value;
            switch (kind) {
                case CACHED_INSTRUCTION_MULTIPARAM_KIND_STRING:
                    value = persistVal;
                    break;
                case CACHED_INSTRUCTION_MULTIPARAM_KIND_FILE:
                    value = LocalPictureHandler.fileFromURI(persistVal);
                    break;
                default:
                    return null;
            }
            return new MultiPartRequestParameter(key, value);
        }
    }
}
