package com.labs.pbrother.freegallery.controller.db

import android.provider.BaseColumns

/**
 * Created by simon on 21.02.17.
 */
class DBContract {

    abstract class CollectionMetaEntry {
        companion object {
            val _ID = BaseColumns._ID
            val TABLE_NAME = "collectionmeta"
            val COLUMN_NAME_COLLECTION_ID = "collectionid"
            val COLUMN_NAME_LOVED = "lovestatus"
            val COLUMN_NAME_COLOR = "colorid"
        }
    }

    abstract class Tag {
        companion object {
            val _ID = BaseColumns._ID
            val TABLE_NAME = "tags"
            val COLUMN_NAME_ITEM_ID = "itemid"
            val COLUMN_NAME_TAG = "tag"
        }
    }

    abstract class Trash {
        companion object {
            val _ID = BaseColumns._ID
            val TABLE_NAME = "trash"
            val COLUMN_NAME_ITEM_PATH = "itemid"
            val COLUMN_NAME_MEDIATYPE = "mediatype"
        }
    }

}
