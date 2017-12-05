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

    fun refresh(collectionId: String) {
        val collection = foo.collectionItem(collectionId)
        collectionItem.postValue(collection)
        liveColor.postValue(collection.color)
        drawerItems.postValue(foo.drawerItems)
        items.postValue(foo.cachedItemsFor(collectionItem.value!!, Item.SORT_ORDER))
    }

    private fun selectedItems(selection: List<Int>) : List<Item> {
        val result =  ArrayList<Item>()
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

    fun urisToShare(selectedItems: List<Int>) : ArrayList<Uri> {
        val uris = ArrayList<Uri>()
        for (i in selectedItems) {
            val bar = items.value
            if (null != bar) {
                val (_, path) = bar[i]
                uris.add(FileProvider.getUriForFile(getApplication(), getApplication<Application>().packageName + ".provider", File(path)))
            }
        }
        return uris
    }

    fun restoreItems(selectedItems: List<Int>) {
        val restoreList = ArrayList<Item>()
        val itms = items.value
        if (null != itms) selectedItems.forEach { restoreList.add(itms[it]) }
        foo.restore(restoreList)
    }

    fun trashItems(selectedItems: List<Int>) : Int {
        val itms = items.value
        if(null != itms) {
            val deletionItems = ArrayList<Item>()
            selectedItems.forEach { deletionItems.add(itms[it]) }
            return foo.trashItems(itms)
        }
        return -1
    }

    fun tagItems(selectedItems: List<Int>, tag: String) {
        val itms = items.value
        if(null != itms) selectedItems.forEach {
            foo.tagItem(itms[it], tag)
        }
    }

    fun colorizeCollection(color: Int) {
        val col = collectionItem.value
        if(null != col) {
            foo.colorizeCollection(col, color)
            liveColor.value = color
        }
    }

    fun undoTrashing(id: Int) = foo.undoTrashing(id)
}
