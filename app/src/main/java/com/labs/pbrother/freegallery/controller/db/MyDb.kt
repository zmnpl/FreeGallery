package com.labs.pbrother.freegallery.controller.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.labs.pbrother.freegallery.controller.CollectionMeta
import com.labs.pbrother.freegallery.controller.ItemTag
import com.labs.pbrother.freegallery.controller.TrashItem
import com.labs.pbrother.freegallery.controller.db.DBContract.*
import org.jetbrains.anko.db.*

/**
 * Created by simon on 21.02.17.
 */
class MyDb(val context: Context) {
    // Application data which can be queried
    private val dbHelper: MyDatabaseOpenHelper
        get() = context.database

    // collection meta
    private val collectionMetaProjection = arrayOf(
            CollectionMetaEntry.COLUMN_COLLECTION_ID,
            CollectionMetaEntry.COLUMN_LOVED,
            CollectionMetaEntry.COLUMN_COLOR)

    private val collectionMetaParser = rowParser { collectionId: String, loved: Int, color: Int ->
        CollectionMeta(collectionId, 1 == loved, color)
    }

    // tags
    private val itemTagProjection = arrayOf(
            //Tag._ID,
            Tag.COLUMN_ITEM_ID,
            Tag.COLUMN_TAG
    )

    private val itemTagParser = rowParser { path: String, tag: String -> ItemTag(path, tag) }

    // trash
    private val trashProjection = arrayOf(
            Trash.COLUMN_ITEM_PATH,
            Trash.COLUMN_MEDIATYPE
    )

    private val trashParser = rowParser { path: String, type: Int -> TrashItem(path, type) }


    // initializing operations; operating on given data objects

    // all tags together with path as key
    fun itemTags(): HashMap<String, HashSet<String>> {
        val itemTags = HashMap<String, HashSet<String>>()

        dbHelper.readableDatabase
                .select(Tag.TABLE_NAME,
                        *itemTagProjection)
                .parseList(itemTagParser)
                .forEach {
                    if (itemTags.containsKey(it.path)) {
                        itemTags.getValue(it.path).add(it.tag)
                    } else {
                        val foo = HashSet<String>()
                        foo.add(it.tag)
                        itemTags.put(it.path, foo)
                    }
                }

        return itemTags
    }

    fun collectionMeta(): HashMap<String, CollectionMeta> {
        val meta = HashMap<String, CollectionMeta>()

        dbHelper.readableDatabase
                .select(CollectionMetaEntry.TABLE_NAME,
                        *collectionMetaProjection)
                .parseList(collectionMetaParser)
                .forEach { meta.put(it.id, it) }

        return meta
    }

    fun collectionMetaFor(id: String): CollectionMeta? {
        return dbHelper.readableDatabase.select(CollectionMetaEntry.TABLE_NAME,
                *collectionMetaProjection)
                .whereArgs("(${CollectionMetaEntry.COLUMN_COLLECTION_ID} = {id})",
                        "id" to id)
                .parseOpt(collectionMetaParser)
    }

    // Tag operations

    /**
     * get all tags from db
     */
    fun allTags(): List<String> {
        return dbHelper.readableDatabase
                .select(Tag.TABLE_NAME,
                        Tag.COLUMN_TAG)
                .distinct()
                .orderBy(Tag.COLUMN_TAG, SqlOrderDirection.ASC)
                .parseList(StringParser).filter {
                    true
                }
        // TODO - filter out all tags for which items are trashed
    }

    /**
     * get item count for tag

     * @param tag
     */
    fun countItemsForTag(tag: String): Int {
        try {
            return dbHelper.readableDatabase
                    .select(Tag.TABLE_NAME,
                            Tag.COLUMN_TAG)
                    .whereArgs("(${Tag.COLUMN_TAG} = {tag})",
                            "tag" to tag)
                    .parseList(StringParser)
                    .count()
        } catch (e: Exception) {
            println("foo")
        }
        return 0
    }

    /**
     * get list of paths for tag
     * @param tag
     */
    fun getPathsForTag(tag: String): List<String> {
        return dbHelper.readableDatabase
                .select(Tag.TABLE_NAME,
                        Tag.COLUMN_ITEM_ID)
                .whereArgs("(${Tag.COLUMN_TAG} = {tag})",
                        "tag" to tag)
                .parseList(StringParser)
    }

    /**
     * get thumbnail path for tag

     * @param tag
     */
    fun getThumbForTag(tag: String): String {
        try {
            return dbHelper.readableDatabase
                    .select(Tag.TABLE_NAME,
                            Tag.COLUMN_TAG)
                    .whereArgs("(${Tag.COLUMN_TAG} = {tag})",
                            "tag" to tag)
                    .orderBy(Tag._ID, SqlOrderDirection.DESC)
                    .limit(1)
                    .parseOpt(StringParser) ?: ""
        } catch (e: Exception) {
        }
        return ""
    }

    /**
     * deleteTag deletes tag entirely from db

     * @param tag
     */
    fun deleteTag(tag: String): Int {
        return dbHelper.writableDatabase
                .delete(Tag.TABLE_NAME,
                        "(${Tag.COLUMN_TAG} = {tag})",
                        "tag" to tag)
    }

    /**
     * deleteTagsForItem removes all tags for the given item
     * @param id
     */
    fun deleteAllTagsForItem(id: String): Int {
        return dbHelper.writableDatabase
                .delete(Tag.TABLE_NAME,
                        "(${Tag.COLUMN_ITEM_ID} = {itempath})",
                        "itempath" to id)
    }

    /**
     * deleteTagForItem removes deletes given tag for given item
     * @param id
     */
    fun deleteTagForItem(id: String, tag: String): Int {
        return dbHelper.writableDatabase
                .delete(Tag.TABLE_NAME,
                        "(${Tag.COLUMN_ITEM_ID} = {itempath}) and ${Tag.COLUMN_TAG} = {tag}",
                        "itempath" to id,
                        "tag" to tag)
    }

    /**
     * inserts tag for item
     * @param itemId
     * @param tag
     */
    fun insertTag(itemId: String, tag: String): Long {
        try {
            return dbHelper.writableDatabase
                    .insertOrThrow(
                            Tag.TABLE_NAME,
                            Tag.COLUMN_ITEM_TAG to itemId + "@" + tag,
                            Tag.COLUMN_TAG to tag,
                            Tag.COLUMN_ITEM_ID to itemId)
        } catch (e: Exception) {
            // item was already tagged with this one
            if (e is SQLiteConstraintException) {
                return -1
            }
        }
        return 0
    }

    fun copyTags(fromID: String, toID: String) {
        val tags = itemTags().get(fromID)?.toList() ?: ArrayList()
        val db = dbHelper.writableDatabase

        for (tag in tags) {
            try {
                db.insertOrThrow(Tag.TABLE_NAME, Tag.COLUMN_ITEM_TAG to toID + "@" + tag, Tag.COLUMN_TAG to tag, Tag.COLUMN_ITEM_ID to toID)
            } catch (e: Exception) {
                // item was already tagged with this one
                if (e is SQLiteConstraintException) {
                }
            }
        }
    }

    // Collection Meta operations

    /**
     * insertUpdateCollectionMeta takes all meta informations for a collection and takes care for
     * persisting it into the application database
     * @param identifier
     * @param loved
     * @param color
     */
    fun insertUpdateCollectionMeta(identifier: String, loved: Boolean, color: Int) {
        try {
            dbHelper.writableDatabase.insertOrThrow(
                    CollectionMetaEntry.TABLE_NAME,
                    CollectionMetaEntry.COLUMN_COLLECTION_ID to identifier,
                    CollectionMetaEntry.COLUMN_LOVED to if (loved) 1 else 0,
                    CollectionMetaEntry.COLUMN_COLOR to color
            )
        } catch (e: Exception) {
            if (e is SQLiteConstraintException) {
                dbHelper.writableDatabase
                        .update(CollectionMetaEntry.TABLE_NAME,
                                CollectionMetaEntry.COLUMN_LOVED to if (loved) 1 else 0,
                                CollectionMetaEntry.COLUMN_COLOR to color)
                        .whereArgs("${CollectionMetaEntry.COLUMN_COLLECTION_ID} = {id}",
                                "id" to identifier)
                        .exec()
            }
        }
    }

    /**
     * deletes metadata for given collection id
     * @param id
     */
    fun deleteCollectionMeta(id: String): Int {
        return dbHelper.writableDatabase.delete(
                CollectionMetaEntry.TABLE_NAME,
                "${CollectionMetaEntry.COLUMN_COLLECTION_ID} = {id}",
                "id" to id
        )
    }

    // trash operations

    /**
     * insert item to trash
     * @param path
     */
    fun insertUpdateTrashedItem(path: String, mediatype: Int) {
        try {
            dbHelper.writableDatabase.insertOrThrow(
                    Trash.TABLE_NAME,
                    Trash.COLUMN_ITEM_PATH to path,
                    Trash.COLUMN_MEDIATYPE to mediatype
            )
        } catch (e: Exception) {
            if (e is SQLiteConstraintException) {
                // already trashed ... ^^
            }
        }
    }

    /**
     * remove item from trash
     * @param path
     */
    fun deleteTrashEntry(path: String): Int {
        return dbHelper.writableDatabase.delete(
                Trash.TABLE_NAME,
                "${Trash.COLUMN_ITEM_PATH} = {path}",
                "path" to path
        )
    }

    fun deleteTrashEntries(paths: List<String>) {
        val db = dbHelper.writableDatabase

        paths.forEach {
            db.delete(
                    Trash.TABLE_NAME,
                    "${Trash.COLUMN_ITEM_PATH} = {path}",
                    "path" to it)
        }
    }

    /**
     * remove all items from trash
     */
    fun emtpyTrash(): Int {
        return dbHelper.writableDatabase
                .delete(Trash.TABLE_NAME, null, null)
    }

    fun allTrashItems(): List<TrashItem> {
        return dbHelper.readableDatabase
                .select(Trash.TABLE_NAME,
                        *trashProjection)
                .orderBy(Trash._ID, SqlOrderDirection.DESC)
                .parseList(trashParser)
    }

    val thumbForTrash: String
        get() {
            return dbHelper.readableDatabase
                    .select(Trash.TABLE_NAME,
                            *trashProjection)
                    .orderBy(Trash._ID, SqlOrderDirection.DESC)
                    .limit(1)
                    .parseOpt(trashParser)?.path ?: ""
        }

    fun countTrashItems(): Int {
        return dbHelper.readableDatabase
                .select(Trash.TABLE_NAME,
                        Trash._ID)
                .parseList(StringParser)
                .count()
    }

}
