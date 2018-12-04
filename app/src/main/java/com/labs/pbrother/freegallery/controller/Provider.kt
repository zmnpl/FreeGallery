package com.labs.pbrother.freegallery.controller

import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import android.util.SparseArray
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.app
import com.labs.pbrother.freegallery.controller.Cache.drawerCache
import com.labs.pbrother.freegallery.controller.Cache.itemCache
import com.labs.pbrother.freegallery.controller.Cache.overviewCache
import com.labs.pbrother.freegallery.controller.Cache.tagCache
import com.labs.pbrother.freegallery.controller.db.MyDb
import com.labs.pbrother.freegallery.extension.getRealPathFromURI
import com.labs.pbrother.freegallery.prefs
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap


/**
 * Created by simon on 22.02.17.
 */

class Provider : MetaUpdatorizer {

    private val resolver: MediaResolver = MediaResolver(app)
    private val deletions = SparseArray<ArrayList<TrashLog>>()

    val timeline: CollectionItem
        get() = resolver.timeline

    val trash: CollectionItem
        get() = resolver.trash

    val overviewItems: ArrayList<CollectionItem>
        get() {
            val collectionItems = LinkedHashMap<String, CollectionItem>()
            val timeline = timeline
            collectionItems.put(timeline.id, timeline)

            resolver.overviewCollections.forEach {
                collectionItems.put(it.id, it)
            }

            val trash = trash
            if (trash.count > 0) collectionItems.put(trash.id, trash)

            overviewCache.putAll(collectionItems)
            return ArrayList(collectionItems.values)
        }

    val drawerItems: ArrayList<CollectionItem>
        get() {
            val collectionItems = LinkedHashMap<String, CollectionItem>()
            val timeline = timeline
            collectionItems.put(app.getString(R.string.timelineName), timeline)

            resolver.tagCollections.forEach {
                tagCache.add(it.id)
                collectionItems.put(it.id, it)
            }

            val trash = trash
            if (trash.count > 0) collectionItems.put(trash.id, trash)

            drawerCache.putAll(collectionItems)
            return ArrayList(collectionItems.values)
        }

    fun tags(): List<String> = tagCache.toList()

    fun itemsFor(ci: CollectionItem, cached: Boolean = false): ArrayList<Item> {
        if (cached) {
            return itemCache[ci.id] ?: itemsFor(ci, false)
        }
        val items = resolver.itemsForCollection(ci)
        val itemsList = ArrayList(items)
        itemCache.put(ci.id, itemsList)
        return itemsList
    }

    fun collectionItem(collectionId: String): CollectionItem {
        if (overviewCache.size == 0) overviewItems
        if (drawerCache.size == 0) drawerItems
        return overviewCache[collectionId] ?: drawerCache[collectionId] ?: CollectionItem()
    }

    fun itemForUri(uri: Uri): Item {
        var path = app.getRealPathFromURI(uri)
        if ("" != path && null != path) {
            return resolver.makeSingleItemFromPath(path)
        }
        val result = Item()
        result.path = uri.toString()
        return result
    }

    // TODO - never finished this one
    fun collectionItemForImageUri(imageUri: Uri): CollectionItem {
        var cursor: Cursor? = null
        try {
            cursor = app.contentResolver.query(imageUri, IMAGE_PROJECTION, null, null, null)
            if (cursor!!.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val mPath = cursor.getString(index)
            }
        } catch (e: Exception) {
        } finally {
            cursor?.close()
        }
        return CollectionItem(id = DUMMY_ID)
    }

    fun deleteTag(tag: String): Boolean {
        if (tag == app.getString(R.string.timelineName) || tag == app.getString(R.string.trashName)) return false

        if (MyDb(app).deleteTag(tag) > 0) {
            drawerCache.remove(tag)
            return true
        }

        return false
    }

    fun trashItems(items: List<Item>): Int {
        val id = Random().nextInt(999999)
        deletions.put(id, sendToTrash(items))
        return id
    }

    fun trashForSDItems() {
//        // sdcard
//        items.forEach() {
//            //val uri = getImageContentUri(app, File(it.path))
//            val uri = Uri.parse(findSDUri(it.path))
//            DocumentsContract.deleteDocument(app.getContentResolver(), uri)
//            print(uri.toString())
//        }
    }

    fun findSDUri(path: String): String {
        //First we get `DocumentFile` from the `TreeUri` which in our case is `sdCardUri`.
        var documentFile: androidx.documentfile.provider.DocumentFile? = androidx.documentfile.provider.DocumentFile.fromTreeUri(app, Uri.parse(prefs.sdCardUri))

        val parts = path.split("/")

        for (i in 3 until parts.size) {
            if (documentFile != null) {
                documentFile = documentFile.findFile(parts[i])
            }
        }
        return documentFile?.uri.toString()

        /*if (documentFile == null) {

            // File not found on tree search
            // User selected a wrong directory as the sd-card
            // Here must inform user about how to get the correct sd-card
            // and invoke file chooser dialog again.

        } else {

            // File found on sd-card and it is a correct sd-card directory
            // save this path as a root for sd-card on your database(SQLite, XML, txt,...)

            // Now do whatever you like to do with documentFile.
            // Here I do deletion to provide an example.


            if (documentFile.delete()) {// if delete file succeed
                // Remove information related to your media from ContentResolver,
                // which documentFile.delete() didn't do the trick for me.
                // Must do it otherwise you will end up with showing an empty
                // ImageView if you are getting your URLs from MediaStore.
                //
                //val mediaContentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, longMediaId)
                //app.contentResolver.delete(mediaContentUri, null, null)
            }


        }
        return documentFile.toString()*/
    }

    fun undoTrashing(id: Int): Boolean {
        val trashed = deletions[id]
        if (null != trashed) {
            trashed.forEach {
                val trashFile = File(it.trashPath)
                val restoreFile = File(it.originalPath)

                if (trashFile.exists() && !restoreFile.exists()) {
                    trashFile.renameTo(restoreFile)
                }
            }

            val foo = trashed.map { it.originalPath }
            val work = foo.toTypedArray()
            MediaScannerConnection.scanFile(app, work, null, null)
            val db = MyDb(app)
            db.deleteTrashEntries(trashed.map { it.trashPath })
        }

        return true
    }

    fun sendToTrash(items: List<Item>): ArrayList<TrashLog> {
        // log: original path , trash path
        val log = ArrayList<TrashLog>()
        val db = MyDb(app)

        items.forEach {
            val currentFile = File(it.path)

            val parent = currentFile.parent
            val trashFile = File(parent + "/." + currentFile.name)

            //val trashFile = File(parent + "/trash/" + currentFile.name)
            //File(parent + "/" + "trash").mkdirs()
            //File(parent + "/" + "trash/.nomedia").createNewFile()

            if (currentFile.exists()) {
                val oldPath = currentFile.path
                currentFile.renameTo(trashFile)
                db.insertUpdateTrashedItem(trashFile.path, if ("" != it.path) it.type else -1)
            }
            log.add(TrashLog(originalPath = currentFile.path, trashPath = trashFile.path))
            val uri = FileProvider.getUriForFile(app, app.packageName + ".provider", currentFile)
            val d = app.contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", arrayOf<String>(currentFile.path))

            // TODO - update cache (?)
        }

        // scan old paths
        val work = log.map { it.originalPath }.toTypedArray()
        MediaScannerConnection.scanFile(app, work, null, null)

        return log
    }

    fun emptyTrash() {
        val db = MyDb(app)
        db.emtpyTrash()

        val items = itemCache[app.getString(R.string.trashName)]
        val scanThese = arrayOfNulls<String>(items?.size ?: 0)
        var i = 0
        items?.forEach {
            val f = File(it.path)
            f.delete()
            scanThese[i] = f.path
            i++
        }

        drawerCache.remove(app.getString(R.string.trashName))
        overviewCache.remove(app.getString(R.string.trashName))
        MediaScannerConnection.scanFile(app, scanThese, null, null)
    }

    fun restore(items: List<Item>) {
        val db = MyDb(app)
        val untrash = ArrayList<String>()
        val scan = ArrayList<String>()

        items.forEach {
            val trashFile = File(it.path)
            val restoredFile = File(trashFile.parent + "/" + trashFile.name.removePrefix("."))
            //val restoredFile = File(trashFile.parent.removeSuffix("trash/") + trashFile.name)

            if (trashFile.exists() && !restoredFile.exists()) {
                untrash.add(trashFile.path)
                scan.add(restoredFile.path)
                trashFile.renameTo(restoredFile)
            }
        }
        db.deleteTrashEntries(untrash)

        val work = scan.toTypedArray()
        MediaScannerConnection.scanFile(app, work, null, null)
    }

    // MetaUpdate Methods

    override fun loveCollection(collection: CollectionItem, loved: Boolean) {
        val db = MyDb(app)
        collection.love(loved)
        db.insertUpdateCollectionMeta(collection.id, loved, collection.color)
    }

    override fun colorizeCollection(collection: CollectionItem, c: Int?): Int {
        var color = c
        if (null == color) color = prefs.defaultCollectionColor
        val db = MyDb(app)
        collection.colorize(color)
        overviewCache[collection.id]?.colorize(color)
        drawerCache[collection.id]?.colorize(color)
        db.insertUpdateCollectionMeta(collection.id, collection.isLoved, color)
        return color
    }

    override fun uncolorCollection(collection: CollectionItem) = colorizeCollection(collection, null)

    fun tagItems(items: List<Item>, tag: String) {
        tagCache.add(tag)
        val db = MyDb(app)
        items.forEach { tagIt(it, tag, db) }
    }

    override fun tagItem(item: Item, tag: String) {
        tagCache.add(tag)
        val db = MyDb(app)
        tagIt(item, tag, db)
    }

    private fun tagIt(item: Item, tag: String, db: MyDb) {
        db.insertTag(item.id, tag)
        item.addTag(tag)
    }

    fun copyTags(fromID: String, toID: String) {
        MyDb(app).copyTags(fromID, toID)
    }

    override fun untagItem(item: Item, tag: String) {
        val db = MyDb(app)
        item.untag(tag)
        db.deleteTagForItem(item.id, tag)
    }
}
