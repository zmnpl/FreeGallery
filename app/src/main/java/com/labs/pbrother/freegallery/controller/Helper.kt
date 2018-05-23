package com.labs.pbrother.freegallery.controller

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
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
val LAT = 6
val LONG = 7
val SIZE = 8
val BUCKETID = 9

val IMAGE_PROJECTION = arrayOf("DISTINCT " +
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        //MediaStore.Images.Media.INTERNAL_CONTENT_URI,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.LATITUDE,
        MediaStore.Images.Media.LONGITUDE,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.BUCKET_ID)

val VID_PROJECTION = arrayOf("DISTINCT " +
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.LATITUDE,
        MediaStore.Images.Media.LONGITUDE,
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


// mostly copied from simple gallery; try out that one!
fun Context.getRealPathFromURI(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }

    when (uri.authority) {
        "com.android.providers.downloads.documents" -> {
            val id = DocumentsContract.getDocumentId(uri)
            val idLong = id.toLongOrNull()
            if (null != idLong) {
                val newUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), idLong)
                val path = getDataColumn(newUri)
                if (path != null) {
                    return path
                }
            }
        }

        "com.android.externalstorage.documents" -> {
            val documentId = DocumentsContract.getDocumentId(uri)
            val parts = documentId.split(":")
            if (parts[0].equals("primary", true)) {
                return "${Environment.getExternalStorageDirectory().absolutePath}/${parts[1]}"
            }
        }

        "com.android.providers.media.documents" -> {
            val documentId = DocumentsContract.getDocumentId(uri)
            val split = documentId.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]

            val contentUri = when (type) {
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])
            val path = getDataColumn(contentUri, selection, selectionArgs)
            if (path != null) {
                return path
            }
        }

        else -> {
        }
    }

    return getDataColumn(uri)
}

fun Context.getDataColumn(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): String? {
    var cursor: Cursor? = null
    try {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            return cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}
