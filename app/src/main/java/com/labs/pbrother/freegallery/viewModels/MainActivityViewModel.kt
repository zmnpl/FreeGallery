package com.labs.pbrother.freegallery.viewModels

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.app
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.Item.Companion.SORT_DESC
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.extension.permissionsGood
import com.labs.pbrother.freegallery.prefs
import org.jetbrains.anko.doAsync
import java.io.File

/**
 * Created by simon on 21.11.17.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private var provider = Provider()
    private var collectionID = ""
    private lateinit var collection: CollectionItem
    var drawerItems = MutableLiveData<ArrayList<CollectionItem>>()
    var overviewItems = MutableLiveData<ArrayList<CollectionItem>>()
    var collectionItem = MutableLiveData<CollectionItem>()
    var items = MutableLiveData<ArrayList<Item>>()
    var toolbarColor = MutableLiveData<Int>()
    var toolbarText = MutableLiveData<String>()

    val collectionType
        get() = collectionItem.value?.type

    val collectionId
        get() = collectionItem.value?.id

    val tags
        get() = provider.tags()

    init {
        if (app.permissionsGood) {
            doAsync {
                refreshDrawerItems()
                refreshOverviewItems()
            }
        }
    }

    fun refreshCollection(collectionId: String) {
        this.collectionID = collectionId
        collection = provider.collectionItem(collectionId)
        collectionItem.postValue(collection)
        toolbarColor.postValue(collection.color)
        toolbarText.postValue(collection.displayName)
    }

    fun refreshDrawerItems() {
        drawerItems.postValue(provider.drawerItems)
    }

    fun refreshOverviewItems(showHiddenCollectionItems: Boolean = false) {
        if (showHiddenCollectionItems) {
            overviewItems.postValue(provider.overviewItems)
            return
        }

        val overview = ArrayList(provider.overviewItems.filter {
            it.hide == false
        })

        overviewItems.postValue(overview)
    }

    fun showHiddenOverviewItems() {
        refreshOverviewItems(true)
    }

    fun setToolbarDefaults() {
        toolbarColor.postValue(prefs.defaultCollectionColor)
        toolbarText.postValue(app.getString(R.string.app_name))
    }

    fun refreshItems(cached: Boolean = false) {
        items.postValue(provider.itemsFor(collection, cached))
    }

    fun selectedItems(selection: List<Int>): List<Item> {
        val result = ArrayList<Item>()
        selection.forEach {
            val ci = items.value?.get(it)
            if (null != ci) result.add(ci)
        }
        return result
    }

    private fun selectedOverviewItems(selection: List<Int>): List<CollectionItem> {
        val result = ArrayList<CollectionItem>()
        selection.forEach {
            val ci = overviewItems.value?.get(it)
            if (null != ci) result.add(ci)
        }
        return result
    }

    fun hideOverviewItems(itemIndexes: List<Int>, hide: Boolean = false) {
        for (item in selectedOverviewItems(itemIndexes)) {
            provider.hideCollection(item, hide)
        }
    }

    fun colorizeMultiple(itemIndexes: List<Int>, color: Int) {
        for (item in selectedOverviewItems(itemIndexes)) {
            provider.colorizeCollection(item, color)
        }
    }

    fun setSortAsc() {
        if (Item.SORT_ORDER == Item.SORT_ASC) return
        Item.SORT_ORDER = Item.SORT_ASC
        refreshItems()
    }

    fun setSortDesc() {
        if (Item.SORT_ORDER == Item.SORT_DESC) return
        Item.SORT_ORDER = SORT_DESC
        refreshItems()
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
        toolbarColor.postValue(provider.colorizeCollection(collection, color))
    }

    fun removeColor() {
        toolbarColor.postValue(provider.uncolorCollection(collection))
    }
}
