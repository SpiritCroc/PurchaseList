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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeItemsFragment extends ShowItemsFragment {

    private static final String PREF_LAST_LIST = "last_recipe_list";

    private ArrayList<String> mUsages = new ArrayList<>();
    private Item[] mItems;
    private ArrayList<Item> mSelection = new ArrayList<>();
    private View mFab;

    private int mItemColor;
    private int mActiveItemColor;
    private int mGroupColor;

    @Override
    protected String getRequestSite() {
        return Constants.SITE.GET_LIST_BY_USAGE;
    }

    @Override
    protected boolean isReadOnly() {
        return true;
    }

    @Override
    protected String getOfflinePreference() {
        return PREF_LAST_LIST;
    }

    @Override
    protected Item[] previewCache(Item[] items) {
        // Don't apply any cached changes here
        return items;
    }

    @Override
    protected void updateEmptyListImage(ImageView imageView) {
        imageView.setVisibility(Settings.getBoolean(getActivity(), Settings.DINO)
                ? View.VISIBLE : View.GONE);
        imageView.setImageResource(R.drawable.dino_hungry);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load styled attributes
        int[] attrs = { R.attr.colorRecipeListItem,
                R.attr.colorRecipeListItemActive,
                R.attr.colorRecipeListGroup,
        };
        TypedArray a = getActivity().obtainStyledAttributes(attrs);
        mItemColor = a.getColor(0, 0x808080);
        mActiveItemColor = a.getColor(1, 0x808080);
        mGroupColor = a.getColor(2, 0x808080);
        a.recycle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_items_by_usage, container, false);
        mListView = view.findViewById(R.id.item_list);
        mListView.setEmptyView(view.findViewById(R.id.list_empty_view));
        mEmptyImage = (ImageView) view.findViewById(R.id.list_empty_image);
        mSwipeRefreshLayout =
                (SwipeRefreshLayout) view.findViewById(R.id.item_swipe_refresh_layout);
        ((ExpandableListView) mListView).setOnChildClickListener(
                new ExpandableListView.OnChildClickListener() {
                    @Override
                    public boolean onChildClick(ExpandableListView parent, View v,
                                                int groupPosition, int childPosition, long id) {
                        CheckBox cb;
                        cb = (CheckBox) v.findViewById(android.R.id.checkbox);
                        cb.toggle();

                        return onItemClick(groupPosition, childPosition, cb);
                    }
                });
        mFab = view.findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItems();
            }
        });
        updateFabVisibility();
        // Footer to keep space for floating action button
        mListView.addFooterView(
                inflater.inflate(R.layout.list_bottom_spacer, mListView, false), null, false);
        return view;
    }

    @Override
    protected CustomListAdapterInterface<Item> getListAdapter(Item[] items) {
        mItems = items;
        mSelection.clear();
        mUsages.clear();
        for (int i = 0; i < mItems.length; i++) {
            String usage = mItems[i].usage;
            if (!mUsages.contains(usage)) {
                mUsages.add(usage);
            }
        }

        final String ROOT = "ROOT_NAME";
        final String CHILD = "CHILD_NAME";

        // Groups
        List<Map<String, String>> usageData = new ArrayList<Map<String, String>>() {{
            for (int i = 0; i < mUsages.size(); i++) {
                final int j = i;
                add(new HashMap<String, String>() {{
                    put(ROOT, mUsages.get(j));
                }});
            }
        }};

        final List<List<Map<String, String>>> listOfChildGroups = new ArrayList<>();

        // Add items
        for (int i = 0; i < mUsages.size(); i++) {
            final int x = i;
            List<Map<String, String>> childGroup = new ArrayList<Map<String, String>>(){{
                int childCount = 0;
                for (int j = 0; j < mItems.length; j++) {
                    if (mItems[j].usage.equals(mUsages.get(x))) {
                        final Item item = mItems[j];
                        add(new HashMap<String, String>() {{
                            put(CHILD, item.name);
                        }});
                    }
                }
            }};
            listOfChildGroups.add(childGroup);
        }

        return new RecipeItemListAdapter(
                getActivity(),
                new SimpleCheckableExpandableListAdapter.OnAdapterUpdateListener() {
                    @Override
                    public void onCheckboxClick(CheckBox cb, int groupPosition, int childPosition) {
                        if (SimpleCheckableExpandableListAdapter.Position.isGroup(childPosition)) {
                            onGroupCheckboxClick(groupPosition, cb);
                        } else {
                            onItemClick(groupPosition, childPosition, cb);
                        }
                    }
                    @Override
                    public Boolean getCheckedStateFor(int groupPosition, int childPosition) {
                        if (SimpleCheckableExpandableListAdapter.Position.isGroup(childPosition)) {
                            return getGroupState(groupPosition);
                        } else {
                            return mSelection
                                    .contains(getItemForPosition(groupPosition, childPosition));
                        }
                    }
                    @Override
                    public void onGroupExpandOrCollapse() {
                    }
                    @Override
                    public int getTextColorForPosition(int groupPosition, int childPosition) {
                        if (!getItemForPosition(groupPosition, childPosition).isCompleted()) {
                            // Already on active list
                            return mActiveItemColor;
                        } else {
                            return mItemColor;
                        }
                    }
                    @Override
                    public int getTextColorForGroup(int groupPosition) {
                        return mGroupColor;
                    }
                },

                usageData,
                R.layout.checkable_expandable_group_item,
                new String[] {ROOT},
                new int[] {android.R.id.text1},

                listOfChildGroups,
                R.layout.checkable_expandable_list_item,
                new String[] {CHILD},
                new int[] {android.R.id.text1}
        );
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_show_completed).setVisible(false);
        menu.findItem(R.id.action_show_by_usage).setVisible(false);
        menu.findItem(R.id.action_sort_order).setVisible(false);
    }

    /**
     * @return
     * False - no item in the group is checked
     * True - all items in the group are checked
     * Null - some, but not all items in the group are checked
     */
    private Boolean getGroupState(int groupPosition) {
        ArrayList<Item> items = getItemsForGroup(groupPosition);
        boolean someIn = false;
        boolean allIn = true;
        for (int i = 0; i < items.size(); i++) {
            if (mSelection.contains(items.get(i))) {
                someIn = true;
            } else {
                allIn = false;
            }
        }
        if (!someIn) {
            return false;
        }
        if (allIn) {
            return true;
        }
        return null;
    }

    private Item getItemForPosition(int groupPosition, int childPosition) {
        int index = 0;
        for (int i = 0; i < mItems.length; i++) {
            if (mItems[i].usage.equals(mUsages.get(groupPosition))) {
                index++;
            }
            if (index == childPosition + 1) {
                return mItems[i];
            }
        }
        return null;
    }

    private ArrayList<Item> getItemsForGroup(int groupPosition) {
        ArrayList<Item> list = new ArrayList<>();
        for (int i = 0; i < mItems.length; i++) {
            if (mItems[i].usage.equals(mUsages.get(groupPosition))) {
                list.add(mItems[i]);
            }
        }
        return list;
    }

    private void onGroupCheckboxClick(int groupPosition, CheckBox cb) {
        ArrayList<Item> items = getItemsForGroup(groupPosition);
        if (cb.isChecked()) {
            for (int i = 0; i < items.size(); i++) {
                if (!mSelection.contains(items.get(i))) {
                    mSelection.add(items.get(i));
                }
            }
        } else {
            for (int i = 0; i < items.size(); i++) {
                mSelection.remove(items.get(i));
            }
        }
        mListAdapter.notifyDataSetInvalidated();
        updateFabVisibility();
    }

    private boolean onItemClick(int groupPosition, int childPosition, CheckBox cb) {
        Item item = getItemForPosition(groupPosition, childPosition);
        Boolean previousGroupSelected = getGroupState(groupPosition);
        if (cb.isChecked()) {
            mSelection.add(item);
        } else {
            mSelection.remove(item);
        }
        if (previousGroupSelected != getGroupState(groupPosition)) {
            mListAdapter.notifyDataSetInvalidated();
        }
        updateFabVisibility();
        return true;
    }

    private void addItems() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.re_add_items_title)
                .setMessage(getResources().getQuantityString(R.plurals.re_add_items_message,
                        mSelection.size(), mSelection.size()))
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Only close
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Add items back
                        for (Item item: mSelection) {
                            Item addItem = item.copy();
                            addItem.id = System.currentTimeMillis();
                            addItem.creationDate = System.currentTimeMillis();
                            addItem.creator = Settings.getString(getActivity(), Settings.WHOAMI);
                            addItem.updatedBy = Settings.getString(getActivity(), Settings.WHOAMI);
                            addItem.completionDate = -1;
                            RecipeItemsFragment.this.editItem(true, addItem, false, false);
                        }
                        // We're done
                        getActivity().finish();
                    }
                })
                .show();
    }

    private void updateFabVisibility() {
        mFab.setVisibility(mSelection.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private class RecipeItemListAdapter extends SimpleCheckableExpandableListAdapter<Item> {

        public RecipeItemListAdapter(Context context, OnAdapterUpdateListener updateListener,
                                      List<? extends Map<String, ?>> groupData, int groupLayout,
                                      String[] groupFrom, int[] groupTo,
                                      List<? extends List<? extends Map<String, ?>>> childData,
                                      int childLayout, String[] childFrom, int[] childTo) {
            super(context, updateListener, groupData, groupLayout, groupFrom, groupTo,
                    childData, childLayout, childFrom, childTo);
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            View v = super.getChildView(groupPosition, childPosition, isLastChild, convertView,
                    parent);
            // text1 / name set from parent
            TextView tv2 = (TextView) v.findViewById(android.R.id.text2);
            String info = getItemForPosition(groupPosition, childPosition).info;
            tv2.setText(info);
            tv2.setVisibility(TextUtils.isEmpty(info) ? View.GONE :View.VISIBLE);
            tv2.setTextColor(updateListener.getTextColorForPosition(groupPosition, childPosition));
            return v;
        }
    }
}
