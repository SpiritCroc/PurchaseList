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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;

public class EditItemFragment extends DialogFragment
        implements ServerCommunicator.OnHttpsSetupFinishListener {

    private static final String TAG = EditItemFragment.class.getSimpleName();

    private static final String KEY_ADD_ITEM = DialogFragment.class.getName() + ".add_item";
    private static final String KEY_EDIT_ITEM = DialogFragment.class.getName() + ".edit_item";
    private static final String KEY_INIT_ITEM = DialogFragment.class.getName() + ".init_item";

    private static final int RESULT_CODE_ADD_LOCAL_PICTURE = 1;
    private static final int RESULT_CODE_ADD_CAPTURE_PICTURE = 2;

    private boolean mAddItem = true;
    private Item mInitItem;
    private Item mEditItem;

    private enum PictureChangeAction {
        NONE,
        //REMOVE, // clear url field to remove: mEditPictureUrl.setText("");
        EXTERNAL,
        LOCAL,
    }
    private PictureChangeAction mPictureChangeAction = PictureChangeAction.NONE;
    private String mPictureUrl;
    private File mCaptureFile;

    private OnEditItemResultListener mListener;

    private AutoCompleteTextView mEditName;
    private EditText mEditInfo;
    private AutoCompleteTextView mEditUsage;
    private EditText mEditPictureUrl;
    private View mEditPictureUrlLayout;
    private ImageView mEditPicture;
    private PopupMenu mEditPictureMenu;

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
        mEditInfo = (EditText) dialogView.findViewById(R.id.info_edit);
        mEditUsage = (AutoCompleteTextView) dialogView.findViewById(R.id.usage_edit);
        mEditPictureUrl = (EditText) dialogView.findViewById(R.id.picture_url_edit);
        mEditPictureUrlLayout = dialogView.findViewById(R.id.picture_url_edit_layout);
        mEditPicture = (ImageView) dialogView.findViewById(R.id.picture_edit);
        if (mEditItem != null) {
            mEditName.setText(mEditItem.name);
            mEditInfo.setText(mEditItem.info);
            mEditUsage.setText(mEditItem.usage);
            if (TextUtils.isEmpty(mEditItem.localPictureUrl)) {
                mPictureUrl = mEditItem.pictureUrl;
                mEditPictureUrl.setText(mPictureUrl);
            } else {
                mEditPictureUrl.setText("");
            }
        }
        updatePicturePreview();
        mEditPictureMenu = new PopupMenu(getActivity(), mEditPicture);
        mEditPictureMenu.inflate(R.menu.popup_edit_picture);
        mEditPictureMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_picture_local:
                        openLocalPictureChooser();
                        return true;
                    case R.id.action_picture_url:
                        if (mPictureChangeAction == PictureChangeAction.LOCAL ||
                                mPictureUrl == null ||
                                !mPictureUrl.equals(completePictureUrl(mPictureUrl))) {
                            // Clear
                            mEditPictureUrl.setText("");
                        }
                        mEditPictureUrlLayout.setVisibility(View.VISIBLE);
                        return true;
                    case R.id.action_picture_remove:
                        // Set to "external" empty URL
                        mEditPictureUrl.setText("");
                        mEditPictureUrlLayout.setVisibility(View.GONE);
                        return true;
                    case R.id.action_picture_reset:
                        resetPicture();
                        return true;
                    case R.id.action_picture_capture:
                        openPictureCaptureChooser();
                        return true;
                    default:
                        return false;
                }
            }
        });

        mEditName.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1,
                SuggestionsRetriever.getCachedSuggestions(activity)));
        mEditName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (TextUtils.isEmpty(mEditInfo.getText())) {
                    Item suggestionItem = SuggestionsRetriever
                            .getSuggestionItemInfosByName(activity, mEditName.getText().toString());
                    mEditInfo.setText(suggestionItem.info);
                    mEditInfo.setSelectAllOnFocus(true);
                    // mEditPictureUrls entered URLs are treated as absolute, so do this secretly
                    /*
                    mEditPictureUrl.setText(suggestionItem.pictureUrl);
                    mEditPictureUrl.setSelectAllOnFocus(true);
                    */
                    mEditPictureUrl.setText("");// calls pictureChangeCleanup();
                    mEditPictureUrlLayout.setVisibility(View.GONE);
                    mPictureUrl = suggestionItem.pictureUrl;
                    mPictureChangeAction = PictureChangeAction.EXTERNAL;
                    updatePicturePreview();
                } else {
                    mEditInfo.setSelectAllOnFocus(false);
                    mEditPictureUrl.setSelectAllOnFocus(false);
                }
            }
        });
        mEditUsage.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1,
                UsageSuggestionsRetriever.getCachedSuggestions(activity)));
        mEditPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Item preview = getPreview(false);
                Menu menu = mEditPictureMenu.getMenu();
                menu.findItem(R.id.action_picture_remove).setVisible(preview.hasPicture());
                menu.findItem(R.id.action_picture_url).setVisible(
                        mEditPictureUrlLayout.getVisibility() != View.VISIBLE);
                menu.findItem(R.id.action_picture_reset).setVisible(mInitItem != null &&
                        mPictureChangeAction != PictureChangeAction.NONE);
                mEditPictureMenu.show();
            }
        });
        mEditPictureUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void afterTextChanged(Editable editable) {
                pictureChangeCleanup();
                readPictureUrl();
                updatePicturePreview();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(mAddItem ? R.string.add_item_title : R.string.edit_item_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Clean up
                        pictureChangeCleanup();
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
                                Item preview = getPreview(true);
                                if (preview == null) {
                                    return;
                                }
                                if (mInitItem == null || !preview.name.equals(mInitItem.name)
                                        || !preview.info.equals(mInitItem.info)
                                        || !preview.usage.equals(mInitItem.usage)
                                        || !preview.pictureUrl.equals(mInitItem.pictureUrl)
                                        || !preview.localPictureUrl.equals(
                                                mInitItem.localPictureUrl)) {
                                    // Something has changed
                                    mListener.onEditItemResult(mAddItem, preview,
                                            mPictureChangeAction == PictureChangeAction.LOCAL,
                                            mPictureChangeAction != PictureChangeAction.EXTERNAL);
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

    private Item getPreview(boolean enforceCorrectness) {
        String name = mEditName.getText().toString();
        if (enforceCorrectness && TextUtils.isEmpty(name)) {
            mEditName.setError(getString(R.string.error_empty));
            return null;
        }
        String whoami = Settings.getString(getActivity(), Settings.WHOAMI);
        Item preview = new Item();
        preview.id = mAddItem ? System.currentTimeMillis() : mEditItem.id;
        preview.name = name;
        preview.info = mEditInfo.getText().toString();
        preview.usage = mEditUsage.getText().toString();
        switch (mPictureChangeAction) {
            case NONE:
                if (mInitItem == null) {
                    preview.pictureUrl = "";
                    preview.localPictureUrl = "";
                } else {
                    preview.pictureUrl = mInitItem.pictureUrl;
                    preview.localPictureUrl = mInitItem.localPictureUrl;
                }
                break;
            case EXTERNAL:
                preview.pictureUrl = mPictureUrl;
                preview.localPictureUrl = "";
                break;
            case LOCAL:
                preview.localPictureUrl = mPictureUrl;
                preview.pictureUrl = "";
                break;
            default:
                Log.e(TAG, "Unhandled picture change action " +
                        mPictureChangeAction);
                if (mInitItem == null) {
                    preview.pictureUrl = "";
                    preview.localPictureUrl = "";
                } else {
                    preview.pictureUrl = mInitItem.pictureUrl;
                    preview.localPictureUrl = mInitItem.localPictureUrl;
                }
                break;
        }
        preview.creator = mAddItem ? whoami : mEditItem.creator;
        preview.updatedBy = whoami;
        preview.creationDate = System.currentTimeMillis();
        preview.completionDate = mAddItem ? -1 : mEditItem.completionDate;
        return preview;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Clean up
        pictureChangeCleanup();
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
        if (mEditUsage != null) {
            mEditItem.usage = mEditUsage.getText().toString();
        }
        if (mEditPictureUrl != null) {
            mEditItem.pictureUrl = mEditPictureUrl.getText().toString();
        }
        outState.putBoolean(KEY_ADD_ITEM, mAddItem);
        outState.putParcelable(KEY_EDIT_ITEM, mEditItem);
        if (!mAddItem) {
            outState.putParcelable(KEY_INIT_ITEM, mInitItem);
        }
    }

    private void resetPicture() {
        if (mInitItem != null && TextUtils.isEmpty(mInitItem.localPictureUrl)) {
            // This also sets mPictureUrl
            mEditPictureUrl.setText(mInitItem.pictureUrl);
        } else {
            mEditPictureUrl.setText("");
        }
        mPictureChangeAction = PictureChangeAction.NONE;
        mEditPictureUrlLayout.setVisibility(View.GONE);
        updatePicturePreview();
    }

    private void openLocalPictureChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
            Log.e(TAG, "No activity to choose pictures found");
            Toast.makeText(getActivity(), R.string.toast_no_resolver_for_action, Toast.LENGTH_LONG)
                    .show();
        } else {
            startActivityForResult(intent, RESULT_CODE_ADD_LOCAL_PICTURE);
        }
    }

    private void openPictureCaptureChooser() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mCaptureFile = LocalPictureHandler.generateCapturePictureFile(getActivity(), "jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getActivity(),
                "de.spiritcroc.remotepurchaselist.fileprovider", mCaptureFile));
        if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
            Log.e(TAG, "No activity to capture pictures found");
            Toast.makeText(getActivity(), R.string.toast_no_resolver_for_action, Toast.LENGTH_LONG)
                    .show();
        } else {
            startActivityForResult(intent, RESULT_CODE_ADD_CAPTURE_PICTURE);
        }
    }

    private void updatePicturePreview() {
        if (ServerCommunicator.setupHttps(getActivity(), EditItemFragment.this)) {
            onHttpsReady();
        }
    }

    private void pictureChangeCleanup() {
        if (mPictureChangeAction == PictureChangeAction.LOCAL && !TextUtils.isEmpty(mPictureUrl)) {
            LocalPictureHandler.removeLocalPicture(mPictureUrl);
            mPictureUrl = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case RESULT_CODE_ADD_LOCAL_PICTURE:
                    // Clean up previous changes
                    pictureChangeCleanup();
                    // Import new picture
                    new ImportLocalPictureTask().execute(data);
                    break;
                case RESULT_CODE_ADD_CAPTURE_PICTURE:
                    // Clean up previous changes
                    pictureChangeCleanup();
                    // Use new picture
                    if (mCaptureFile.exists()) {
                        finishLocalPictureImport(mCaptureFile.toURI().toString());
                    }
                    break;
            }
        }
    }

    private class ImportLocalPictureTask extends AsyncTask<Intent, Void, String> {
        private Dialog mDialog;
        private Context mContext;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mContext = getActivity().getApplicationContext();
            View progressView = getActivity().getLayoutInflater()
                    .inflate(R.layout.dialog_progress_indeterminate, null);
            TextView messageView = progressView.findViewById(R.id.progress_message);
            messageView.setText(R.string.import_local_picture_dialog_message);
            mDialog = new AlertDialog.Builder(mContext)
                    .setCancelable(false)
                    .setTitle(R.string.import_local_picture_dialog_title)
                    .create();
        }
        @Override
        protected String doInBackground(Intent... data) {
            if (data.length != 1) {
                Log.e(TAG, "Invalid parameter count for ImportLocalPictureTask: " + data.length);
                return null;
            }
            return LocalPictureHandler.importLocalPicture(mContext, data[0]);
        }
        @Override
        protected void onPostExecute(String url) {
            mDialog.dismiss();
            finishLocalPictureImport(url);
        }
    }

    private void finishLocalPictureImport(String url) {
        if (!TextUtils.isEmpty(url)) {
            // Erase editUrl first - this will lead to change of picture URL and action,
            // so change these after that
            mEditPictureUrl.setText("");
            mEditPictureUrlLayout.setVisibility(View.GONE);
            mPictureUrl = url;
            mPictureChangeAction = PictureChangeAction.LOCAL;
            updatePicturePreview();
        }
    }

    private String completePictureUrl(String url) {
        if (!TextUtils.isEmpty(url) && !url.contains("://")) {
            return  "https://" + url;
        }
        return url;
    }

    private void readPictureUrl() {
        mPictureUrl = mEditPictureUrl.getText().toString();
        mPictureUrl = completePictureUrl(mPictureUrl);
        mPictureChangeAction = PictureChangeAction.EXTERNAL;
    }

    @Override
    public void onHttpsReady() {
        Item picturePreviewItem = new Item();
        if (mPictureChangeAction == PictureChangeAction.LOCAL) {
            picturePreviewItem.localPictureUrl = mPictureUrl;
        } else if (mPictureChangeAction == PictureChangeAction.NONE) {
            if (mInitItem != null) {
                picturePreviewItem = mInitItem;
            }
        } else {
            picturePreviewItem.pictureUrl = mPictureUrl;
        }
        Glide.with(mEditPicture)
                .load(picturePreviewItem.getPictureUrl(getActivity()))
                .error(R.drawable.ic_broken_picture)
                .fallback(R.drawable.ic_picture_add)
                .centerCrop()
                .into(mEditPicture);
    }

    public interface OnEditItemResultListener {
        void onEditItemResult(boolean add, Item resultItem, boolean localPictureUpload,
                              boolean skipUrlUpdate);
    }
}
