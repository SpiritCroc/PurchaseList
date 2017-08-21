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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

public class ShowCompletedItemsFragment extends ShowItemsFragment {

    @Override
    protected void setTitle(CharSequence title) {
        super.setTitle(getString(R.string.app_title_completed, title));
    }

    @Override
    protected String getRequestSite() {
        return Constants.SITE.GET_COMPLETED_LIST;
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.findItem(R.id.action_show_completed).setVisible(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        // Custom click listener
        ((ListView) view.findViewById(R.id.item_list))
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        showReAddDialog(mListAdapter.getItem(i));
                    }
                });
        return view;
    }

    private void showReAddDialog(final Item oldItem) {
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
                        ShowCompletedItemsFragment.this.editItem(true, addItem, false);
                    }
                })
                .show();
    }

    @Override
    protected void updateEmptyListImage(ImageView imageView) {
        imageView.setVisibility(Settings.getBoolean(getActivity(), Settings.DINO)
                ? View.VISIBLE : View.GONE);
        imageView.setImageResource(R.drawable.dino_hungry);
    }
}
