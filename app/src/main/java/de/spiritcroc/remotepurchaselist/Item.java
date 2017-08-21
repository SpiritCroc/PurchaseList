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

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

public class Item implements Parcelable {
    public long id;
    public String name;
    public String info;
    public String creator;
    public long creationDate;
    public long completionDate;
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
        out.creator = in.creator;
        out.creationDate = in.creationDate;
        out.completionDate = in.completionDate;
        out.mIsCached = in.mIsCached;
        return out;
    }

    private static final String SP_ID = "_id";
    private static final String SP_NAME = "_name";
    private static final String SP_INFO = "_info";
    private static final String SP_CREATOR = "_creator";
    private static final String SP_CREATION_DATE = "_creation_date";
    private static final String SP_COMPLETION_DATE = "_completion_date";

    void saveToCachePreferences(SharedPreferences.Editor editor, String baseKey) {
        editor.putLong(baseKey + SP_ID, id);
        editor.putString(baseKey + SP_NAME, name);
        editor.putString(baseKey + SP_INFO, info);
        editor.putString(baseKey + SP_CREATOR, creator);
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
            result.creator = sp.getString(baseKey + SP_CREATOR, null);
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
                .remove(baseKey + SP_CREATOR)
                .remove(baseKey + SP_CREATION_DATE)
                .remove(baseKey + SP_COMPLETION_DATE);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + id + ";" + name + ";" + info + ";" + creator +
                ";" + creationDate + ";" + completionDate + ")";
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
        out.writeStringArray(new String[]{name, info, creator});
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
            item.creator = strings[2];
            return item;
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
}
