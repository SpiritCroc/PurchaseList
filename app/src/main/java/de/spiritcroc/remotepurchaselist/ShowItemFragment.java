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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.io.File;

public class ShowItemFragment extends DialogFragment
        implements ServerCommunicator.OnHttpsSetupFinishListener {

    private static final String TAG = ShowItemsFragment.class.getSimpleName();
    private static final String KEY_ITEM = DialogFragment.class.getName() + ".item";

    private Item mItem;
    private ImageView mPictureView;

    private ItemInteractionListener mListener;

    public ShowItemFragment setItem(Item editItem) {
        mItem = editItem;
        return this;
    }

    public ShowItemFragment setItemInteractionListener(ItemInteractionListener listener) {
        mListener = listener;
        return this;
    }

    // https://github.com/bumptech/glide/issues/459#issuecomment-99960446
    class ViewImageTask extends AsyncTask<Object, Void, File> {
        private final Context context;
        public ViewImageTask(Context context) {
            this.context = context;
        }
        @Override protected File doInBackground(Object... params) {
            Object url = params[0]; // should be easy to extend to share multiple images at once
            try {
                // Glide v4
                return Glide
                        .with(context)
                        .downloadOnly()
                        .load(url)
                        .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get() // needs to be called on background thread
                        ;
            } catch (Exception ex) {
                Log.w("SHARE", "Sharing " + url + " failed", ex);
                return null;
            }
        }
        @Override protected void onPostExecute(File result) {
            if (result == null) { return; }
            Uri uri = FileProvider.getUriForFile(context, Constants.fileProvider(context), result);
            share(uri); // startActivity probably needs UI thread
        }

        private void share(Uri uri) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/jpeg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        if (savedInstanceState != null) {
            mItem = savedInstanceState.getParcelable(KEY_ITEM);
        }

        if (mItem == null) {
            Log.w(TAG, "onCreateDialog: missing item!");
            mItem = new Item();
        }

        final View dialogView = activity.getLayoutInflater()
                .inflate(R.layout.dialog_show_item, null);
        TextView infoView = (TextView) dialogView.findViewById(R.id.info);
        infoView.setText(mItem.info);
        infoView.setVisibility(TextUtils.isEmpty(mItem.info) ? View.GONE : View.VISIBLE);
        TextView usageView = (TextView) dialogView.findViewById(R.id.usage);
        usageView.setText(mItem.usage);
        usageView.setVisibility(TextUtils.isEmpty(mItem.usage) ? View.GONE : View.VISIBLE);
        TextView creatorView = (TextView) dialogView.findViewById(R.id.creator);
        creatorView.setText(TextUtils.isEmpty(mItem.updatedBy)
                        || mItem.creator.equals(mItem.updatedBy)
                ? getString(R.string.list_entry_creator, mItem.creator)
                : getString(R.string.list_entry_creator_updated_by, mItem.creator,
                        mItem.updatedBy));
        TextView dateView = (TextView) dialogView.findViewById(R.id.date);
        dateView.setText(ShowItemsFragment.getFormattedDate(getActivity(),
                mItem.completionDate > mItem.creationDate
                        ? mItem.completionDate : mItem.creationDate));
        mPictureView = (ImageView) dialogView.findViewById(R.id.picture);
        if (mItem.hasPicture()) {
            mPictureView.setVisibility(View.VISIBLE);
            if (ServerCommunicator.setupHttps(getActivity(), ShowItemFragment.this)) {
                onHttpsReady();
            }
            mPictureView.setOnClickListener(new View.OnClickListener() {
                private ViewImageTask mViewImageTask;
                @Override
                public void onClick(View v) {
                    if (mViewImageTask != null) {
                        mViewImageTask.cancel(false);
                    }
                    mViewImageTask = new ViewImageTask(mPictureView.getContext());
                    mViewImageTask.execute(mItem.getPictureUrl(getActivity()));
                }
            });
        } else {
            mPictureView.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(mItem.name)
                .setView(dialogView)
                .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Only close
                    }
                });
        if (mItem.isCompleted()) {
            builder.setNeutralButton(R.string.show_item_re_add,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mListener == null) {
                                Log.w(TAG, "Missing interaction listener");
                                return;
                            }
                            mListener.reAddItem(mItem);
                        }
            });
        } else {
            builder.setNeutralButton(R.string.show_item_modify,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mListener == null) {
                                Log.w(TAG, "Missing interaction listener");
                                return;
                            }
                            mListener.editItem(mItem);
                        }
            });
            builder.setPositiveButton(R.string.action_complete,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (mListener == null) {
                                Log.w(TAG, "Missing interaction listener");
                                return;
                            }
                            mListener.completeItem(mItem);
                        }
            });
        }
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mItem == null) {
            Log.w(TAG, "onSaveInstanceState: no item set!");
            mItem = new Item();
        }
        outState.putParcelable(KEY_ITEM, mItem);
    }

    public interface ItemInteractionListener {
        void completeItem(Item item);
        void editItem(Item item);
        void reAddItem(Item item);
    }

    @Override
    public void onHttpsReady() {
        Glide.with(mPictureView)
                .load(mItem.getPictureUrl(getActivity()))
                .error(R.drawable.ic_broken_picture)
                .fitCenter()
                .into(mPictureView);
    }
}
