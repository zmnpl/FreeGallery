package com.labs.pbrother.freegallery.viewModels

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Provider


/**
 * Created by simon on 30.11.17.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private var foo = Provider(getApplication())

    var overviewItems = MutableLiveData<ArrayList<CollectionItem>>()
    var drawerItems = MutableLiveData<ArrayList<CollectionItem>>()

    fun refresh() {
        overviewItems.postValue(foo.overviewItems)
        drawerItems.postValue(foo.drawerItems)
    }

    fun post() {
        overviewItems.postValue(overviewItems.value)
        drawerItems.postValue(overviewItems.value)
    }

    private fun selectedItems(selection: List<Int>): List<CollectionItem> {
        val result = ArrayList<CollectionItem>()
        selection.forEach() {
            val ci = overviewItems.value?.get(it)
            if (null != ci) result.add(ci)
        }
        return result
    }

    fun hide(itemIndexes: List<Int>) {
        // TODO
    }

    fun colorize(itemIndexes: List<Int>, color: Int) {
        for (item in selectedItems(itemIndexes)) {
            foo.colorizeCollection(item, color)
        }
    }
}
