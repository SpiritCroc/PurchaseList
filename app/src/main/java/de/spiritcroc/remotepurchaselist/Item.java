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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Item implements Parcelable {
    public long id;
    public String name;
    public String info;
    public String usage;
    public String creator;
    public String updatedBy;
    public String pictureUrl = "";
    public String localPictureUrl = "";
    public long creationDate;
    public long completionDate = -1;
    private boolean mIsCached = false;

    public boolean isCached() {
        return mIsCached;
    }

    public Item copy() {
        return copy(this);
    }

    public static Item copy(Item in) {
        Item out = new Item();
        out.id = in.id;
        out.name = in.name;
        out.info = in.info;
        out.usage = in.usage;
        out.creator = in.creator;
        out.updatedBy = in.updatedBy;
        out.pictureUrl = in.pictureUrl;
        out.localPictureUrl = in.localPictureUrl;
        out.creationDate = in.creationDate;
        out.completionDate = in.completionDate;
        out.mIsCached = in.mIsCached;
        return out;
    }

    private static final String SP_ID = "_id";
    private static final String SP_NAME = "_name";
    private static final String SP_INFO = "_info";
    private static final String SP_USAGE = "_usage";
    private static final String SP_CREATOR = "_creator";
    private static final String SP_UPDATED_BY = "_updated_by";
    private static final String SP_PICTURE_URL = "_picture_url";
    private static final String SP_LOCAL_PICTURE_URL = "_local_picture_url";
    private static final String SP_CREATION_DATE = "_creation_date";
    private static final String SP_COMPLETION_DATE = "_completion_date";

    void saveToCachePreferences(SharedPreferences.Editor editor, String baseKey) {
        editor.putLong(baseKey + SP_ID, id);
        editor.putString(baseKey + SP_NAME, name);
        editor.putString(baseKey + SP_INFO, info);
        editor.putString(baseKey + SP_USAGE, usage);
        editor.putString(baseKey + SP_CREATOR, creator);
        editor.putString(baseKey + SP_UPDATED_BY, updatedBy);
        editor.putString(baseKey + SP_PICTURE_URL, pictureUrl);
        editor.putString(baseKey + SP_LOCAL_PICTURE_URL, localPictureUrl);
        editor.putLong(baseKey + SP_CREATION_DATE, creationDate);
        editor.putLong(baseKey + SP_COMPLETION_DATE, completionDate);
    }
    static Item loadFromCachePreferences(SharedPreferences sp, String baseKey) {
        if (sp.contains(baseKey + SP_ID)) {
            Item result = new Item();
            result.mIsCached = true;
            result.id = sp.getLong(baseKey + SP_ID, 0);
            result.name = sp.getString(baseKey + SP_NAME, null);
            result.info = sp.getString(baseKey + SP_INFO, null);
            result.usage = sp.getString(baseKey + SP_USAGE, null);
            result.creator = sp.getString(baseKey + SP_CREATOR, null);
            result.updatedBy = sp.getString(baseKey + SP_UPDATED_BY, null);
            result.pictureUrl = sp.getString(baseKey + SP_PICTURE_URL, null);
            result.localPictureUrl = sp.getString(baseKey + SP_LOCAL_PICTURE_URL, null);
            result.creationDate = sp.getLong(baseKey + SP_CREATION_DATE, 0);
            result.completionDate = sp.getLong(baseKey + SP_COMPLETION_DATE, 0);
            return result;
        } else {
            return null;
        }
    }
    static void deleteCachePreference(SharedPreferences.Editor editor, String baseKey) {
        editor.remove(baseKey + SP_ID)
                .remove(baseKey + SP_NAME)
                .remove(baseKey + SP_INFO)
                .remove(baseKey + SP_USAGE)
                .remove(baseKey + SP_CREATOR)
                .remove(baseKey + SP_UPDATED_BY)
                .remove(baseKey + SP_PICTURE_URL)
                .remove(baseKey + SP_LOCAL_PICTURE_URL)
                .remove(baseKey + SP_CREATION_DATE)
                .remove(baseKey + SP_COMPLETION_DATE);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + id + ";" + name + ";" + info + ";" + usage +
                ";" + creator + ";" + creationDate + ";" + completionDate + ";" + pictureUrl +
                ";" + localPictureUrl + ")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Item && ((Item) o).id == id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLongArray(new long[]{id, creationDate, completionDate});
        out.writeStringArray(new String[]{name, info, usage, creator, updatedBy, pictureUrl,
                localPictureUrl});
    }

    public static final Parcelable.Creator<Item> CREATOR
            = new Parcelable.Creator<Item>() {

        @Override
        public Item createFromParcel(Parcel in) {
            long[] longs = in.createLongArray();
            String[] strings = in.createStringArray();
            Item item = new Item();
            item.id = longs[0];
            item.creationDate = longs[1];
            item.completionDate = longs[2];
            item.name = strings[0];
            item.info = strings[1];
            item.usage = strings[2];
            item.creator = strings[3];
            item.updatedBy = strings[4];
            item.pictureUrl = strings[5];
            item.localPictureUrl = strings[6];
            return item;
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    public boolean isCompleted() {
        return completionDate > 0;
    }

    public boolean hasPicture() {
        return !TextUtils.isEmpty(pictureUrl) || !TextUtils.isEmpty(localPictureUrl);
    }

    public Object getPictureUrl(Context context) {
        if (TextUtils.isEmpty(localPictureUrl)) {
            return ServerCommunicator.getPictureUrl(context, this);
        } else {
            return localPictureUrl;
        }
    }
}
