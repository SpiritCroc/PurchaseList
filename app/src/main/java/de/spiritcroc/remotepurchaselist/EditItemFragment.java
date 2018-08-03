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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class EditItemFragment extends DialogFragment {

    private static final String KEY_ADD_ITEM = DialogFragment.class.getName() + ".add_item";
    private static final String KEY_EDIT_ITEM = DialogFragment.class.getName() + ".edit_item";
    private static final String KEY_INIT_ITEM = DialogFragment.class.getName() + ".init_item";

    private boolean mAddItem = true;
    private Item mInitItem;
    private Item mEditItem;

    private OnEditItemResultListener mListener;

    private AutoCompleteTextView mEditName;
    private TextView mEditInfo;

    public EditItemFragment setEditItem(Item editItem) {
        mInitItem = editItem;
        mEditItem = editItem.copy();
        mAddItem = false;
        return this;
    }

    public EditItemFragment setOnEditResultListener(OnEditItemResultListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        if (savedInstanceState != null) {
            mAddItem = savedInstanceState.getBoolean(KEY_ADD_ITEM);
            mEditItem = savedInstanceState.getParcelable(KEY_EDIT_ITEM);
            if (!mAddItem) {
                mInitItem = savedInstanceState.getParcelable(KEY_INIT_ITEM);
            }
        }
        if (mAddItem) {
            // Don't discard it by mistake
            setCancelable(false);
        }

        final View dialogView = activity.getLayoutInflater()
                .inflate(R.layout.dialog_edit_item, null);
        mEditName = (AutoCompleteTextView) dialogView.findViewById(R.id.name_edit);
        mEditInfo = (TextView) dialogView.findViewById(R.id.info_edit);
        if (mEditItem != null) {
            mEditName.setText(mEditItem.name);
            mEditInfo.setText(mEditItem.info);
        }
        mEditName.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1,
                SuggestionsRetriever.getCachedSuggestions(activity)));
        mEditName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (TextUtils.isEmpty(mEditInfo.getText())) {
                    mEditInfo.setText(SuggestionsRetriever.getCorrespondingInfoSuggestion(activity,
                            mEditName.getText().toString()));
                    mEditInfo.setSelectAllOnFocus(true);
                } else {
                    mEditInfo.setSelectAllOnFocus(false);
                }
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(mAddItem ? R.string.add_item_title : R.string.edit_item_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Only close dialog
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String name = mEditName.getText().toString();
                                if (TextUtils.isEmpty(name)) {
                                    mEditName.setError(getString(R.string.error_empty));
                                    return;
                                }
                                String whoami = Settings.getString(activity, Settings.WHOAMI);
                                Item preview = new Item();
                                preview.id = mAddItem ? System.currentTimeMillis() : mEditItem.id;
                                preview.name = name;
                                preview.info = mEditInfo.getText().toString();
                                preview.creator = mAddItem ? whoami : mEditItem.creator;
                                preview.updatedBy = whoami;
                                preview.creationDate = System.currentTimeMillis();
                                preview.completionDate = mAddItem ? -1 : mEditItem.completionDate;
                                if (mInitItem == null || !preview.name.equals(mInitItem.name)
                                        || !preview.info.equals(mInitItem.info)) {
                                    // Something has changed
                                    mListener.onEditItemResult(mAddItem, preview);
                                } // else: no update needed, only close dialog
                                dismiss();
                            }
                        });
            }
        });
        // We want to edit
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        );
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEditItem == null) {
            mEditItem = new Item();
        }
        if (mEditName != null) {
            mEditItem.name = mEditName.getText().toString();
        }
        if (mEditInfo != null) {
            mEditItem.info = mEditInfo.getText().toString();
        }
        outState.putBoolean(KEY_ADD_ITEM, mAddItem);
        outState.putParcelable(KEY_EDIT_ITEM, mEditItem);
        if (!mAddItem) {
            outState.putParcelable(KEY_INIT_ITEM, mInitItem);
        }
    }

    public interface OnEditItemResultListener {
        void onEditItemResult(boolean add, Item resultItem);
    }
}
