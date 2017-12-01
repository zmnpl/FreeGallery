package com.labs.pbrother.freegallery.activities

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Foo


/**
 * Created by simon on 30.11.17.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    var foo = Foo(getApplication())

    fun refresh() {
        overviewItems?.postValue(foo.overviewItems)
        drawerItems?.postValue(foo.drawerItems)
    }

    private fun selectedItems(selection: List<Int>) : List<CollectionItem> {
        val result =  ArrayList<CollectionItem>()
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

    private var overviewItems = MutableLiveData<ArrayList<CollectionItem>>()
    fun getLiveOverviewItems(): MutableLiveData<ArrayList<CollectionItem>> {
        overviewItems.value = foo.overviewItems
        return overviewItems
    }

    private var drawerItems = MutableLiveData<ArrayList<CollectionItem>>()
    fun getLiveDrawerItems(): MutableLiveData<ArrayList<CollectionItem>> {
        drawerItems.value = foo.drawerItems
        return drawerItems
    }
}
