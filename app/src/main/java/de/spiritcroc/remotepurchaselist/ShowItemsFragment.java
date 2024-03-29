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

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.spiritcroc.remotepurchaselist.settings.AboutActivity;
import de.spiritcroc.remotepurchaselist.settings.SettingsActivity;

public class ShowItemsFragment extends Fragment
        implements EditItemFragment.OnEditItemResultListener,
                ShowItemFragment.ItemInteractionListener,
                ServerCommunicator.OnHttpsSetupFinishListener {

    private static final String TAG = ShowItemsFragment.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String PREF_LAST_LIST = "last_list";

    private static final String FRAGMENT_TAG_EDIT_ITEM_DIALOG = ShowItemsFragment.class.getName() +
            ".edit_item_dialog";

    private static final String FRAGMENT_TAG_SHOW_ITEM_DIALOG = ShowItemsFragment.class.getName() +
            ".show_item_dialog";

    private boolean mDownloading = false;
    private boolean mResumeDownloadNeeded = true;
    private boolean mDirectDownloadNeeded = false;

    private String mServerUrl;
    private String mServerLoginUsername;
    private String mServerLoginPassword;

    // Remember theme, so we re-download on theme change, leading to crash
    // because of activity recreation
    private int mTheme;

    private boolean mPicturePreview = false;

    // Temporary sort order selection
    private int mSortOrder;

    protected ListView mListView;
    protected SwipeRefreshLayout mSwipeRefreshLayout;
    private Snackbar mSnackbar;
    protected ImageView mEmptyImage;

    protected CustomListAdapterInterface<Item> mListAdapter;

    private ActionMode mActionMode;
    private ArrayList<Integer> mSelectedItems = new ArrayList<>();

    // Sometimes http request fails because of server isn't ready yet otherwise
    // Thanks to schaarsc <schaarsc> for https://github.com/stefan-
    // niedermann/nextcloud-notes/commit/b3c2cc2f0e1228b07b55658ff8fa6a244533f5c1
    // (only sync if cert4android service is available)
    //private boolean mCert4androidReady = false;

    private ActionMode.Callback mSelectedItemsActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.fragment_show_items_select, menu);
            if (mSelectedItems.size() == mListAdapter.getCount()) {
                menu.findItem(R.id.action_select_all).setVisible(false);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()){
                case R.id.action_complete:
                    completeSelection();
                    return true;
                case R.id.action_select_all:
                    for (int i = 0; i < mListAdapter.getCount(); i++) {
                        if (!mSelectedItems.contains(i)) {
                            mSelectedItems.add(i);
                        }
                    }
                    mListAdapter.notifyDataSetChanged();
                    updateSelectedItemsActionModeMenu();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mSelectedItems.clear();
            mActionMode = null;
            mListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mTheme = Settings.getInt(getActivity(), Settings.THEME);
        mPicturePreview = Settings.getBoolean(getActivity(), Settings.OVERVIEW_PICTURE_PREVIEW);

        EditItemFragment editItemFragment = (EditItemFragment)
                getFragmentManager().findFragmentByTag(FRAGMENT_TAG_EDIT_ITEM_DIALOG);
        if (editItemFragment != null) {
            editItemFragment.setOnEditResultListener(this);
        }

        ShowItemFragment showItemFragment = (ShowItemFragment)
                getFragmentManager().findFragmentByTag(FRAGMENT_TAG_SHOW_ITEM_DIALOG);
        if (showItemFragment != null) {
            showItemFragment.setItemInteractionListener(this);
        }

        /*
        getActivity().getApplicationContext()
                .bindService(new Intent(getActivity(), CustomCertService.class),
                        new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName componentName,
                                                           IBinder iBinder) {
                                if (DEBUG) Log.d(TAG, "cert4android service connected");
                                mCert4androidReady = true;
                                if (mResumeDownloadNeeded) {
                                    loadContent(true);
                                }
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName componentName) {
                                if (DEBUG) Log.d(TAG, "cert4android service disconnected");
                                mCert4androidReady = false;
                            }
                        }, Context.BIND_AUTO_CREATE);
        */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_show_items, container, false);

        mListView = (ListView) view.findViewById(R.id.item_list);
        if (isReadOnly()) {
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    showItem(mListAdapter.getItem(i));
                }
            });
        } else {
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (i >= mListAdapter.getCount()) {
                        // Account for empty spacer item
                        return;
                    }
                    if (mSelectedItems.isEmpty()) {
                        showItem(mListAdapter.getItem(i));
                    } else {
                        selectItem(i);
                    }
                }
            });
            mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (i >= mListAdapter.getCount()) {
                        // Account for empty spacer item
                        return false;
                    }
                    selectItem(i);
                    return true;
                }
            });
            // Footer to keep space for floating action button
            mListView.addFooterView(
                    inflater.inflate(R.layout.list_bottom_spacer, mListView, false), null, false);
        }
        mListView.setEmptyView(view.findViewById(R.id.list_empty_view));
        mEmptyImage = (ImageView) view.findViewById(R.id.list_empty_image);

        mSwipeRefreshLayout =
                (SwipeRefreshLayout) view.findViewById(R.id.item_swipe_refresh_layout);

        View fab = view.findViewById(R.id.fab);
        if (isReadOnly()) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addItem();
                }
            });
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadContent(true);
            }
        });

        // Empty list is sad :(
        // load offline content, online content will be loaded in onResume
        loadContent(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTheme == Settings.getInt(getActivity(), Settings.THEME)) {
            if (!mResumeDownloadNeeded) {
                // Check if some server settings changed
                Context context = getActivity();
                String serverUrl = Settings.getString(context, Settings.SERVER_URL);
                String serverLoginUsername = Settings.getString(context,
                        Settings.SERVER_LOGIN_USERNAME);
                String serverLoginPassword = Settings.getString(context,
                        Settings.SERVER_LOGIN_PASSWORD);
                if (
                        (serverUrl != mServerUrl &&
                                serverUrl != null &&
                                !serverUrl.equals(mServerUrl)) ||
                        (serverLoginUsername != mServerLoginUsername &&
                                serverLoginUsername != null &&
                                !serverLoginUsername.equals(mServerLoginUsername)) ||
                        (serverLoginPassword != mServerLoginPassword &&
                                serverLoginPassword != null &&
                                !serverLoginPassword.equals(mServerLoginPassword))) {
                    mServerUrl = serverUrl;
                    mServerLoginUsername = serverLoginUsername;
                    mServerLoginPassword = serverLoginPassword;
                    mResumeDownloadNeeded = true;
                }
            }
            // Don't start download when in action mode, as this would close it
            if (mActionMode == null) {
                if (mResumeDownloadNeeded) {
                    //if (mCert4androidReady) {
                        loadContent(true);
                    //}
                }
            }
            // Update suggestions if necessary
            SuggestionsRetriever.updateSuggestions(getActivity().getApplicationContext());
            UsageSuggestionsRetriever.updateSuggestions(getActivity().getApplicationContext());
        } // else: already up-to date or re-creation is coming
        boolean picturePreview =
                Settings.getBoolean(getActivity(), Settings.OVERVIEW_PICTURE_PREVIEW);
        if (picturePreview != mPicturePreview) {
            mPicturePreview = picturePreview;
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }
        updateEmptyListImage(mEmptyImage);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_show_items, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            case R.id.action_show_completed:
                startActivity(new Intent(getActivity(), MainActivity.class)
                        .putExtra(MainActivity.FRAGMENT_CLASS,
                                ShowCompletedItemsFragment.class.getName()));
                mResumeDownloadNeeded = true;
                return true;
            case R.id.action_show_by_usage:
                startActivity(new Intent(getActivity(), MainActivity.class)
                        .putExtra(MainActivity.FRAGMENT_CLASS,
                                RecipeItemsFragment.class.getName()));
                mResumeDownloadNeeded = true;
                return true;
            case R.id.action_sort_order:
                showSortOrderDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void updateEmptyListImage(ImageView imageView) {
        imageView.setVisibility(Settings.getBoolean(getActivity(), Settings.DINO)
                ? View.VISIBLE : View.GONE);
        imageView.setImageResource(R.drawable.dino_sated);
    }

    private void showSortOrderDialog() {
        mSortOrder = Settings.getInt(getActivity(), getSortOrderPreference());
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.change_sort_order_title)
                .setSingleChoiceItems(
                        getResources().getStringArray(R.array.pref_sort_order_entries),
                        Settings.getInt(getActivity(), getSortOrderPreference()),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int[] values = getResources()
                                        .getIntArray(R.array.pref_sort_order_values);
                                mSortOrder = values[i];
                            }
                        }
                )
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mSortOrder != Settings.getInt(getActivity(),
                                getSortOrderPreference())) {
                            Settings.putInt(getActivity(), getSortOrderPreference(), mSortOrder);
                            reload();
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        if (mSortOrder != Settings.getInt(getActivity(),
                                getSortOrderPreference())) {
                            Settings.putInt(getActivity(), getSortOrderPreference(), mSortOrder);
                            reload();
                        }
                    }
                })
                .create().show();
    }

    private boolean isNetworkConnectionAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    protected void reload() {
        loadContent(true);
    }

    private void loadContent(boolean download) {
        if (DEBUG) Log.d(TAG, "loadContent: attempt download: " + download);
        if (download) {
            mResumeDownloadNeeded = false;
            if (mDownloading) {
                if (DEBUG) Log.d(TAG, "cancel loadContent: already being done");
                mResumeDownloadNeeded = mDirectDownloadNeeded = true;
                return;
            }
            // Check whether settings are fine
            if (TextUtils.isEmpty(Settings.getString(getActivity(), Settings.SERVER_URL)) ||
                    TextUtils.isEmpty(Settings.getString(getActivity(), Settings.WHOAMI))) {
                showGoToSettingsSnackbar(getString(R.string.toast_prefs_required));
                mSwipeRefreshLayout.setRefreshing(false);
                return;
            }

            if (!isNetworkConnectionAvailable()) {
                dismissSnackbar();
                setTitle(getString(R.string.app_title_offline));
                mSwipeRefreshLayout.setRefreshing(false);
                mResumeDownloadNeeded = true;
            } else {
                mDownloading = true;
                new RequestItemsTask().execute();
                return;
            }
        }
        if (getOfflinePreference() != null) {
            try {
                loadContent(new JSONObject(
                        PreferenceManager.getDefaultSharedPreferences(getActivity())
                                .getString(getOfflinePreference(), "")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // Might make use of cache, so load either way
            try {
                loadContent(new JSONObject(Constants.JSON.EMPTY_FALLBACK));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void addItem() {
        new EditItemFragment().setOnEditResultListener(this)
                .show(getFragmentManager(), FRAGMENT_TAG_EDIT_ITEM_DIALOG);
    }

    public void editItem(final Item item) {
        new EditItemFragment().setEditItem(item)
                .setOnEditResultListener(this)
                .show(getFragmentManager(), FRAGMENT_TAG_EDIT_ITEM_DIALOG);
    }

    private void showItem(final Item item) {
        if (isReadOnly() || item.hasPicture()) {
            new ShowItemFragment().setItem(item)
                    .setItemInteractionListener(this)
                    .show(getFragmentManager(), FRAGMENT_TAG_SHOW_ITEM_DIALOG);
        } else {
            editItem(item);
        }
    }

    @Override
    public void onEditItemResult(boolean add, Item item, boolean localPictureUpload,
                                 boolean skipUrlUpdate) {
        // Reload list only after all cache updates are done.
        // edit first so add works too.
        editItem(add, item, false, skipUrlUpdate);
        if (localPictureUpload) {
            uploadLocalPicture(item);
        }
        // Offline preview
        loadContent(false);
        // Push update
        loadContent(true);
    }

    protected void editItem(boolean add, Item item, boolean reloadList, boolean skipUrlUpdate) {
        String params = ServerCommunicator.initializeParameter(getActivity());
        params = ServerCommunicator.addParameter(params, Constants.JSON.NAME, item.name);
        params = ServerCommunicator.addParameter(params, Constants.JSON.CREATOR, item.creator);
        params = ServerCommunicator.addParameter(params, Constants.JSON.UPDATED_BY, item.updatedBy);
        params = ServerCommunicator.addParameter(params, Constants.JSON.CREATION_DATE,
                String.valueOf(item.creationDate));
        params = ServerCommunicator.addParameter(params, Constants.JSON.INFO, item.info);
        params = ServerCommunicator.addParameter(params, Constants.JSON.USAGE, item.usage);
        if (!skipUrlUpdate) {
            params = ServerCommunicator.addParameter(params, Constants.JSON.PICTURE_URL,
                    item.pictureUrl);
        } // else: use whatever is already there (ensure not overwriting cached picture uploads)
        // ID also required for edit: server would create us one if we
        // didn't send it, but we need it for offline preview
        params = ServerCommunicator.addParameter(params, Constants.JSON.ID,
                String.valueOf(item.id));
        HttpPostOfflineCache.addItemToCache(getActivity(), add
                ? Constants.SITE.INSERT_ITEM : Constants.SITE.UPDATE_ITEM, params, item);
        if (reloadList) {
            // Offline preview
            loadContent(false);
            // Push update
            loadContent(true);
        }
        // Ensure we always have new usages in the suggestion cache
        UsageSuggestionsRetriever.appendCachedSuggestion(getActivity(), item.usage);
    }

    protected void uploadLocalPicture(Item item) {
        if (TextUtils.isEmpty(item.localPictureUrl)) {
            Log.e(TAG, "uploadLocalPicture on empty picture URL");
            return;
        }
        List<HttpPostOfflineCache.MultiPartRequestParameter> params =
                ServerCommunicator.initializeMultipartParameter(getActivity());
        params = ServerCommunicator.addParameter(params, Constants.JSON.ID, item.id);
        params = ServerCommunicator.addParameter(params, Constants.JSON.UPDATED_BY, item.updatedBy);
        params = ServerCommunicator.addParameter(params, Constants.JSON.PICTURE,
                LocalPictureHandler.fileFromURI(item.localPictureUrl));
        // No preview: already done in editItem
        HttpPostOfflineCache.addItemToCache(getActivity(), Constants.SITE.PICTURE_ADD, params,
                null);
    }

    @Override
    public void completeItem(Item item) {
        ArrayList<Item> preview = new ArrayList<>();
        preview.add(item);
        completeItems(preview);
    }

    private void completeSelection() {
        ArrayList<Item> preview = new ArrayList<>();
        for (Integer selectedPos : mSelectedItems) {
            preview.add(mListAdapter.getItem(selectedPos));
        }
        completeItems(preview);
    }

    private void completeItems(List<Item> items) {
        String params = ServerCommunicator.initializeParameter(getActivity());
        params = ServerCommunicator.addParameter(params, Constants.JSON.SELECTION,
                getSelection(items));
        params = ServerCommunicator.addParameter(params, Constants.JSON.UPDATED_BY,
                Settings.getString(getActivity(), Settings.WHOAMI));
        HttpPostOfflineCache.addItemsToRemoveCache(getActivity(), Constants.SITE.COMPLETE_ITEMS,
                params, items);
        // Offline preview
        loadContent(false);
        // Upload stuff
        loadContent(true);
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }

    private void selectItem(int position) {
        if (mSelectedItems.isEmpty()) {
            mActionMode = getActivity().startActionMode(mSelectedItemsActionModeCallback);
        }
        if (mSelectedItems.contains(position)) {
            mSelectedItems.remove((Integer) position);
        } else {
            mSelectedItems.add(position);
        }
        mListAdapter.notifyDataSetChanged();
        if (mSelectedItems.isEmpty()) {
            mActionMode.finish();
            mActionMode = null;
        } else {
            updateSelectedItemsActionModeMenu();
        }
    }

    private void updateSelectedItemsActionModeMenu() {
        mActionMode.getMenu().clear();
        mSelectedItemsActionModeCallback.onCreateActionMode(mActionMode, mActionMode.getMenu());
        mActionMode.setTitle(getResources().getQuantityString(R.plurals.action_mode_selected_items,
                mSelectedItems.size(), mSelectedItems.size()));
    }

    private String getSelection(List<Item> items) {
        if (items.isEmpty()) {
            return null;
        }
        String result = Constants.JSON.ID + " = " + items.get(0).id;
        for (int i = 1; i < items.size(); i++) {
            result += " OR " + Constants.JSON.ID + " = " + items.get(i).id;
        }
        return result;
    }

    protected void setTitle(CharSequence title) {
        if (getActivity() != null) {
            getActivity().setTitle(title);
        }
    }

    protected void showGoToSettingsSnackbar(CharSequence text) {
        if (mSnackbar == null) {
            mSnackbar = Snackbar.make(mListView, text, Snackbar.LENGTH_INDEFINITE);
            mSnackbar.setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(getActivity(), SettingsActivity.class));
                    dismissSnackbar();
                }
            });
        } else {
            mSnackbar.setText(text);
        }
        mSnackbar.show();
    }

    protected void dismissSnackbar() {
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
    }

    protected String getRequestSite() {
        return Constants.SITE.GET_LIST;
    }

    protected String getRequestParameters() {
        String params = ServerCommunicator.initializeParameter(getActivity());
        params = ServerCommunicator.addParameter(params, Constants.JSON.SORT_ORDER,
                getSortOrder());
        return params;
    }

    protected String getSortOrder() {
        int sortOrder = Settings.getInt(getActivity(), getSortOrderPreference());
        switch (sortOrder) {
            case 1:
                return Constants.JSON.ORDER_BY + Constants.JSON.CREATION_DATE + Constants.JSON.ASC;
            case 2:
                return Constants.JSON.ORDER_BY + Constants.JSON.NAME + Constants.JSON.ASC;
            case 0:
            default:
                return Constants.JSON.ORDER_BY + Constants.JSON.CREATION_DATE + Constants.JSON.DESC;
        }
    }

    protected boolean isReadOnly() {
        return false;
    }

    protected String getOfflinePreference() {
        return PREF_LAST_LIST;
    }

    protected Item[] previewCache(Item[] items) {
        return HttpPostOfflineCache.previewCache(getActivity(), items);
    }

    protected String getSortOrderPreference() {
        return Settings.SORT_ORDER;
    }

    private class RequestItemsTask extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSwipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected JSONObject doInBackground(Void... args) {
            try {
                // Execute pending requests
                HttpPostOfflineCache.executePending(getActivity());

                // Download site
                return ServerCommunicator.requestHttp(getActivity(), getRequestSite(),
                        getRequestParameters());
            } catch (Exception e) {
                if (getActivity() == null) {
                    Log.d(TAG, "Lost activity, discarding error during background execution", e);
                } else {
                    e.printStackTrace();
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            mSwipeRefreshLayout.setRefreshing(false);
            mDownloading = false;
            if (getActivity() == null) {
                Log.d(TAG, "Discarding request result");
                // Discard
                return;
            }
            try {
                try {
                    if (result.getInt(Constants.JSON.SUCCESS) == 0) {
                        // Not successful
                        setTitle(getString(R.string.app_title_fail));
                        loadContent(false);
                        return;
                    }
                } catch (Exception e) {
                    // Not an expected response
                    if (isNetworkConnectionAvailable()) {
                        // If we're offline now, that's probably the reason, otherwise,
                        // there probably was a server communication error
                        showGoToSettingsSnackbar(getText(R.string.toast_server_error));
                    } else {
                        // Try again later
                        mResumeDownloadNeeded = true;
                    }
                    e.printStackTrace();
                    setTitle(getString(R.string.app_title_fail));
                    loadContent(false);
                    return;
                }
                dismissSnackbar();

                // Persist for offline
                if (getOfflinePreference() != null) {
                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .edit()
                            .putString(getOfflinePreference(), result.toString())
                            .apply();
                }

                // Everything is up to date
                HttpPostOfflineCache.clearPreviewCacheIfInstructionsEmpty(getActivity());

                // Try to load with received json
                if (loadContent(result)) {

                    String updateTimeFormatPattern =
                            Settings.getString(getActivity(), Settings.UPDATE_TIME_FORMAT);
                    DateFormat updateTimeFormat;
                    if (TextUtils.isEmpty(updateTimeFormatPattern)) {
                        updateTimeFormat = SimpleDateFormat.getTimeInstance();
                    } else {
                        updateTimeFormat = new SimpleDateFormat(updateTimeFormatPattern);
                    }
                    setTitle(getString(R.string.app_title_online,
                                 updateTimeFormat.format(System.currentTimeMillis())));
                } else {
                    setTitle(getString(R.string.app_title_fail));
                    loadContent(false);
                }
            } finally {
                // Load again if required
                if (mDirectDownloadNeeded) {
                    mDirectDownloadNeeded = false;
                    loadContent(true);
                }
            }
        }
    }

    private boolean loadContent(JSONObject json) {
        try {
            JSONArray jItems = json.getJSONArray(Constants.JSON.ITEMS);
            int size = jItems.length();
            Item[] items = new Item[size];
            for (int i = 0; i < size; i++) {
                JSONObject jItem = jItems.getJSONObject(i);
                items[i] = new Item();
                items[i].id = jItem.getLong(Constants.JSON.ID);
                items[i].name = jItem.getString(Constants.JSON.NAME);
                if (jItem.has(Constants.JSON.INFO)) {
                    items[i].info = jItem.getString(Constants.JSON.INFO);
                }
                if (jItem.has(Constants.JSON.USAGE)) {
                    items[i].usage = jItem.getString(Constants.JSON.USAGE);
                }
                items[i].creator = jItem.getString(Constants.JSON.CREATOR);
                if (jItem.has(Constants.JSON.UPDATED_BY)) {
                    items[i].updatedBy = jItem.getString(Constants.JSON.UPDATED_BY);
                }
                items[i].creationDate = jItem.getLong(Constants.JSON.CREATION_DATE);
                if (jItem.has(Constants.JSON.COMPLETION_DATE)) {
                    items[i].completionDate = jItem.getLong(Constants.JSON.COMPLETION_DATE);
                }
                if (jItem.has(Constants.JSON.PICTURE_URL)) {
                    items[i].pictureUrl = jItem.getString(Constants.JSON.PICTURE_URL);
                }
            }
            items = previewCache(items);

            // If items have changed, close action mode
            if (mListAdapter != null && mActionMode != null) {
                if (items.length != mListAdapter.getCount()) {
                    mActionMode.finish();
                    mActionMode = null;
                } else {
                    for (int i = 0; i < items.length; i++) {
                        if (!items[i].equals(mListAdapter.getItem(i))) {
                            mActionMode.finish();
                            mActionMode = null;
                            break;
                        }
                    }
                }
            }
            mListAdapter = getListAdapter(items);
            if (mListView instanceof ExpandableListView) {
                ((ExpandableListView) mListView).setAdapter((ExpandableListAdapter) mListAdapter);
            } else {
                mListView.setAdapter((ListAdapter) mListAdapter);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected CustomListAdapterInterface<Item> getListAdapter(Item[] items) {
        return new ItemArrayAdapter(getActivity(), R.layout.list_item, items);
    }

    protected class ItemArrayAdapter extends ArrayAdapter<Item>
            implements CustomListAdapterInterface<Item> {

        private Item[] mItems;
        private int mItemBgColor;
        private int mItemSelectedBgColor;

        public ItemArrayAdapter(Context context, int resource, Item[] objects) {
            super(context, resource, objects);
            mItems = objects;
            loadResources();
        }

        @Override
        public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, parent, false);

                holder = new ViewHolder();
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.creator = (TextView) convertView.findViewById(R.id.creator);
                holder.info = (TextView) convertView.findViewById(R.id.info);
                holder.usage = (TextView) convertView.findViewById(R.id.usage);
                holder.date = (TextView) convertView.findViewById(R.id.date);
                holder.notSyncedIndicator = convertView.findViewById(R.id.indicator_not_synced);
                holder.pictureIndicator = convertView.findViewById(R.id.indicator_picture);
                holder.picture = (ImageView) convertView.findViewById(R.id.picture);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.name.setText(mItems[position].name);
            if (TextUtils.isEmpty(mItems[position].updatedBy) ||
                    mItems[position].creator.equals(mItems[position].updatedBy)) {
                // Creator and updatedBy identical
                holder.creator.setText(getString(R.string.list_entry_creator,
                        mItems[position].creator));
            } else {
                holder.creator.setText(getString(R.string.list_entry_creator_updated_by,
                        mItems[position].creator, mItems[position].updatedBy));
            }
            holder.info.setText(mItems[position].info);
            holder.info.setVisibility(TextUtils.isEmpty(mItems[position].info)
                    ? View.GONE : View.VISIBLE);
            holder.usage.setText(mItems[position].usage);
            holder.usage.setVisibility(TextUtils.isEmpty(mItems[position].usage)
                    ? View.GONE : View.VISIBLE);
            if (mItems[position].completionDate > mItems[position].creationDate) {
                holder.date.setText(getFormattedDate(getActivity(),
                        mItems[position].completionDate));
            } else {
                holder.date.setText(getFormattedDate(getActivity(), mItems[position].creationDate));
            }
            holder.notSyncedIndicator.setVisibility(mItems[position].isCached()
                    ? View.VISIBLE : View.GONE);
            if (Settings.getBoolean(getActivity(), Settings.OVERVIEW_PICTURE_PREVIEW)) {
                holder.pictureIndicator.setVisibility(View.GONE);
                if (mItems[position].hasPicture()) {
                    holder.picture.setVisibility(View.VISIBLE);
                    if (ServerCommunicator.setupHttps(getActivity(), ShowItemsFragment.this)) {
                        Glide.with(holder.picture)
                                .load(mItems[position].getPictureUrl(getActivity()))
                                .error(R.drawable.ic_broken_picture)
                                .circleCrop()
                                .into(holder.picture);
                    }
                } else {
                    holder.picture.setVisibility(View.GONE);
                }
            } else {
                holder.picture.setVisibility(View.GONE);
                holder.pictureIndicator.setVisibility(mItems[position].hasPicture()
                        ? View.VISIBLE : View.GONE);
            }
            convertView.setBackgroundColor(mSelectedItems.contains(position)
                    ? mItemSelectedBgColor
                    : mItemBgColor);

            return convertView;
        }

        private class ViewHolder {
            TextView name;
            TextView creator;
            TextView info;
            TextView usage;
            TextView date;
            View notSyncedIndicator;
            View pictureIndicator;
            ImageView picture;
        }

        private void loadResources() {
            int[] attrs = new int[] {
                    R.attr.backgroundColorListItem,
                    R.attr.backgroundColorListItemSelected,
            };
            TypedArray ta = getActivity().getTheme().obtainStyledAttributes(attrs);
            mItemBgColor = ta.getColor(0, Color.TRANSPARENT);
            mItemSelectedBgColor = ta.getColor(1, Color.GRAY);
        }
    }

    public static String getFormattedDate(Context context, long time) {
        /*
        Calendar today = Calendar.getInstance();
        Calendar formatted = Calendar.getInstance();
        formatted.setTimeInMillis(time);
        String format;
        if (today.get(Calendar.YEAR) == formatted.get(Calendar.YEAR)) {
            if (today.get(Calendar.MONTH) == formatted.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) == formatted.get(Calendar.DAY_OF_MONTH)) {
                format = getString(R.string.date_format_hour_minute);
            } else {
                format = getString(R.string.date_format_day);
            }
        } else {
            format = getString(R.string.date_format_year);
        }
        return new SimpleDateFormat(format).format(time);
        */
        String creationDateFormatPattern =
                Settings.getString(context, Settings.LIST_ITEM_CREATION_DATE_FORMAT);
        DateFormat creationDateFormat;
        if (TextUtils.isEmpty(creationDateFormatPattern)) {
            creationDateFormat = SimpleDateFormat.getDateInstance();
        } else {
            creationDateFormat = new SimpleDateFormat(creationDateFormatPattern);
        }
        return creationDateFormat.format(time);
    }

    @Override
    public void onHttpsReady() {
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void reAddItem(final Item oldItem) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.re_add_item_title)
                .setMessage(getString(R.string.re_add_item_message, oldItem.name))
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Item addItem = oldItem.copy();
                        addItem.id = System.currentTimeMillis();
                        addItem.creationDate = System.currentTimeMillis();
                        addItem.creator = Settings.getString(getActivity(), Settings.WHOAMI);
                        addItem.completionDate = -1;
                        ShowItemsFragment.this.editItem(true, addItem, false, false);
                    }
                })
                .show();
    }
}
