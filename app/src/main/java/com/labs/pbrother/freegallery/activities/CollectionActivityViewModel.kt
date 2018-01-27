package com.labs.pbrother.freegallery.activities

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import android.support.v4.content.FileProvider
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.Item.Companion.SORT_DESC
import com.labs.pbrother.freegallery.controller.Provider
import java.io.File

/**
 * Created by simon on 21.11.17.
 */
class CollectionActivityViewModel(application: Application) : AndroidViewModel(application) {
    private var foo = Provider(getApplication())
    private var collectionID = ""
    private lateinit var collection: CollectionItem

    val collectionType
        get() = collectionItem.value?.type

    val collectionId
        get() = collectionItem.value?.id

    val tags
        get() = foo.tags()

    var drawerItems = MutableLiveData<ArrayList<CollectionItem>>()
    var collectionItem = MutableLiveData<CollectionItem>()
    var items = MutableLiveData<ArrayList<Item>>()
    var liveColor = MutableLiveData<Int>()

    fun refresh(collection: Boolean, drawer: Boolean, items: Boolean, collectionID: String, cached: Boolean = false) {
        this.collectionID = collectionID
        if (collection) refreshCollection(collectionID)
        if (drawer) refreshDrawerItems()
        if (items) refreshItems(cached)
    }

    private fun refreshCollection(collectionId: String) {
        collection = foo.collectionItem(collectionId)
        collectionItem.postValue(collection)
        liveColor.postValue(collection.color)
    }

    fun refreshDrawerItems() {
        drawerItems.postValue(foo.drawerItems)
    }

    private fun refreshItems(cached: Boolean = false) {
        if (cached) {
            items.postValue(foo.cachedItemsFor(collection, Item.SORT_ORDER))
        } else {
            items.postValue(foo.itemsFor(collection))
        }
    }


    fun selectedItems(selection: List<Int>): List<Item> {
        val result = ArrayList<Item>()
        selection.forEach() {
            val ci = items.value?.get(it)
            if (null != ci) result.add(ci)
        }
        return result
    }

    fun setSortAsc() {
        if (Item.SORT_ORDER == Item.SORT_ASC) return
        Item.SORT_ORDER = Item.SORT_ASC
        refresh(false, false, true, collectionID)
    }

    fun setSortDesc() {
        if (Item.SORT_ORDER == Item.SORT_DESC) return
        Item.SORT_ORDER = SORT_DESC
        refresh(false, false, true, collectionID)
    }

    fun colorize(collection: CollectionItem, color: Int) {
        foo.colorizeCollection(collection, color)
    }

    fun removeColor() {
        foo.colorizeCollection(collection, null)
        liveColor.postValue(collection.color)
    }

    fun deleteTag() = foo.deleteTag(collectionItem.value?.id ?: "")

    fun emptyTrash() = foo.emptyTrash()

    fun restoreItems(items: List<Item>) = foo.restore(items)

    fun trashItems(items: List<Item>): Int = foo.trashItems(items)

    fun undoTrashing(id: Int) = foo.undoTrashing(id)

    fun untag(items: List<Item>) = items.forEach { foo.untagItem(it, collectionID) }

    fun tagItems(items: List<Item>, tag: String) {
        items.forEach { foo.tagItem(it, tag) }
    }

    fun urisToShare(items: List<Item>): ArrayList<Uri> {
        val uris = ArrayList<Uri>()
        items.forEach {
            uris.add(FileProvider.getUriForFile(getApplication(), getApplication<Application>().packageName + ".provider", File(it.path)))
        }
        return uris
    }

    fun colorizeCollection(color: Int) {
        val col = collectionItem.value
        if (null != col) {
            foo.colorizeCollection(col, color)
            liveColor.postValue(color)
        }
    }
}
