package com.labs.pbrother.freegallery.controller

import android.provider.MediaStore


/**
 * Created by simon on 17.08.17.
 */


data class CollectionMeta(val id: String, val loved: Boolean, val color: Int)

data class ItemTag(val path: String, val tag: String)
data class TrashLog(val originalPath: String, val trashPath: String)
data class TrashItem(var path: String, var mediatype: Int)


val TYPE_FOLDER = "FOLDER"
val TYPE_TAG = "TAG"
val DUMMY_ID = "CIDUMMY"

val TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
val TPYE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

// Image selection
val CID = 0
val PATH = 1
val DATEADDED = 2
val DATETAKEN = 3
val WIDTH = 4
val HEIGHT = 5
val SIZE = 6
val BUCKETID = 7

val IMAGE_PROJECTION = arrayOf("DISTINCT " +
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.BUCKET_ID
)


val VID_PROJECTION = arrayOf("DISTINCT " +
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.BUCKET_ID)

interface MetaUpdatorizer {
    // for collections
    fun loveCollection(collection: CollectionItem, loved: Boolean)

    fun colorizeCollection(collection: CollectionItem, colorid: Int?)

    // for items
    fun tagItem(item: Item, tag: String)

    fun untagItem(item: Item, tag: String)
}

