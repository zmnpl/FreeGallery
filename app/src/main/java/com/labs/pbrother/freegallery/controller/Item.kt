package com.labs.pbrother.freegallery.controller

import android.media.ExifInterface
import android.provider.MediaStore
import com.labs.pbrother.freegallery.prefs
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

    private val exif = ExifInterface(path)

    val id: String
        get() = path

    val fileUriString: String
        get() = "file://" + path


    val fileName: String
        get() = File(path).name

    val tagsList: ArrayList<String>
        get() = ArrayList(tags.toList())

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

    // exif data

    val latitude: Double
        get() = exif.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, 0.0)

    val longitude: Double
        get() = exif.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, 0.0)

    val camera: String
        get() = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""

    val iso: Int
        get() = exif.getAttributeInt(ExifInterface.TAG_ISO_SPEED_RATINGS, 0)

    val exposureTime: Double
        get() = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)


}

