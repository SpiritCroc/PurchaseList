package de.spiritcroc.remotepurchaselist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class BossUtils {

    private static final String FRAGMENT_TAG_EDIT_ITEM_DIALOG = BossUtils.class.getName() +
            ".edit_item_dialog";

    public static boolean isBoss(Context context) {
        return Settings.getBoolean(context, Settings.BOSS_MODE);
    }

    public static void showBossCompletedItemDialog(final Activity activity, final Item item,
                                                   final ShowItemsFragment callback) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.boss_completed_dialog_title)
                .setMessage(activity.getString(R.string.boss_completed_dialog_summary, item.name))
                .setPositiveButton(R.string.boss_completed_dialog_modify,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new EditItemFragment().setEditItem(item)
                                        .setOnEditResultListener(callback)
                                        .show(activity.getFragmentManager(),
                                                FRAGMENT_TAG_EDIT_ITEM_DIALOG);
                            }
                })
                .setNeutralButton(R.string.boss_completed_dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                deleteItem(activity, item);
                                if (callback != null) {
                                    callback.reload();
                                }
                            }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Just close
                    }
                })
                .show();
    }

    private static void deleteItem(Context context, Item item) {
        String params = ServerCommunicator.addParameter(null, Constants.JSON.ID, "" + item.id);
        params = ServerCommunicator.addParameter(params,
                Constants.JSON.CREATOR, Settings.getString(context, Settings.WHOAMI));
        HttpPostOfflineCache.addItemToDeleteCache(context, Constants.SITE.DELETE_ITEM,
                params, item.id);
    }

}
