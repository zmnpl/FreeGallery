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
class ImageSlideActivityViewModel(application: Application) : AndroidViewModel(application) {
    var foo = Foo(getApplication())

    val tags
        get() = foo.tags()

    var items = MutableLiveData<ArrayList<Item>>()

    fun refresh(collectionId: String, full: Boolean = false) {
        val collection = foo.collectionItem(collectionId)

        if (full) {
            items.postValue(foo.itemsFor(collection, Item.SORT_ORDER))
        } else {
            items.postValue(foo.cachedItemsFor(collection, Item.SORT_ORDER))
        }
    }

    fun trashItems(items: List<Item>): Int = foo.trashItems(items)

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
}
