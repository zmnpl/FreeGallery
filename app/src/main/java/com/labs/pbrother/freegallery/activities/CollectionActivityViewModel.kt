package com.labs.pbrother.freegallery.activities

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import android.support.v4.content.FileProvider
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Foo
import com.labs.pbrother.freegallery.controller.Item
import java.io.File

/**
 * Created by simon on 21.11.17.
 */
class CollectionActivityViewModel(application: Application) : AndroidViewModel(application) {
    var foo = Foo(getApplication())

    val collectionType
        get() = collectionItem.value?.type

    val tags
        get() = foo.tags()

    var drawerItems = MutableLiveData<ArrayList<CollectionItem>>()
    var collectionItem = MutableLiveData<CollectionItem>()
    var items = MutableLiveData<ArrayList<Item>>()
    var liveColor = MutableLiveData<Int>()

    fun refresh(collectionId: String, full: Boolean = false) {
        val collection = foo.collectionItem(collectionId)

        collectionItem.postValue(collection)
        liveColor.postValue(collection.color)
        drawerItems.postValue(foo.drawerItems)
        if (full) {
            items.postValue(foo.itemsFor(collectionItem.value!!, Item.SORT_ORDER))
        } else {
            items.postValue(foo.cachedItemsFor(collectionItem.value!!, Item.SORT_ORDER))
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

    fun colorize(collection: CollectionItem, color: Int) {
        foo.colorizeCollection(collection, color)
    }

    fun deleteTag() = foo.deleteTag(collectionItem.value?.id ?: "")

    fun emptyTrash() = foo.emptyTrash()

    fun restoreItems(items: List<Item>) = foo.restore(items)

    fun trashItems(items: List<Item>): Int = foo.trashItems(items)

    fun undoTrashing(id: Int) = foo.undoTrashing(id)

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
            liveColor.value = color
        }
    }
}
