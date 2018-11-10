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
    private var provider = Provider(getApplication())
    private var collectionID = ""
    private lateinit var collection: CollectionItem
    var drawerItems = MutableLiveData<ArrayList<CollectionItem>>()
    var collectionItem = MutableLiveData<CollectionItem>()
    var items = MutableLiveData<ArrayList<Item>>()
    var liveColor = MutableLiveData<Int>()


    fun refresh(collection: Boolean, drawer: Boolean, items: Boolean, collectionID: String, cached: Boolean = false) {
        this.collectionID = collectionID
        if (collection) refreshCollection(collectionID)
        if (drawer) refreshDrawerItems()
        if (items) refreshItems(collection)
    }


    val collectionType
        get() = collectionItem.value?.type

    val collectionId
        get() = collectionItem.value?.id

    val tags
        get() = provider.tags()


    private fun refreshCollection(collectionId: String) {
        collection = provider.collectionItem(collectionId)
        collectionItem.postValue(collection)
        liveColor.postValue(collection.color)
    }

    fun refreshDrawerItems() {
        drawerItems.postValue(provider.drawerItems)
    }

    private fun refreshItems(cached: Boolean = false) {
        items.postValue(provider.itemsFor(collection))
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

    fun removeColor() {
        provider.colorizeCollection(collection, null)
        liveColor.postValue(collection.color)
    }

    fun deleteTag() = provider.deleteTag(collectionItem.value?.id ?: "")

    fun emptyTrash() = provider.emptyTrash()

    fun restoreItems(items: List<Item>) = provider.restore(items)

    fun trashItems(items: List<Item>): Int = provider.trashItems(items)

    fun undoTrashing(id: Int) = provider.undoTrashing(id)

    fun untag(items: List<Item>) = items.forEach { provider.untagItem(it, collectionID) }

    fun tagItems(items: List<Item>, tag: String) = provider.tagItems(items, tag)

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
            provider.colorizeCollection(col, color)
            liveColor.postValue(color)
        }
    }
}
