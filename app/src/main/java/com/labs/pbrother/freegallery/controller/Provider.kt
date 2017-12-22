package com.labs.pbrother.freegallery.controller

import android.app.Application
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.util.SparseArray
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.Cache.drawerCache
import com.labs.pbrother.freegallery.controller.Cache.itemCache
import com.labs.pbrother.freegallery.controller.Cache.overviewCache
import com.labs.pbrother.freegallery.controller.Cache.tagCache
import com.labs.pbrother.freegallery.controller.db.MyDb
import com.labs.pbrother.freegallery.settings.SettingsHelper
import java.io.File
import java.util.*


/**
 * Created by simon on 22.02.17.
 */

class Provider(var applicationContext: Application) : MetaUpdatorizer {

    private var resolver: MediaResolver = MediaResolver(applicationContext)
    private val deletions = SparseArray<ArrayList<TrashLog>>()

    // Data Access for bound Activities
    val timeline: CollectionItem
        get() = resolver.timeline

    val trash: CollectionItem
        get() = resolver.trash

    val overviewItems: ArrayList<CollectionItem>
        get() {
            val timeline = timeline
            overviewCache.put(timeline.id, timeline)

            resolver.overviewCollections.forEach {
                overviewCache.put(it.id, it)
            }

            val trash = trash
            if (trash.count > 0) overviewCache.put(trash.id, trash)

            return ArrayList(overviewCache.values)
        }

    val drawerItems: ArrayList<CollectionItem>
        get() {
            val items = LinkedHashMap<String, CollectionItem>()
            val timeline = timeline
            items.put(applicationContext.getString(R.string.timelineName), timeline)

            resolver.tagCollections.forEach {
                tagCache.add(it.id)
                items.put(it.id, it)
            }

            val trash = trash
            if (trash.count > 0) items.put(trash.id, trash)
            drawerCache.putAll(items)

            return ArrayList(items.values)
        }

    fun cachedItemsFor(ci: CollectionItem, sortOrder: Int): ArrayList<Item> = itemCache[ci.id] ?: itemsFor(ci, sortOrder)
    fun itemsFor(ci: CollectionItem, sortOrder: Int): ArrayList<Item> {
        val items = resolver.itemsForCollection(ci, sortOrder)
        val itemsList = ArrayList(items)
        itemCache.put(ci.id, itemsList)
        return itemsList
    }

    fun collectionItem(collectionId: String): CollectionItem {
        if (overviewCache.size == 0) overviewItems
        if (drawerCache.size == 0) drawerItems
        return overviewCache[collectionId] ?: drawerCache[collectionId] ?: CollectionItem()
    }

    fun collectionItemForImageUri(imageUri: Uri): CollectionItem {
        var cursor: Cursor? = null
        try {
            cursor = applicationContext.contentResolver.query(imageUri, IMAGE_PROJECTION, null, null, null)
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

    fun tags(): List<String> = tagCache.toList()

    fun deleteTag(tag: String): Boolean {
        if (tag == applicationContext.getString(R.string.timelineName) || tag == applicationContext.getString(R.string.trashName)) {
            return false
        }

        val db = MyDb(applicationContext)
        if (db.deleteTag(tag) > 0) {
            drawerCache.remove(tag)
            return true
        }

        return false
    }

    fun trashItems(items: List<Item>): Int {
        val id = Random().nextInt(999999)
        deletions.put(id, delete(items))
        return id
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
            val work = foo.toTypedArray<String>()
            MediaScannerConnection.scanFile(applicationContext, work, null, null)
            val db = MyDb(applicationContext)
            db.deleteTrashEntries(trashed.map { it.trashPath })
        }

        return true
    }

    fun delete(items: List<Item>): ArrayList<TrashLog> {
        // log: original path , trash path
        val log = ArrayList<TrashLog>()
        val db = MyDb(applicationContext)

        items.forEach {
            val currentFile = File(it.path)
            val trashFile = File(currentFile.parent + "/." + currentFile.name)

            if (currentFile.exists()) {
                currentFile.renameTo(trashFile)
                log.add(TrashLog(originalPath = currentFile.path, trashPath = trashFile.path))
                db.insertUpdateTrashedItem(trashFile.path, if ("" != it.path) it.type else -1)
                // TODO - update cache (?)
            }
        }

        // scan old paths
        val foo = log.map { it.originalPath }
        val work = foo.toTypedArray<String>()
        MediaScannerConnection.scanFile(applicationContext, work, null, null)

        return log
    }

    fun emptyTrash() {
        val db = MyDb(applicationContext)
        db.emtpyTrash()

        val items = itemCache[applicationContext.getString(R.string.trashName)]
        val scanThese = arrayOfNulls<String>(items?.size ?: 0)
        var i = 0
        items?.forEach {
            val f = File(it.path)
            f.delete()
            scanThese[i] = f.path
            i++
        }

        drawerCache.remove(applicationContext.getString(R.string.trashName))
        overviewCache.remove(applicationContext.getString(R.string.trashName))
        MediaScannerConnection.scanFile(applicationContext, scanThese, null, null)
    }

    fun restore(items: List<Item>) {
        val db = MyDb(applicationContext)
        val untrash = ArrayList<String>()
        val scan = ArrayList<String>()

        items.forEach {
            val trashFile = File(it.path)
            val restoredFile = File(trashFile.parent + "/" + trashFile.name.removePrefix("."))

            if (trashFile.exists() && !restoredFile.exists()) {
                untrash.add(trashFile.path)
                scan.add(restoredFile.path)
                trashFile.renameTo(restoredFile)
            }
        }
        db.deleteTrashEntries(untrash)

        val work = scan.toTypedArray<String>()
        MediaScannerConnection.scanFile(applicationContext, work, null, null)
    }

    // MetaUpdate Methods

    override fun loveCollection(collection: CollectionItem, loved: Boolean) {
        val db = MyDb(applicationContext)
        collection.love(loved)
        db.insertUpdateCollectionMeta(collection.id, loved, collection.color)
    }

    override fun colorizeCollection(collection: CollectionItem, c: Int?) {
        var color = c
        if (null == color) color = SettingsHelper(applicationContext).higlightColor
        val db = MyDb(applicationContext)
        collection.colorize(color)
        db.insertUpdateCollectionMeta(collection.id, collection.isLoved, color)
    }

    override fun tagItem(item: Item, tag: String) {
        val db = MyDb(applicationContext)
        db.insertTag(item.id, tag)
        item.addTag(tag)
        tagCache.add(tag)
    }

    override fun untagItem(item: Item, tag: String) {
        val db = MyDb(applicationContext)
        item.untag(tag)
        db.deleteTagForItem(item.id, tag)
    }
}
