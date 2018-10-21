package com.labs.pbrother.freegallery.controller

import android.media.ExifInterface
import com.labs.pbrother.freegallery.prefs
import java.io.File

/**
 * Created by simon on 21.02.17.
 */
//@Parcelize
data class Item constructor(var type: Int = TYPE_IMAGE,
                            var path: String = "",
                            var dateAdded: Long = 0, // milliseconds since 1. Jan 1970
                            var dateTaken: Long = 0,
                            var size: Long = 0,
                            var width: Int = 0,
                            var height: Int = 0,
                            private var tags: HashSet<String> = HashSet()
) : Comparable<Item> {

    companion object {
        val SORT_DESC = 1 // regular; newest to oldest
        val SORT_ASC = -1
        var SORT_ORDER = SORT_DESC

        val ORDER_BY_DATE_ADDED = 0
        val ORDER_BY_DATE_TAKEN = 1
        var ORDER_BY = ORDER_BY_DATE_ADDED
    }

    private val exif: ExifInterface by lazy {
        ExifInterface(path)
    }

    private val latLongHolder = floatArrayOf(0.0f, 0.0f)
    private val latLong: FloatArray by lazy {
        exif.getLatLong(latLongHolder)
        latLongHolder
    }

    val id: String
        get() = path

    val fileUriString: String
        get() = "file://" + path


    val fileName: String
        get() = File(path).name

    val tagsList: ArrayList<String>
        get() = ArrayList(tags.toList())

    val tagsString: String
        get() = tagsList.joinToString { ", " }

    fun addAllTags(tags: HashSet<String>) {
        this.tags = tags;
    }

    val isOnSDCard: Boolean
        get() = path.startsWith(prefs.sdCardRootPath)

    val tagString: String
        get() = tags.joinToString(", ")

    val isTagged: Boolean
        get() = tags.size > 0

    fun addTag(tag: String) {
        tags.add(tag)
    }

    fun untag(tag: String) {
        tags.remove(tag)
    }

    override operator fun compareTo(other: Item): Int = when (ORDER_BY) {
        ORDER_BY_DATE_TAKEN -> compareByDateTaken(other)
        else -> compareByDateAdded(other)
    }

    private fun compareByDateAdded(other: Item): Int = if (dateAdded < other.dateAdded) 1 else -1
    private fun compareByDateTaken(other: Item): Int = if (dateTaken < other.dateTaken) 1 else -1

    val latitude: Double
        get() = when (type) {
            TYPE_IMAGE -> {
                latLong[0].toDouble()
            }
            else -> 0.0
        }

    val longitude: Double
        get() = when (type) {
            TYPE_IMAGE -> {
                latLong[1].toDouble()
            }
            else -> 0.0
        }

    val camera: String
        get() = if (type == TYPE_IMAGE) {
            var make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
            if (make != "") {
                make = make + ", "
            }
            val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
            make + model
        } else {
            ""
        }

    val iso: Int
        get() = if (type == TYPE_IMAGE) {
            exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)
        } else {
            0
        }

    val exposureTimeSeconds: Double
        get() = if (type == TYPE_IMAGE) {
            exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
        } else {
            0.0
        }

}

