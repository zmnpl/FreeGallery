package com.labs.pbrother.freegallery.extension

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore


// mostly copied from simple gallery; try out that one!
fun Context.getRealPathFromURI(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }

    when (uri.authority) {
        "com.android.providers.downloads.documents" -> {
            val documentId = DocumentsContract.getDocumentId(uri)
            val idLong = documentId.toLongOrNull()
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
