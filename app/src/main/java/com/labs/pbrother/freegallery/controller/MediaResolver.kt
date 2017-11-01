package com.labs.pbrother.freegallery.controller

import android.content.Context
import android.provider.MediaStore

import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.db.MyDb
import com.labs.pbrother.freegallery.settings.SettingsHelper

import java.io.File
import java.util.*

/**
 * Created by simon on 21.02.17.
 */

/**
 * Provide context for ContentResolver
 * @param context
 */
internal class MediaResolver(private val context: Context) {

    private val db: MyDb
    private var settings: SettingsHelper

    init {
        settings = SettingsHelper(context)
        db = MyDb(context)
    }

    companion object {
        private val IMAGE_DEFAULT_SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC"
    }

    // collection items for purposes
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun CollectionItem.infuseMeta(meta: CollectionMeta) {
        this.colorize(meta.color)
        this.love(meta.loved)
    }

    // result set
    // build up query
    val overviewCollections: TreeSet<CollectionItem>
        get() {
            val collectionItems = TreeSet<CollectionItem>()
            val PROJECTION = arrayOf("DISTINCT " + MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val SELECTION = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " NOT IN (?)"
            val SELECTION_ARGS = arrayOf("foobarfoo, barfoobar") // TODO - get from seetigns
            val SORT_ORDER = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC"

            val collectionMeta = db.collectionMeta()

            val r = context.contentResolver
            val c = r.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    SELECTION,
                    SELECTION_ARGS,
                    SORT_ORDER
            )

            if (c?.moveToFirst() == true) {
                do {
                    val thumb = File(thumbForBucketId(c.getString(0)))
                    val ci = CollectionItem(id = thumb.parent,
                            type = TYPE_FOLDER,
                            thumb = thumb.path,
                            count = countBucket(c.getString(0)),
                            color = settings.higlightColor
                    )
                    val meta = collectionMeta[ci.id]
                    if (null != meta) ci.infuseMeta(meta)
                    collectionItems.add(ci)
                } while (c.moveToNext())
            }
            c?.close()

            return collectionItems
        }

    val timeline: CollectionItem
        get() {
            val id = context.getString(R.string.timelineName)
            val thumb = latestItemPath
            val cnt = countAllItems()
            val color = settings.highlightColorAccent

            val timeline = CollectionItem(id = context.getString(R.string.timelineName),
                    type = TYPE_TAG,
                    thumb = latestItemPath,
                    count = countAllItems(),
                    color = settings.highlightColorAccent)

            val meta = db.collectionMetaFor(timeline.id)
            if (null != meta) timeline.infuseMeta(meta)

            return timeline
        }

    val trash: CollectionItem
        get() {
            val trash = CollectionItem(id = context.getString(R.string.trashName),
                    type = TYPE_TAG,
                    thumb = db.thumbForTrash,
                    count = db.countTrashItems(),
                    color = settings.higlightColor)

            val meta = db.collectionMetaFor(trash.id)
            if (null != meta) trash.infuseMeta(meta)

            return trash
        }

    val tagCollections: TreeSet<CollectionItem>
        get() {
            val tagCollections = TreeSet<CollectionItem>()
            val collectionMeta = db.collectionMeta()
            for (tag in db.allTags()) {
                val itm = CollectionItem(id = tag,
                        type = TYPE_TAG,
                        thumb = db.getThumbForTag(tag),
                        count = db.countItemsForTag(tag),
                        color = settings.higlightColor)

                val meta = collectionMeta[itm.id]
                if (null != meta) itm.infuseMeta(meta)
                tagCollections.add(itm)
            }

            return tagCollections
        }

    // media items
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun itemsForCollection(ci: CollectionItem, sortOrder: Int): TreeSet<Item> {
        return when {
            ci.id == context.getString(R.string.trashName) -> trashItems(sortOrder)
            ci.id == context.getString(R.string.timelineName) -> allItems(sortOrder)
            ci.type == TYPE_FOLDER -> getItemsForBucketPath(ci.id, sortOrder)
            ci.type == TYPE_TAG -> tagItems(ci.id, sortOrder)
            else -> TreeSet<Item>()
        }
    }

    private fun getItemsForBucketPath(path: String, sortOrder: Int): TreeSet<Item> {
        val items = orderedItemsTreeSet(sortOrder)
        items.addAll(imagesForBucket(path, sortOrder))
        items.addAll(vidsForBucket(path, sortOrder))
        return items
    }

    private fun imagesForBucket(path: String, sortOrder: Int): TreeSet<Item> {
        val images = orderedItemsTreeSet(sortOrder)
        val SELECTION = MediaStore.Images.Media.DATA + " LIKE (?)"
        val SELECTION_ARGS = arrayOf(path + "%")

        val c = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                IMAGE_PROJECTION,
                SELECTION,
                SELECTION_ARGS,
                IMAGE_DEFAULT_SORT_ORDER
        )
        if (c.moveToFirst()) {
            do {
                images.add(Item(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                        c.getString(PATH),
                        c.getLong(DATEADDED),
                        c.getLong(DATETAKEN),
                        c.getLong(SIZE),
                        c.getInt(WIDTH),
                        c.getInt(HEIGHT),
                        c.getLong(LAT).toDouble(),
                        c.getLong(LONG).toDouble())) // TODO - seems to be wrong; always 56 and 6 ... wtf?
            } while (c.moveToNext())
        }
        c.close()
        return images
    }

    private fun vidsForBucket(path: String, sortOrder: Int): TreeSet<Item> {
        val vids = orderedItemsTreeSet(sortOrder)
        val SELECTION = MediaStore.Video.Media.DATA + " LIKE (?)"
        val SELECTION_ARGS = arrayOf(path + "%")

        val c = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                IMAGE_PROJECTION,
                SELECTION,
                SELECTION_ARGS,
                IMAGE_DEFAULT_SORT_ORDER
        )
        if (c.moveToFirst()) {
            do {
                vids.add(Item(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                        c.getString(PATH),
                        c.getLong(DATEADDED),
                        c.getLong(DATETAKEN),
                        c.getLong(SIZE),
                        c.getInt(WIDTH),
                        c.getInt(HEIGHT),
                        c.getLong(LAT).toDouble(),
                        c.getLong(LONG).toDouble())) // TODO - seems to be wrong; always 56 and 6 ... wtf?
            } while (c.moveToNext())
        }
        c.close()
        return vids
    }

    private fun tagItems(tag: String, sortOrder: Int): TreeSet<Item> {
        val items = orderedItemsTreeSet(sortOrder)
        for (path in db.getPathsForTag(tag)) {
            val itm = makeSingleItemFromPath(path)
            items.add(itm)
        }
        return items
    }

    private fun trashItems(sortOrder: Int): TreeSet<Item> {
        val items = orderedItemsTreeSet(sortOrder)
        for ((path, mediatype) in db.allTrashItems()) {
            when (mediatype) {
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {
                    items.add(Item(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, path, 0, 0, 0, 0, 0, 0.0, 0.0))
                }
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                    items.add(Item(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, path, 0, 0, 0, 0, 0, 0.0, 0.0))
                }
                else -> {
                }
            }
        }

        return items
    }

    private fun allItems(sortOrder: Int): TreeSet<Item> {
        val items = orderedItemsTreeSet(sortOrder)
        items.addAll(imagesForBucket("%", sortOrder))
        items.addAll(vidsForBucket("%", sortOrder))
        return items
    }

    private fun makeSingleItemFromPath(path: String): Item {
        var item: Item

        val IMAGE_SELECTION = MediaStore.Images.Media.DATA + " = (?) "
        val VID_SELECTION = MediaStore.Video.Media.DATA + " = (?) "

        val SELECTION_ARGS = arrayOf(path)

        val r = context.contentResolver
        // Try Image
        var c = r.query(
                MediaStore.Files.getContentUri("external"),
                IMAGE_PROJECTION,
                IMAGE_SELECTION,
                SELECTION_ARGS,
                null
        )
        if (c != null && c.moveToFirst()) {
            return Item(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                    c.getString(PATH),
                    c.getLong(DATEADDED),
                    c.getLong(DATETAKEN),
                    c.getLong(SIZE),
                    c.getInt(WIDTH),
                    c.getInt(HEIGHT),
                    c.getLong(LAT).toDouble(),
                    c.getLong(LONG).toDouble())
        }

        // Try Vid
        c = r.query(
                MediaStore.Files.getContentUri("external"),
                VID_PROJECTION,
                VID_SELECTION,
                SELECTION_ARGS,
                null
        )
        if (c != null && c.moveToFirst()) {
            return Item(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                    c.getString(PATH),
                    c.getLong(DATEADDED),
                    c.getLong(DATETAKEN),
                    c.getLong(SIZE),
                    c.getInt(WIDTH),
                    c.getInt(HEIGHT),
                    c.getLong(LAT).toDouble(),
                    c.getLong(LONG).toDouble())
        }
        c.close()
        // Return empty item
        return Item()
    }


    // "helpers"
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun orderedItemsTreeSet(sortOrder: Int): TreeSet<Item> {
        return when (sortOrder) {
            SORT_ITEMS_ASC -> TreeSet(Collections.reverseOrder())
            else -> TreeSet<Item>()
        }
    }

    private fun thumbForBucketId(bucketid: String): String {
        val SELECT = arrayOf("DISTINCT " + MediaStore.Images.Media.DATA)
        val WHERE = MediaStore.Images.Media.BUCKET_ID + " = (?)"
        val WHERE_ARGS = arrayOf(bucketid)
        val ORDERBY = MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 1"

        val c = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                SELECT,
                WHERE,
                WHERE_ARGS,
                ORDERBY
        )
        if (c.moveToFirst()) {
            return c.getString(0)
        }
        c.close()

        return ""
    }

    private val latestItemPath: String
        get() {
            var thumb = ""
            val PROJECTION = arrayOf("DISTINCT " + MediaStore.Images.Media.DATA)
            val SELECTION = MediaStore.Images.Media.BUCKET_ID + " NOT IN (?)"
            val SELECTION_ARGS = arrayOf("foobar")
            val SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 1"
            val r = context.contentResolver
            val c = r.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    PROJECTION,
                    SELECTION,
                    SELECTION_ARGS,
                    SORT_ORDER
            )
            if (c != null && c.moveToFirst()) {
                thumb = c.getString(0)
            }
            c.close()
            return thumb
        }

    private fun countBucket(bucketid: String): Int {
        var count = 0
        val PROJECTION = arrayOf("COUNT(" + MediaStore.Images.Media.DATA + ")")
        val SELECTION = MediaStore.Images.Media.BUCKET_ID + " = (?)"
        val SELECTION_ARGS = arrayOf(bucketid)
        val r = context.contentResolver
        val c = r.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                SELECTION,
                SELECTION_ARGS, null
        )
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0)
        }
        c.close()
        return count
    }

    private fun countAllItems(): Int {
        val PROJECTION = arrayOf("DISTINCT " + MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA)
        val SELECTION = (MediaStore.Files.FileColumns.DATA + " NOT LIKE (?) "
                + "AND (" + MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + " = " + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ")")

        val SELECTION_ARGS = arrayOf("foobarfoobar%")
        val DEFAULT_SORT_ORDER = MediaStore.Files.FileColumns.DATE_ADDED + " DESC"

        val r = context.contentResolver
        val c = r.query(
                MediaStore.Files.getContentUri("external"),
                PROJECTION,
                SELECTION,
                SELECTION_ARGS,
                DEFAULT_SORT_ORDER
        )
        val result = c.count
        c.close()
        return result
    }
}
