package com.labs.pbrother.freegallery.controller

import android.provider.MediaStore
import java.io.File

/**
 * Created by simon on 21.02.17.
 */
//@Parcelize
data class Item constructor(var type: Int = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                            var path: String = "",
                            var dateAdded: Long = 0, // milliseconds since 1. Jan 1970
                            var dateTaken: Long = 0,
                            var size: Long = 0,
                            var width: Int = 0,
                            var height: Int = 0,
                            var latitude: Double = 0.0,
                            var longitude: Double = 0.0,
                            private var tags: HashSet<String> = HashSet()
) : Comparable<Item> {

    companion object {
        var SORT_ORDER: Int = SORT_ITEMS_DESC
    }

    val id: String
        get() = path

    val fileUrl: String
        get() = "file://" + path


    val fileName: String
        get() = File(path).name

    val tagsList: ArrayList<String>
        get() = ArrayList(tags.toList())

    fun addTag(tag: String) {
        tags.add(tag)
    }

    fun untag(tag: String) {
        tags.remove(tag)
    }

    override operator fun compareTo(other: Item): Int = if (dateAdded < other.dateAdded) 1 else -1

}

