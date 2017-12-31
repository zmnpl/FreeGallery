package com.labs.pbrother.freegallery.controller.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.labs.pbrother.freegallery.controller.ItemTag
import org.jetbrains.anko.db.*

/**
 * Created by simon on 23.08.17.
 */
class MyDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, MyDatabaseOpenHelper.DB_NAME, null, MyDatabaseOpenHelper.DB_VERSION) {
    companion object {
        val DB_NAME = "FG.db"
        val DB_VERSION = 30

        private var instance: MyDatabaseOpenHelper? = null

        @Synchronized
        fun getInstance(ctx: Context): MyDatabaseOpenHelper {
            if (instance == null) {
                instance = MyDatabaseOpenHelper(ctx.applicationContext)
            }
            return instance as MyDatabaseOpenHelper
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Here you create tables

        db.createTable(DBContract.CollectionMetaEntry.TABLE_NAME, true,
                DBContract.CollectionMetaEntry._ID to INTEGER + UNIQUE,
                DBContract.CollectionMetaEntry.COLUMN_COLLECTION_ID to TEXT + UNIQUE + PRIMARY_KEY,
                DBContract.CollectionMetaEntry.COLUMN_LOVED to INTEGER,
                DBContract.CollectionMetaEntry.COLUMN_COLOR to INTEGER)

        /*
        // TODO - how to with Anko?
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DBContract.TagUnique.TABLE_NAME + "( "
                + DBContract.TagUnique.COLUMN_ITEM_ID + " TEXT, "
                + DBContract.TagUnique.COLUMN_TAG + " TEXT, "
                + "UNIQUE (" + DBContract.TagUnique.COLUMN_ITEM_ID + ", " + DBContract.TagUnique.COLUMN_TAG + " )"
                + " )")
        */

        db.createTable(DBContract.TagUnique.TABLE_NAME, true,
                DBContract.TagUnique._ID to INTEGER + UNIQUE,
                DBContract.TagUnique.COLUMN_ITEM_TAG to TEXT + UNIQUE + PRIMARY_KEY,
                DBContract.TagUnique.COLUMN_ITEM_ID to TEXT,
                DBContract.TagUnique.COLUMN_TAG to TEXT
                )

        db.createTable(DBContract.Trash.TABLE_NAME, true,
                DBContract.Trash._ID to INTEGER + UNIQUE,
                DBContract.Trash.COLUMN_ITEM_PATH to TEXT + UNIQUE + PRIMARY_KEY,
                DBContract.Trash.COLUMN_MEDIATYPE to INTEGER)

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

        db.dropTable(DBContract.CollectionMetaEntry.TABLE_NAME, true)
        db.dropTable(DBContract.Trash.TABLE_NAME, true)

        val itemTagParser = rowParser { path: String, tag: String -> ItemTag(path, tag) }
        db.select(DBContract.Tag.TABLE_NAME,
                DBContract.Tag.COLUMN_ITEM_ID,
                DBContract.Tag.COLUMN_TAG)
                .parseList(itemTagParser)
                .forEach {
                    try {
                        db.insertOrThrow(
                                DBContract.TagUnique.TABLE_NAME,
                                DBContract.TagUnique.COLUMN_ITEM_ID to it.path,
                                DBContract.TagUnique.COLUMN_TAG to it.tag,
                                DBContract.TagUnique.COLUMN_ITEM_TAG to it.path  + "@" + it.tag
                        )
                    } catch (e: Exception) {
                        print("bar")
                        if (e is SQLiteConstraintException) {
                            print("foo")
                        }
                    }

                }
        db.dropTable(DBContract.Tag.TABLE_NAME, true)
    }
}

// Access property for Context
val Context.database: MyDatabaseOpenHelper
    get() = MyDatabaseOpenHelper.getInstance(applicationContext)
