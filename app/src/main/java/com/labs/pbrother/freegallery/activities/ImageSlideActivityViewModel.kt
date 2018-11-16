package com.labs.pbrother.freegallery.activities

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import android.support.v4.content.FileProvider
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.Provider
import org.jetbrains.anko.doAsync
import java.io.File

/**
 * Created by simon on 21.11.17.
 */
class ImageSlideActivityViewModel(application: Application) : AndroidViewModel(application) {
    private var foo = Provider()

    val tags
        get() = foo.tags()

    var items = MutableLiveData<ArrayList<Item>>()

    fun refresh(collectionId: String, full: Boolean = false) {
        val collection = foo.collectionItem(collectionId)
        items.postValue(foo.itemsFor(collection))
    }

    fun trashItems(items: List<Item>): Int = foo.trashItems(items)

    fun itemIdOf(index: Int): String = items.value?.get(index)?.id ?: ""

    fun itemAt(index: Int): Item? = items.value?.get(index)

    fun tagItems(items: List<Item>, tag: String) = items.forEach { tagItem(it, tag) }

    fun copyTags(oldPath: String, newPath: String) {
        foo.copyTags(oldPath, newPath)
    }

    fun tagItem(item: Item?, tag: String) = if (null != item) {
        foo.tagItem(item, tag)
    } else {
    }

    fun removeItem(item: Item): Int {
        val deletionItems = java.util.ArrayList<Item>()
        deletionItems.add(item)

        doAsync { foo.trashItems(deletionItems) }
        items.value?.remove(item)

        return items.value?.size ?: 0
    }

    fun urisToShare(items: List<Item>): ArrayList<Uri> {
        val uris = ArrayList<Uri>()
        items.forEach {
            uris.add(FileProvider.getUriForFile(getApplication(), getApplication<Application>().packageName + ".provider", File(it.path)))
        }
        return uris
    }

    fun getItemForExternalUri(uri: Uri) {
        val itms = ArrayList<Item>()
        val itm = foo.itemForUri(uri)
        itms.add(itm)
        items.postValue(itms)

        // Branch for when Activity gets called by intent from other app
        // TODO - let service create item
        // TODO - try to resolve image path and derive full folder collection item
    }

}
