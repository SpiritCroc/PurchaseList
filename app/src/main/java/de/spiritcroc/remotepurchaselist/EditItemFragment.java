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
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

public class EditItemFragment extends DialogFragment {

    public static final String KEY_ADD_ITEM = DialogFragment.class.getName() + ".add_item";
    public static final String KEY_EDIT_ITEM = DialogFragment.class.getName() + ".edit_item";

    private boolean mAddItem = true;
    private Item mEditItem;

    private OnEditItemResultListener mListener;

    private TextView mEditName;
    private TextView mEditInfo;

    public EditItemFragment setEditItem(Item editItem) {
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
        if (savedInstanceState != null) {
            mAddItem = savedInstanceState.getBoolean(KEY_ADD_ITEM);
            mEditItem = savedInstanceState.getParcelable(KEY_EDIT_ITEM);
        }
        if (mAddItem) {
            // Don't discard it by mistake
            setCancelable(false);
        }

        final View dialogView = getActivity().getLayoutInflater()
                .inflate(R.layout.dialog_edit_item, null);
        mEditName = (TextView) dialogView.findViewById(R.id.name_edit);
        mEditInfo = (TextView) dialogView.findViewById(R.id.info_edit);
        if (mEditItem != null) {
            mEditName.setText(mEditItem.name);
            mEditInfo.setText(mEditItem.info);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
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
                                Item preview = new Item();
                                preview.id = mAddItem ? System.currentTimeMillis() : mEditItem.id;
                                preview.name = name;
                                preview.info = mEditInfo.getText().toString();
                                preview.creator =
                                        Settings.getString(getActivity(), Settings.WHOAMI);
                                preview.creationDate = System.currentTimeMillis();
                                preview.completionDate = -1;
                                mListener.onEditItemResult(mAddItem, preview);
                                dismiss();
                            }
                        });
            }
        });
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
    }

    public interface OnEditItemResultListener {
        void onEditItemResult(boolean add, Item resultItem);
    }
}
