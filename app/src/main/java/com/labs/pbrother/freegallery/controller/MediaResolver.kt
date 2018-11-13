package com.labs.pbrother.freegallery.controller

import android.content.Context
import android.provider.MediaStore
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.db.MyDb
import com.labs.pbrother.freegallery.extension.makeImageItem
import com.labs.pbrother.freegallery.extension.makeVideoItem
import com.labs.pbrother.freegallery.prefs
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by simon on 21.02.17.
 */

/**
 * Provide context for ContentResolver
 * @param context
 */
internal class MediaResolver(private val context: Context) {

    private val db: MyDb

    init {
        db = MyDb(context)
    }

    companion object {
        private val IMAGE_DEFAULT_SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC"
    }

    // collection items for purposes
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // result set
    // build up query
    val overviewCollections: TreeSet<CollectionItem>
        get() {
            val collectionItems = TreeSet<CollectionItem>()
            val PROJECTION = arrayOf("DISTINCT " + MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val SELECTION = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " NOT IN (?)"
            val SELECTION_ARGS = arrayOf("foobarfoo, barfoobar") // TODO - get from settigns
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
                            color = prefs.defaultCollectionColor
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
            val color = prefs.highlightColorAccent

            val timeline = CollectionItem(id = context.getString(R.string.timelineName),
                    type = TYPE_TAG,
                    thumb = latestItemPath,
                    count = countAllItems(),
                    color = prefs.highlightColorAccent)

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
                    color = prefs.defaultCollectionColor)

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
                        color = prefs.defaultCollectionColor)

                val meta = collectionMeta[itm.id]
                if (null != meta) itm.infuseMeta(meta)
                tagCollections.add(itm)
            }

            return tagCollections
        }

    // media items
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun itemsForCollection(ci: CollectionItem): TreeSet<Item> = when {
        ci.id == context.getString(R.string.trashName) -> trashItems(Item.SORT_ORDER)
        ci.id == context.getString(R.string.timelineName) -> allItems()
        ci.type == TYPE_FOLDER -> getItemsForBucketPath(ci.id, Item.SORT_ORDER)
        ci.type == TYPE_TAG -> tagItems(ci.id, Item.SORT_ORDER)
        else -> TreeSet<Item>()
    }

    private fun getItemsForBucketPath(path: String, sortOrder: Int): TreeSet<Item> {
        val items = orderedItemsTreeSet(sortOrder)
        val tags = db.itemTags()
        items.addAll(imagesForBucket(path, sortOrder, tags))
        items.addAll(vidsForBucket(path, sortOrder, tags))
        return items
    }

    private fun imagesForBucket(path: String, sortOrder: Int, tags: HashMap<String, HashSet<String>>): TreeSet<Item> {
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
                val itm = c.makeImageItem()
                if (tags.containsKey(itm.path)) itm.addAllTags(tags.getValue(itm.path))
                images.add(itm)
            } while (c.moveToNext())
        }
        c.close()
        return images
    }

    private fun vidsForBucket(path: String, sortOrder: Int, tags: HashMap<String, HashSet<String>>): TreeSet<Item> {
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
                val itm = c.makeVideoItem()
                if (tags.containsKey(itm.path)) itm.addAllTags(tags.getValue(itm.path))
                vids.add(itm)
            } while (c.moveToNext())
        }
        c.close()
        return vids
    }

    private fun tagItems(tag: String, sortOrder: Int): TreeSet<Item> {
        val items = orderedItemsTreeSet(sortOrder)
        val paths = db.getPathsForTag(tag)
        if (paths.count() < 1) return items // don't do the rest of the operations, when not necessary
        val tags = db.itemTags()

        if (paths.count() <= 200) {
            val x = paths.forEach {
                val itm = makeSingleItemFromPath(it)
                if (tags.containsKey(itm.path)) itm.addAllTags(tags.getValue(itm.path))
                items.add(itm)
            }
            return items
        }

        items.addAll(allItems().filter { tags.containsKey(it.path) })
        return items
    }

    private fun trashItems(sortOrder: Int): TreeSet<Item> {
        val items = orderedItemsTreeSet(sortOrder)
        for ((path, mediatype) in db.allTrashItems()) {
            when (mediatype) {
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {
                    items.add(Item(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, path, 0, 0, 0, 0, 0))
                }
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                    items.add(Item(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO, path, 0, 0, 0, 0, 0))
                }
                else -> {
                }
            }
        }

        return items
    }

    private fun allItems(): TreeSet<Item> {
        val items = orderedItemsTreeSet(Item.SORT_ORDER)
        val tags = db.itemTags()
        items.addAll(imagesForBucket("%", Item.SORT_ORDER, tags))
        items.addAll(vidsForBucket("%", Item.SORT_ORDER, tags))
        return items
    }

    fun makeSingleItemFromPath(path: String): Item {
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
        if (c != null && c.moveToFirst()) return c.makeImageItem()

        // Try Vid
        c = r.query(
                MediaStore.Files.getContentUri("external"),
                VID_PROJECTION,
                VID_SELECTION,
                SELECTION_ARGS,
                null
        )
        if (c != null && c.moveToFirst()) return c.makeVideoItem()
        c.close()

        // Return empty item
        return Item()
    }

    fun itemsFromPaths(paths: List<String>): List<Item> {

        val params = "?" + ", ?".repeat(if (paths.count() > 1) paths.count() - 1 else 0)
        val items = ArrayList<Item>()
        val IMAGE_SELECTION = MediaStore.Images.Media.DATA + "in ($params) "
        val VID_SELECTION = MediaStore.Video.Media.DATA + " in ($params) "
        val r = context.contentResolver

        // Try Image
        var c = r.query(
                MediaStore.Files.getContentUri("external"),
                IMAGE_PROJECTION,
                IMAGE_SELECTION,
                paths.toTypedArray(),
                null
        )
        if (c != null && c.moveToFirst()) {
            do {
                items.add(c.makeImageItem())
            } while (c.moveToNext())
        }

        // Try Vid
        c = r.query(
                MediaStore.Files.getContentUri("external"),
                VID_PROJECTION,
                VID_SELECTION,
                paths.toTypedArray(),
                null
        )
        if (c != null && c.moveToFirst()) {
            do {
                items.add(c.makeVideoItem())
            } while (c.moveToNext())

        }
        c.close()
        // Return empty item
        return items
    }


    // "helpers"
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun orderedItemsTreeSet(sortOrder: Int): TreeSet<Item> = when (sortOrder) {
        Item.SORT_ASC -> TreeSet(Collections.reverseOrder())
        else -> TreeSet<Item>()
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
