/*
 * Copyright (C) 2017-2018 SpiritCroc
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
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;

public class ShowCompletedItemsFragment extends ShowItemsFragment
        implements BackButtonHandler, SearchView.OnQueryTextListener {

    private SearchView mSearchView;
    private String mSearch;
    private boolean mHideDuplicateEntries;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHideDuplicateEntries =
                Settings.getBoolean(getActivity(), Settings.HIDE_DUPLICATES_COMPLETED);
    }

    @Override
    protected String getRequestSite() {
        return Constants.SITE.GET_COMPLETED_LIST;
    }

    @Override
    protected String getRequestParameters() {
        String requestParameters = super.getRequestParameters();
        if (!TextUtils.isEmpty(mSearch)) {
            requestParameters = ServerCommunicator.addParameter(requestParameters,
                    Constants.JSON.SEARCH, mSearch);
        }
        if (mHideDuplicateEntries) {
            requestParameters = ServerCommunicator.addParameter(requestParameters,
                    Constants.JSON.GROUPING, Constants.JSON.HIDE_OLDER_DUPLICATES);
        }
        return requestParameters;
    }

    @Override
    protected String getSortOrder() {
        int sortOrder = Settings.getInt(getActivity(), getSortOrderPreference());
        switch (sortOrder) {
            case 0:
                return Constants.JSON.ORDER_BY + Constants.JSON.COMPLETION_DATE +
                        Constants.JSON.DESC;
            case 1:
                return Constants.JSON.ORDER_BY + Constants.JSON.COMPLETION_DATE +
                        Constants.JSON.ASC;
            default:
                return super.getSortOrder();
        }
    }

    @Override
    protected boolean isReadOnly() {
        return true;
    }

    @Override
    protected String getOfflinePreference() {
        // Don't save for offline display
        return null;
    }

    @Override
    protected Item[] previewCache(Item[] items) {
        return HttpPostOfflineCache.previewCompletedCache(getActivity(), items);
    }

    @Override
    protected String getSortOrderPreference() {
        return Settings.SORT_ORDER_COMPLETED;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_completed_items, container, false);
        ViewGroup parentContainer = (ViewGroup) view.findViewById(R.id.show_items_container);
        View parent = super.onCreateView(inflater, parentContainer, savedInstanceState);
        parentContainer.addView(parent);
        // Custom click listener
        if (BossUtils.isBoss(getActivity())) {
            // Extra functionality
            ListView list = view.findViewById(R.id.item_list);
            list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i,
                                               long l) {
                    BossUtils.showBossCompletedItemDialog(getActivity(), mListAdapter.getItem(i),
                            ShowCompletedItemsFragment.this);
                    return true;
                }
            });
        }
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_show_completed).setVisible(false);
        menu.findItem(R.id.action_show_by_usage).setVisible(false);
        inflater.inflate(R.menu.fragment_show_completed_items, menu);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus && TextUtils.isEmpty(mSearchView.getQuery())) {
                    closeSearchView();
                }
            }
        });
        menu.findItem(R.id.action_hide_duplicates).setChecked(mHideDuplicateEntries);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_hide_duplicates:
                mHideDuplicateEntries = !mHideDuplicateEntries;
                item.setChecked(mHideDuplicateEntries);
                Settings.putBoolean(getActivity(), Settings.HIDE_DUPLICATES_COMPLETED,
                        mHideDuplicateEntries);
                reload();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (mSearchView.isIconified()) {
            return false;
        } else {
            closeSearchView();
            return true;
        }
    }

    @Override
    public boolean onQueryTextChange(String text) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String text) {
        mSearch = text;
        reload();
        return true;
    }

    private void closeSearchView() {
        mSearch = null;
        mSearchView.setQuery("", false);
        mSearchView.setIconified(true);
        reload();
    }

    @Override
    protected void updateEmptyListImage(ImageView imageView) {
        imageView.setVisibility(Settings.getBoolean(getActivity(), Settings.DINO)
                ? View.VISIBLE : View.GONE);
        imageView.setImageResource(R.drawable.dino_hungry);
    }
}
