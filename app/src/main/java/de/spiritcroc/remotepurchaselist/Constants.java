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

public final class Constants {

    // Don't instantiate
    private Constants() {}

    public static final class SITE {
        public static final String GET_LIST = "get_list.php";
        public static final String GET_COMPLETED_LIST = "get_completed_list.php";
        public static final String GET_FULL_LIST = "get_full_list.php";
        public static final String INSERT_ITEM = "insert_item.php";
        public static final String UPDATE_ITEM = "update_item.php";
        public static final String COMPLETE_ITEMS = "complete_items.php";
    }
    public static final class JSON {
        public static final String SUCCESS = "success";
        public static final String MESSAGE = "message";
        public static final String ITEMS = "items";

        public static final String ID = "ID";
        public static final String NAME = "NAME";
        public static final String INFO = "INFO";
        public static final String CREATOR = "CREATOR";
        public static final String UPDATED_BY = "UPDATED_BY";
        public static final String CREATION_DATE = "CREATION_DATE";
        public static final String COMPLETION_DATE = "COMPLETION_DATE";

        public static final String SELECTION = "SELECTION";
        public static final String SEARCH = "SEARCH";
        public static final String SORT_ORDER = "SORTORDER";
        public static final String GROUPING = "GROUPING";
        public static final String ORDER_BY = "ORDER BY ";
        public static final String ASC = " ASC";
        public static final String DESC = " DESC";

        public static final String HIDE_OLDER_DUPLICATES = "hideOldDuplicates";
    }
}
