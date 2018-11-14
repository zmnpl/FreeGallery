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
    private var provider = Provider(getApplication())

    var overviewItems = MutableLiveData<ArrayList<CollectionItem>>()
    var drawerItems = MutableLiveData<ArrayList<CollectionItem>>()

    fun refresh() {
        overviewItems.postValue(provider.overviewItems)
        drawerItems.postValue(provider.drawerItems)
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
            provider.colorizeCollection(item, color)
        }
    }
}
