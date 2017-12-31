package com.labs.pbrother.freegallery.controller.db

import android.provider.BaseColumns

/**
 * Created by simon on 21.02.17.
 */
class DBContract {

    abstract class CollectionMetaEntry {
        companion object {
            val TABLE_NAME = "collectionmeta"
            val _ID = BaseColumns._ID
            val COLUMN_COLLECTION_ID = "collectionid"
            val COLUMN_LOVED = "lovestatus"
            val COLUMN_COLOR = "colorid"
        }
    }

    // deprecated
    abstract class Tag {
        companion object {
            val TABLE_NAME = "tags"
            val _ID = BaseColumns._ID
            val COLUMN_ITEM_ID = "itemid"
            val COLUMN_TAG = "tag"
        }
    }

    abstract class TagUnique {
        companion object {
            val TABLE_NAME = "tagsUnique"
            val _ID = BaseColumns._ID
            val COLUMN_ITEM_TAG = "itemTag"
            val COLUMN_ITEM_ID = "itemid"
            val COLUMN_TAG = "tag"
        }
    }

    abstract class Trash {
        companion object {
            val TABLE_NAME = "trash"
            val _ID = BaseColumns._ID
            val COLUMN_ITEM_PATH = "itemid"
            val COLUMN_MEDIATYPE = "mediatype"
        }
    }

}
