package com.labs.pbrother.freegallery.controller.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.labs.pbrother.freegallery.controller.CollectionMeta
import com.labs.pbrother.freegallery.controller.ItemTag
import com.labs.pbrother.freegallery.controller.TrashItem
import org.jetbrains.anko.db.*
import com.labs.pbrother.freegallery.controller.db.DBContract.*

/**
 * Created by simon on 23.08.17.
 */
class MyDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, MyDatabaseOpenHelper.DB_NAME, null, MyDatabaseOpenHelper.DB_VERSION) {
    companion object {
        val DB_NAME = "FG.db"
        val DB_VERSION = 37

        private var instance: MyDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): MyDatabaseOpenHelper {
            if (instance == null) {
                instance = MyDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance as MyDatabaseOpenHelper
        }
    }

    private fun createCollectionMetaTable(db: SQLiteDatabase) = db.createTable(CollectionMetaEntry.TABLE_NAME, true,
            CollectionMetaEntry._ID to INTEGER + UNIQUE,
            CollectionMetaEntry.COLUMN_COLLECTION_ID to TEXT + UNIQUE + PRIMARY_KEY,
            CollectionMetaEntry.COLUMN_LOVED to INTEGER,
            CollectionMetaEntry.COLUMN_COLOR to INTEGER
    )

    private fun createTagTable(db: SQLiteDatabase) = db.createTable(Tag.TABLE_NAME, true,
            Tag._ID to INTEGER + UNIQUE,
            Tag.COLUMN_ITEM_TAG to TEXT + UNIQUE + PRIMARY_KEY,
            Tag.COLUMN_ITEM_ID to TEXT,
            Tag.COLUMN_TAG to TEXT
    )

    private fun createTrashTable(db: SQLiteDatabase) = db.createTable(Trash.TABLE_NAME, true,
            Trash._ID to INTEGER + UNIQUE,
            Trash.COLUMN_ITEM_PATH to TEXT + UNIQUE + PRIMARY_KEY,
            Trash.COLUMN_MEDIATYPE to INTEGER
    )

    /*
    private fun createNotTrashedTaggedItemsView(db: SQLiteDatabase) = db.execSQL(
            "CREATE VIEW "
                    + UntrashedTagged.VIEW_NAME
                    + " AS "
                    + "SELECT DISTINCT "
                    + Tag.TABLE_NAME + "." + Tag.COLUMN_ITEM_ID + " AS " + UntrashedTagged.COLUMN_ITEM_ID + ", "
                    + Tag.TABLE_NAME + "." + Tag.COLUMN_ITEM_TAG + " AS " + UntrashedTagged.COLUMN_TAG  + ", "
                    + Trash.TABLE_NAME + "." + Trash.COLUMN_ITEM_PATH + " AS " + UntrashedTagged.COLUMN_TRASH_ITEM_ID
                    + " FROM " + Tag.TABLE_NAME + " LEFT OUTER JOIN " + Trash.TABLE_NAME
                    + " ON " + Tag.TABLE_NAME + "." + Tag.COLUMN_ITEM_ID + " = " + Trash.TABLE_NAME + "." + Trash.COLUMN_ITEM_PATH
    )*/

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
        createCollectionMetaTable(db)
        createTagTable(db)
        createTrashTable(db)
        //createNotTrashedTaggedItemsView(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
        if (oldVersion < 30) update29(db, oldVersion, newVersion)
        onCreate(db)
    }

    fun update29(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // created new tag table with combined unique key here
        // first moves all entries to the new table
        // then drops old table

        // migrate collection meta
        val collectionMetaParser = rowParser { collectionId: String, loved: Int, color: Int ->
            CollectionMeta(collectionId, 1 == loved, color)
        }

        val collectionMetas = db.select(CollectionMetaEntry.TABLE_NAME,
                CollectionMetaEntry.COLUMN_COLLECTION_ID,
                CollectionMetaEntry.COLUMN_LOVED,
                CollectionMetaEntry.COLUMN_COLOR
        ).parseList(collectionMetaParser)

        db.dropTable(CollectionMetaEntry.TABLE_NAME, true)
        createCollectionMetaTable(db)

        collectionMetas.forEach {
            try {
                db.insertOrThrow(CollectionMetaEntry.TABLE_NAME,
                        CollectionMetaEntry.COLUMN_COLLECTION_ID to it.id,
                        CollectionMetaEntry.COLUMN_LOVED to it.loved,
                        CollectionMetaEntry.COLUMN_COLOR to it.color)
            } catch (e: Exception) {
            }
        }

        // migrate trash
        val trashParser = rowParser { path: String, type: Int -> TrashItem(path, type) }
        val trashs = db.select(Trash.TABLE_NAME,
                Trash.COLUMN_ITEM_PATH,
                Trash.COLUMN_MEDIATYPE).parseList(trashParser)

        db.dropTable(Trash.TABLE_NAME, true)
        createTrashTable(db)

        trashs.forEach {
            try {
                db.insertOrThrow(Trash.TABLE_NAME,
                        Trash.COLUMN_ITEM_PATH to it.path,
                        Trash.COLUMN_MEDIATYPE to it.mediatype)
            } catch (e: Exception) {
            }
        }

        // migrate tags
        val itemTagParser = rowParser { path: String, tag: String -> ItemTag(path, tag) }
        val tags = db.select(Tag.TABLE_NAME,
                Tag.COLUMN_ITEM_ID,
                Tag.COLUMN_TAG)
                .parseList(itemTagParser)

        db.dropTable(Tag.TABLE_NAME, true)
        createTagTable(db)

        tags.forEach {
            try {
                db.insertOrThrow(
                        Tag.TABLE_NAME,
                        Tag.COLUMN_ITEM_ID to it.path,
                        Tag.COLUMN_TAG to it.tag,
                        Tag.COLUMN_ITEM_TAG to it.path + "@" + it.tag
                )
            } catch (e: Exception) {
                if (e is SQLiteConstraintException) {
                }
            }
        }

    }
}

// Access property for Context
val Context.database: MyDatabaseOpenHelper
    get() = MyDatabaseOpenHelper.getInstance(applicationContext)
