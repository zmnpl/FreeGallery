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
    private val dbHelperNew = context.database

    // collection meta
    private val collectionMetaProjection = arrayOf(
            CollectionMetaEntry.COLUMN_NAME_COLLECTION_ID,
            CollectionMetaEntry.COLUMN_NAME_LOVED,
            CollectionMetaEntry.COLUMN_NAME_COLOR)

    private val collectionMetaParser = rowParser { collectionId: String, loved: Int, color: Int ->
        CollectionMeta(collectionId, 1 == loved, color)
    }

    // tags
    private val itemTagProjection = arrayOf(
            Tag.COLUMN_NAME_ITEM_ID,
            Tag.COLUMN_NAME_TAG
    )

    private val itemTagParser = rowParser { path: String, tag: String -> ItemTag(path, tag) }

    // trash
    private val trashProjection = arrayOf(
            Trash.COLUMN_NAME_ITEM_PATH,
            Trash.COLUMN_NAME_MEDIATYPE
    )

    private val trashParser = rowParser { path: String, type: Int -> TrashItem(path, type) }


    // initializing operations; operating on given data objects

    // Loads tag for and into given image
    fun itemTags(): HashMap<String, ItemTag> {
        val itemTags = HashMap<String, ItemTag>()

        dbHelperNew.use {
            select(Tag.TABLE_NAME,
                    *itemTagProjection)
                    .parseList(itemTagParser)
                    .forEach {
                        itemTags.put(it.path, it)
                    }
        }

        return itemTags
    }

    fun collectionMeta(): HashMap<String, CollectionMeta> {
        val meta = HashMap<String, CollectionMeta>()

        dbHelperNew.use {
            select(CollectionMetaEntry.TABLE_NAME,
                    *collectionMetaProjection)
                    .parseList(collectionMetaParser)
                    .forEach {
                        meta.put(it.id, it)
                    }
        }

        return meta
    }

    fun collectionMetaFor(id: String): CollectionMeta? {
        return dbHelperNew.readableDatabase.
                select(CollectionMetaEntry.TABLE_NAME,
                        *collectionMetaProjection)
                .whereArgs("(${CollectionMetaEntry.COLUMN_NAME_COLLECTION_ID} = {id})",
                        "id" to id)
                .parseOpt(collectionMetaParser)
    }

    // Tag operations

    /**
     * get all tags from db
     */
    fun allTags(): List<String> {
        return dbHelperNew.readableDatabase
                .select(Tag.TABLE_NAME,
                        Tag.COLUMN_NAME_TAG)
                .distinct()
                .orderBy(Tag.COLUMN_NAME_TAG, SqlOrderDirection.ASC)
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
        return dbHelperNew.readableDatabase
                .select(Tag.TABLE_NAME,
                        Tag.COLUMN_NAME_TAG)
                .whereArgs("(${Tag.COLUMN_NAME_TAG} = {tag})",
                        "tag" to tag)
                .parseList(StringParser)
                .count()
    }

    /**
     * get list of paths for tag
     * @param tag
     */
    fun getPathsForTag(tag: String): List<String> {
        return dbHelperNew.readableDatabase
                .select(Tag.TABLE_NAME,
                        Tag.COLUMN_NAME_ITEM_ID)
                .whereArgs("(${Tag.COLUMN_NAME_TAG} = {tag})",
                        "tag" to tag)
                .parseList(StringParser)
    }

    /**
     * get thumbnail path for tag

     * @param tag
     */
    fun getThumbForTag(tag: String): String {
        return dbHelperNew.readableDatabase
                .select(Tag.TABLE_NAME,
                        Tag.COLUMN_NAME_TAG)
                .whereArgs("(${Tag.COLUMN_NAME_TAG} = {tag})",
                        "tag" to tag)
                .orderBy(Tag._ID, SqlOrderDirection.DESC)
                .limit(1)
                .parseOpt(StringParser) ?: ""
    }

    /**
     * deleteTag deletes tag entirely from db

     * @param tag
     */
    fun deleteTag(tag: String): Int {
        return dbHelperNew.writableDatabase
                .delete(Tag.TABLE_NAME,
                        "(${Tag.COLUMN_NAME_TAG} = {tag})",
                        "tag" to tag)
    }

    /**
     * deleteTagsForItem removes all tags for the given item
     * @param id
     */
    fun deleteAllTagsForItem(id: String): Int {
        return dbHelperNew.writableDatabase
                .delete(Tag.TABLE_NAME,
                        "(${Tag.COLUMN_NAME_ITEM_ID} = {itempath})",
                        "itempath" to id)
    }

    /**
     * deleteTagForItem removes deletes given tag for given item
     * @param id
     */
    fun deleteTagForItem(id: String, tag: String): Int {
        return dbHelperNew.writableDatabase
                .delete(Tag.TABLE_NAME,
                        "(${Tag.COLUMN_NAME_ITEM_ID} = {itempath}) and ${Tag.COLUMN_NAME_TAG} = {tag}",
                        "itempath" to id,
                        "tag" to tag)
    }

    /**
     * inserts tag for item
     * @param itemId
     * @param tag
     */
    fun insertTag(itemId: String, tag: String): Long {
        // TODO - only do, if not already existing...
        return dbHelperNew.writableDatabase
                .insert(
                        Tag.TABLE_NAME,
                        Tag.COLUMN_NAME_TAG to tag,
                        Tag.COLUMN_NAME_ITEM_ID to itemId)
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
            dbHelperNew.writableDatabase.insertOrThrow(
                    CollectionMetaEntry.TABLE_NAME,
                    CollectionMetaEntry.COLUMN_NAME_COLLECTION_ID to identifier,
                    CollectionMetaEntry.COLUMN_NAME_LOVED to if (loved) 1 else 0,
                    CollectionMetaEntry.COLUMN_NAME_COLOR to color
            )
        } catch (e: Exception) {
            if (e is SQLiteConstraintException) {
                dbHelperNew.writableDatabase
                        .update(CollectionMetaEntry.TABLE_NAME,
                                CollectionMetaEntry.COLUMN_NAME_LOVED to if (loved) 1 else 0,
                                CollectionMetaEntry.COLUMN_NAME_COLOR to color)
                        .whereArgs("${CollectionMetaEntry.COLUMN_NAME_COLLECTION_ID} = {id}",
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
        return dbHelperNew.writableDatabase.delete(
                CollectionMetaEntry.TABLE_NAME,
                "${CollectionMetaEntry.COLUMN_NAME_COLLECTION_ID} = {id}",
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
            dbHelperNew.writableDatabase.insertOrThrow(
                    Trash.TABLE_NAME,
                    Trash.COLUMN_NAME_ITEM_PATH to path,
                    Trash.COLUMN_NAME_MEDIATYPE to mediatype
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
        return dbHelperNew.writableDatabase.delete(
                Trash.TABLE_NAME,
                "${Trash.COLUMN_NAME_ITEM_PATH} = {path}",
                "path" to path
        )
    }

    fun deleteTrashEntries(paths: List<String>) {
        val db = dbHelperNew.writableDatabase

        paths.forEach {
            db.delete(
                    Trash.TABLE_NAME,
                    "${Trash.COLUMN_NAME_ITEM_PATH} = {path}",
                    "path" to it)
        }
    }

    /**
     * remove all items from trash
     */
    fun emtpyTrash(): Int {
        return dbHelperNew.writableDatabase
                .delete(Trash.TABLE_NAME, null, null)
    }

    fun allTrashItems(): List<TrashItem> {
        return dbHelperNew.readableDatabase
                .select(Trash.TABLE_NAME,
                        *trashProjection)
                .orderBy(Trash._ID, SqlOrderDirection.DESC)
                .parseList(trashParser)
    }

    val thumbForTrash: String
        get() {
            return dbHelperNew.readableDatabase
                    .select(Trash.TABLE_NAME,
                            *trashProjection)
                    .orderBy(Trash._ID, SqlOrderDirection.DESC)
                    .limit(1)
                    .parseOpt(trashParser)?.path ?: ""
        }

    fun countTrashItems(): Int {
        return dbHelperNew.readableDatabase
                .select(Trash.TABLE_NAME,
                        Trash._ID)
                .parseList(StringParser)
                .count()
    }

}
