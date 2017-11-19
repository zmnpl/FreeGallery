package com.labs.pbrother.freegallery.controller.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

/**
 * Created by simon on 23.08.17.
 */
class MyDatabaseOpenHelper(ctx: Context) : ManagedSQLiteOpenHelper(ctx, MyDatabaseOpenHelper.DB_NAME, null, MyDatabaseOpenHelper.DB_VERSION) {
    companion object {
        val DB_NAME = "FG.db"
        val DB_VERSION = 20

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
                DBContract.CollectionMetaEntry.COLUMN_NAME_COLLECTION_ID to TEXT + UNIQUE,
                DBContract.CollectionMetaEntry.COLUMN_NAME_LOVED to INTEGER,
                DBContract.CollectionMetaEntry.COLUMN_NAME_COLOR to INTEGER)

        db.createTable(DBContract.Tag.TABLE_NAME, true,
                DBContract.Tag._ID to INTEGER + UNIQUE,
                DBContract.Tag.COLUMN_NAME_ITEM_ID to TEXT,
                DBContract.Tag.COLUMN_NAME_TAG to TEXT)

        db.createTable(DBContract.Trash.TABLE_NAME, true,
                DBContract.Trash._ID to INTEGER + UNIQUE,
                DBContract.Trash.COLUMN_NAME_ITEM_PATH to TEXT,
                DBContract.Trash.COLUMN_NAME_MEDIATYPE to INTEGER)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Here you can upgrade tables, as usual
        db.dropTable(DBContract.CollectionMetaEntry.TABLE_NAME, true)
        db.dropTable(DBContract.Tag.TABLE_NAME, true)
        db.dropTable(DBContract.Trash.TABLE_NAME, true)

        onCreate(db)
    }
}

// Access property for Context
val Context.database: MyDatabaseOpenHelper
    get() = MyDatabaseOpenHelper.getInstance(applicationContext)
