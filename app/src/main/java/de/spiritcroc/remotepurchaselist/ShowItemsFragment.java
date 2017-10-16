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
import android.content.ComponentName;
import android.content.Context;
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
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import at.bitfire.cert4android.CustomCertService;
import de.spiritcroc.remotepurchaselist.settings.AboutActivity;
import de.spiritcroc.remotepurchaselist.settings.SettingsActivity;

public class ShowItemsFragment extends Fragment
        implements EditItemFragment.OnEditItemResultListener {

    private static final String TAG = ShowItemsFragment.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String PREF_LAST_LIST = "last_list";

    private static final String FRAGMENT_TAG_EDIT_ITEM_DIALOG = ShowItemsFragment.class.getName() +
            ".edit_item_dialog";

    private boolean mDownloading = false;
    private boolean mResumeDownloadNeeded = true;
    private boolean mDirectDownloadNeeded = false;

    // Remember theme, so we re-download on theme change, leading to crash
    // because of activity recreation
    private int mTheme;

    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Snackbar mSnackbar;
    private ImageView mEmptyImage;

    protected ItemArrayAdapter mListAdapter;

    private ActionMode mActionMode;
    private ArrayList<Integer> mSelectedItems = new ArrayList<>();

    // Sometimes http request fails because of server isn't ready yet otherwise
    // Thanks to schaarsc <schaarsc> for https://github.com/stefan-
    // niedermann/nextcloud-notes/commit/b3c2cc2f0e1228b07b55658ff8fa6a244533f5c1
    // (only sync if cert4android service is available)
    private boolean mCert4androidReady = false;

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

        EditItemFragment editItemFragment = (EditItemFragment)
                getFragmentManager().findFragmentByTag(FRAGMENT_TAG_EDIT_ITEM_DIALOG);
        if (editItemFragment != null) {
            editItemFragment.setOnEditResultListener(this);
        }

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_show_items, container, false);

        mListView = (ListView) view.findViewById(R.id.item_list);
        if (!isReadOnly()) {
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    if (i >= mListAdapter.getCount()) {
                        // Account for empty spacer item
                        return;
                    }
                    if (mSelectedItems.isEmpty()) {
                        editItem(mListAdapter.getItem(i));
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
            // Don't start download when in action mode, as this would close it
            if (mActionMode == null) {
                if (mResumeDownloadNeeded) {
                    if (mCert4androidReady) {
                        loadContent(true);
                    }
                }
            }
        } // else: already up-to date or re-creation is coming
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
                mResumeDownloadNeeded = true;
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void updateEmptyListImage(ImageView imageView) {
        imageView.setVisibility(Settings.getBoolean(getActivity(), Settings.DINO)
                ? View.VISIBLE : View.GONE);
        imageView.setImageResource(R.drawable.dino_sated);
    }

    private boolean isNetworkConnectionAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void loadContent(boolean download) {
        if (DEBUG) Log.d(TAG, "loadContent: attempt download: " + download);
        mResumeDownloadNeeded = !download;
        if (download) {
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
        }
    }

    private void addItem() {
        new EditItemFragment().setOnEditResultListener(this)
                .show(getFragmentManager(), FRAGMENT_TAG_EDIT_ITEM_DIALOG);
    }

    private void editItem(final Item item) {
        new EditItemFragment().setEditItem(item)
                .setOnEditResultListener(this)
                .show(getFragmentManager(), FRAGMENT_TAG_EDIT_ITEM_DIALOG);
    }

    @Override
    public void onEditItemResult(boolean add, Item item) {
        editItem(add, item, true);
    }

    protected void editItem(boolean add, Item item, boolean reloadList) {
        String params = ServerCommunicator.addParameter(null, Constants.JSON.NAME, item.name);
        params = ServerCommunicator.addParameter(params, Constants.JSON.CREATOR, item.creator);
        params = ServerCommunicator.addParameter(params, Constants.JSON.UPDATED_BY, item.updatedBy);
        params = ServerCommunicator.addParameter(params, Constants.JSON.CREATION_DATE,
                String.valueOf(item.creationDate));
        params = ServerCommunicator.addParameter(params, Constants.JSON.INFO, item.info);
        // ID also required for edit: server would create us one if we
        // didn't send it, but we need it for offline preview
        params = ServerCommunicator.addParameter(params, Constants.JSON.ID, String.valueOf(item.id));
        HttpPostOfflineCache.addItemToCache(getActivity(), add
                ? Constants.SITE.INSERT_ITEM : Constants.SITE.UPDATE_ITEM, params, item);
        if (reloadList) {
            // Offline preview
            loadContent(false);
            // Push update
            loadContent(true);
        }
    }

    private void completeSelection() {
        ArrayList<Long> preview = new ArrayList<>();
        for (Integer selectedPos: mSelectedItems) {
            preview.add(mListAdapter.getItem(selectedPos).id);
        }

        String params =
                ServerCommunicator.addParameter(null, Constants.JSON.SELECTION, getSelection());
        params = ServerCommunicator.addParameter(params, Constants.JSON.UPDATED_BY,
                Settings.getString(getActivity(), Settings.WHOAMI));
        HttpPostOfflineCache.addItemsToRemoveCache(getActivity(), Constants.SITE.COMPLETE_ITEMS,
                params, preview);
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

    private String getSelection() {
        if (mSelectedItems.isEmpty()) {
            return null;
        }
        String result = Constants.JSON.ID + " = " + mListAdapter.getItem(mSelectedItems.get(0)).id;
        for (int i = 1; i < mSelectedItems.size(); i++) {
            result += " OR " + Constants.JSON.ID + " = " +
                    mListAdapter.getItem(mSelectedItems.get(i)).id;
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
                    mResumeDownloadNeeded = true;
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

    protected boolean isReadOnly() {
        return false;
    }

    protected String getOfflinePreference() {
        return PREF_LAST_LIST;
    }

    private class RequestItemsTask extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSwipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected JSONObject doInBackground(Void... args) {
            // Execute pending requests
            HttpPostOfflineCache.executePending(getActivity());

            /*
            // Emulate slow internet
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e) {}
            */

            // Download site
            return ServerCommunicator.requestHttp(getActivity(), getRequestSite(), null);
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            mSwipeRefreshLayout.setRefreshing(false);
            mDownloading = false;
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
                    // Everything is up to date
                    HttpPostOfflineCache.clearPreviewCacheIfInstructionsEmpty(getActivity());
                }

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
                items[i].creator = jItem.getString(Constants.JSON.CREATOR);
                if (jItem.has(Constants.JSON.UPDATED_BY)) {
                    items[i].updatedBy = jItem.getString(Constants.JSON.UPDATED_BY);
                }
                items[i].creationDate = jItem.getLong(Constants.JSON.CREATION_DATE);
                if (jItem.has(Constants.JSON.COMPLETION_DATE)) {
                    items[i].completionDate = jItem.getLong(Constants.JSON.COMPLETION_DATE);
                }
            }
            if (getOfflinePreference() != null) {
                items = HttpPostOfflineCache.previewCache(getActivity(), items);
            }
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
            mListAdapter = new ItemArrayAdapter(getActivity(), R.layout.list_item, items);
            mListView.setAdapter(mListAdapter);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    protected class ItemArrayAdapter extends ArrayAdapter<Item> {

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
                holder.date = (TextView) convertView.findViewById(R.id.date);
                holder.notSyncedIndicator = convertView.findViewById(R.id.indicator_not_synced);

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
            if (mItems[position].completionDate > mItems[position].creationDate) {
                holder.date.setText(getFormattedDate(mItems[position].completionDate));
            } else {
                holder.date.setText(getFormattedDate(mItems[position].creationDate));
            }
            holder.notSyncedIndicator.setVisibility(mItems[position].isCached()
                    ? View.VISIBLE : View.GONE);
            convertView.setBackgroundColor(mSelectedItems.contains(position)
                    ? mItemSelectedBgColor
                    : mItemBgColor);

            return convertView;
        }

        private class ViewHolder {
            TextView name;
            TextView creator;
            TextView info;
            TextView date;
            View notSyncedIndicator;
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

    private String getFormattedDate(long time) {
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
                Settings.getString(getActivity(), Settings.LIST_ITEM_CREATION_DATE_FORMAT);
        DateFormat creationDateFormat;
        if (TextUtils.isEmpty(creationDateFormatPattern)) {
            creationDateFormat = SimpleDateFormat.getDateInstance();
        } else {
            creationDateFormat = new SimpleDateFormat(creationDateFormatPattern);
        }
        return creationDateFormat.format(time);
    }
}
