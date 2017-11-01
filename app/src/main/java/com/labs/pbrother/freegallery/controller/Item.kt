package com.labs.pbrother.freegallery.controller

import android.provider.MediaStore
import java.io.File
import java.util.ArrayList
import kotlin.collections.HashSet

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

    fun id(): String = path

    fun fileUrl(): String = "file://" + path

    fun fileName(): String = File(path).name

    fun tags(): ArrayList<String> = ArrayList(tags.toList())

    fun addTag(tag: String) {
        tags.add(tag)
    }

    fun untag(tag: String) {
        tags.remove(tag)
    }

    override operator fun compareTo(citem: Item): Int = if (dateAdded < citem.dateAdded) 1 else -1

}

