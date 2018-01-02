package com.labs.pbrother.freegallery.controller.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.labs.pbrother.freegallery.controller.CollectionMeta
import com.labs.pbrother.freegallery.controller.ItemTag
import com.labs.pbrother.freegallery.controller.TrashItem
import org.jetbrains.anko.db.*

/**
 * Created by simon on 23.08.17.
 */
class MyDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, MyDatabaseOpenHelper.DB_NAME, null, MyDatabaseOpenHelper.DB_VERSION) {
    companion object {
        val DB_NAME = "FG.db"
        val DB_VERSION = 31

        private var instance: MyDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): MyDatabaseOpenHelper {
            if (instance == null) {
                instance = MyDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance as MyDatabaseOpenHelper
        }
    }

    private fun createCollectionMetaTable(db: SQLiteDatabase) = db.createTable(DBContract.CollectionMetaEntry.TABLE_NAME, true,
            DBContract.CollectionMetaEntry._ID to INTEGER + UNIQUE,
            DBContract.CollectionMetaEntry.COLUMN_COLLECTION_ID to TEXT + UNIQUE + PRIMARY_KEY,
            DBContract.CollectionMetaEntry.COLUMN_LOVED to INTEGER,
            DBContract.CollectionMetaEntry.COLUMN_COLOR to INTEGER
    )

    private fun createTagTable(db: SQLiteDatabase) = db.createTable(DBContract.Tag.TABLE_NAME, true,
            DBContract.Tag._ID to INTEGER + UNIQUE,
            DBContract.Tag.COLUMN_ITEM_TAG to TEXT + UNIQUE + PRIMARY_KEY,
            DBContract.Tag.COLUMN_ITEM_ID to TEXT,
            DBContract.Tag.COLUMN_TAG to TEXT
    )

    private fun createTrashTable(db: SQLiteDatabase) = db.createTable(DBContract.Trash.TABLE_NAME, true,
            DBContract.Trash._ID to INTEGER + UNIQUE,
            DBContract.Trash.COLUMN_ITEM_PATH to TEXT + UNIQUE + PRIMARY_KEY,
            DBContract.Trash.COLUMN_MEDIATYPE to INTEGER
    )

    private fun createNotTrashedTaggedItemsView(db: SQLiteDatabase) = db.execSQL(
            "CREATE VIEW [IF NOT EXISTS] "
                    + DBContract.UntrashedTagged.VIEW_NAME
                    +"("
                    + DBContract.Tag.TABLE_NAME
                    + "."
                    + DBContract.Tag.COLUMN_ITEM_ID
                    + " AS " + DBContract.UntrashedTagged.COLUMN_ITEM_ID

                    + DBContract.Tag.TABLE_NAME
                    + "."
                    + DBContract.Tag.COLUMN_ITEM_TAG
                    + " AS " + DBContract.UntrashedTagged.COLUMN_TAG

                    + DBContract.Trash.TABLE_NAME
                    + "."
                    + DBContract.Trash.COLUMN_ITEM_PATH
                    + " AS " + DBContract.UntrashedTagged.COLUMN_TRASH_ITEM_ID

                    + ") AS "
                    +" select-statement" // TODO
    )

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables
        createCollectionMetaTable(db)
        createTagTable(db)
        createTrashTable(db)
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

        val collectionMetas = db.select(DBContract.CollectionMetaEntry.TABLE_NAME,
                DBContract.CollectionMetaEntry.COLUMN_COLLECTION_ID,
                DBContract.CollectionMetaEntry.COLUMN_LOVED,
                DBContract.CollectionMetaEntry.COLUMN_COLOR
        ).parseList(collectionMetaParser)

        db.dropTable(DBContract.CollectionMetaEntry.TABLE_NAME, true)
        createCollectionMetaTable(db)

        collectionMetas.forEach {
            try {
                db.insertOrThrow(DBContract.CollectionMetaEntry.TABLE_NAME,
                        DBContract.CollectionMetaEntry.COLUMN_COLLECTION_ID to it.id,
                        DBContract.CollectionMetaEntry.COLUMN_LOVED to it.loved,
                        DBContract.CollectionMetaEntry.COLUMN_COLOR to it.color)
            } catch (e: Exception) {
            }
        }

        // migrate trash
        val trashParser = rowParser { path: String, type: Int -> TrashItem(path, type) }
        val trashs = db.select(DBContract.Trash.TABLE_NAME,
                DBContract.Trash.COLUMN_ITEM_PATH,
                DBContract.Trash.COLUMN_MEDIATYPE).parseList(trashParser)

        db.dropTable(DBContract.Trash.TABLE_NAME, true)
        createTrashTable(db)

        trashs.forEach {
            try {
                db.insertOrThrow(DBContract.Trash.TABLE_NAME,
                        DBContract.Trash.COLUMN_ITEM_PATH to it.path,
                        DBContract.Trash.COLUMN_MEDIATYPE to it.mediatype)
            } catch (e: Exception) {
            }
        }

        // migrate tags
        val itemTagParser = rowParser { path: String, tag: String -> ItemTag(path, tag) }
        val tags = db.select(DBContract.Tag.TABLE_NAME,
                DBContract.Tag.COLUMN_ITEM_ID,
                DBContract.Tag.COLUMN_TAG)
                .parseList(itemTagParser)

        db.dropTable(DBContract.Tag.TABLE_NAME, true)
        createTagTable(db)

        tags.forEach {
            try {
                db.insertOrThrow(
                        DBContract.Tag.TABLE_NAME,
                        DBContract.Tag.COLUMN_ITEM_ID to it.path,
                        DBContract.Tag.COLUMN_TAG to it.tag,
                        DBContract.Tag.COLUMN_ITEM_TAG to it.path + "@" + it.tag
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
