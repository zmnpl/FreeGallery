package com.labs.pbrother.freegallery.controller

import android.app.Service
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.MediaStore
import android.util.SparseArray

import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.db.MyDb
import com.labs.pbrother.freegallery.settings.SettingsHelper

import java.io.File
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Random
import kotlin.collections.HashSet


/**
 * Created by simon on 22.02.17.
 */
class MyService : Service(), MetaUpdatorizer {

    lateinit internal var settings: SettingsHelper
    lateinit private var resolver: MediaResolver
    private val deletions = SparseArray<ArrayList<TrashLog>>()
    private var initialized = false
    private val overviewCache: LinkedHashMap<String, CollectionItem> = LinkedHashMap()
    private val drawerCache: LinkedHashMap<String, CollectionItem> = LinkedHashMap()
    private val itemCache: LinkedHashMap<String, ArrayList<Item>> = LinkedHashMap()
    private val tagCache: HashSet<String> = HashSet()

    // Binder for client connection

    // Binder given to clients
    private val binder = LocalBinder()

    // Class used for the client Binder.  Because we know this service always
    // runs in the same process as its clients, we don't need to deal with IPC.
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: MyService
            get() = this@MyService
    }

    //Return binder for access to public methods of this service
    override fun onBind(intent: Intent): IBinder? {
        if (!initialized) {
            settings = SettingsHelper(applicationContext)
            resolver = MediaResolver(this)
            initialized = true
        }
        return binder
    }

    // Lifecycle

    override fun onDestroy() {
        super.onDestroy()
        initialized = false
    }

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
            items.put(getString(R.string.timelineName), timeline)

            resolver.tagCollections.forEach {
                tagCache.add(it.id)
                items.put(it.id, it)
            }

            val trash = trash
            if (trash.count > 0) items.put(trash.id, trash)
            drawerCache.putAll(items)

            return ArrayList(items.values)
        }

    fun itemsForCollection(ci: CollectionItem, sortOrder: Int): ArrayList<Item> {
        val items = resolver.itemsForCollection(ci, sortOrder)
        val itemsList = ArrayList(items)
        itemCache.put(ci.id, itemsList)
        return itemsList
    }

    fun collectionItemForImageUri(imageUri: Uri): CollectionItem {
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(imageUri, IMAGE_PROJECTION, null, null, null)
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

    fun cachedCollectionItem(collectionId: String): CollectionItem {
        if (overviewCache.containsKey(collectionId)) {
            return overviewCache[collectionId] ?: CollectionItem()
        } else if (drawerCache.containsKey(collectionId)) {
            return drawerCache[collectionId] ?: CollectionItem()
        }
        return CollectionItem()
    }

    fun cachedItemsFor(collectionId: String): ArrayList<Item> = itemCache[collectionId] ?: ArrayList<Item>()

    fun tags(): List<String> = tagCache.toList()

    fun deleteTag(tag: String): Boolean {
        if (tag == getString(R.string.timelineName) || tag == getString(R.string.trashName)) {
            return false
        }

        val db = MyDb(this)
        if (db.deleteTag(tag) > 0) {
            drawerCache.remove(tag)
            return true
        }

        return false
    }

    fun trashItems(items: ArrayList<Item>): Int {
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
            MediaScannerConnection.scanFile(this@MyService, work, null, null)
            val db = MyDb(applicationContext)
            db.deleteTrashEntries(trashed.map { it.trashPath })
        }

        return true
    }

    fun delete(items: ArrayList<Item>): ArrayList<TrashLog> {
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
        MediaScannerConnection.scanFile(this@MyService, work, null, null)

        return log
    }

    fun emptyTrash() {
        val db = MyDb(this@MyService)
        db.emtpyTrash()

        val items = itemCache[getString(R.string.trashName)]
        val scanThese = arrayOfNulls<String>(items?.size ?: 0)
        var i = 0
        items?.forEach {
            val f = File(it.path)
            f.delete()
            scanThese[i] = f.path
            i++
        }

        MediaScannerConnection.scanFile(this@MyService, scanThese, null, null)
    }

    fun restore(items: List<Item>) {
        val db = MyDb(this@MyService)
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
        MediaScannerConnection.scanFile(this@MyService, work, null, null)
    }

    // MetaUpdate Methods

    override fun loveCollection(collection: CollectionItem, loved: Boolean) {
        val db = MyDb(this)
        collection.love(loved)
        db.insertUpdateCollectionMeta(collection.id, loved, collection.color())
    }

    override fun colorizeCollection(collection: CollectionItem, colorid: Int?) {
        val db = MyDb(this)
        collection.colorize(colorid!!)
        db.insertUpdateCollectionMeta(collection.id, collection.isLoved, colorid)
    }

    override fun tagItem(item: Item, tag: String) {
        val db = MyDb(this)
        db.insertTag(item.id(), tag)
        item.addTag(tag)
        tagCache.add(tag)
    }

    override fun untagItem(item: Item, tag: String) {
        val db = MyDb(this)
        item.untag(tag)
        db.deleteTagForItem(item.id(), tag)
    }
}
