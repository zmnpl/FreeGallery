package com.labs.pbrother.freegallery.extension

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.Surface
import android.view.WindowManager
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.adjustColorAlpha
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.prefs
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import java.io.File


val PORTRAIT = "p"
val LANDSCAPE = "l"
val REVERSE_PORTRAIT = "rp"
val REVERSE_LANDSCAPE = "rl"

fun Context.getNavBarWidth(): Int {
    val r = resources
    val id = r.getIdentifier("navigation_bar_width", "dimen", "android")
    return r.getDimensionPixelSize(id)
}

fun Context.getNavBarHeight(): Int {
    val r = resources
    val id = r.getIdentifier("navigation_bar_height", "dimen", "android")
    return r.getDimensionPixelSize(id)
}

fun Context.getRotation(): String {
    val rotation = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.orientation
    return when (rotation) {
        Surface.ROTATION_0 -> PORTRAIT
        Surface.ROTATION_90 -> LANDSCAPE
        Surface.ROTATION_180 -> REVERSE_PORTRAIT
        else -> REVERSE_LANDSCAPE
    }
}

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

fun Context.primaryDrawerItemFromItem(item: CollectionItem, tagLetter: String): PrimaryDrawerItem {
    return PrimaryDrawerItem()
            .withTag(item.id)
            .withName(item.displayName.removePrefix(tagLetter))
            .withIcon(Ionicons.Icon.ion_pound)
            .withSelectedIconColor(item.color)
            .withBadge(item.count.toString())
            .withBadgeStyle(BadgeStyle(item.color, getColor(R.color.material_drawer_dark_background)))
            .withSelectedColor(adjustColorAlpha(item.color, 0.9F)) // Set some transparancy for selection to make badge and tag shine through
            .withSelectedIconColor(item.color)
            .withSelectedTextColor(Color.WHITE)
}

fun Context.drawerHomeItem(): PrimaryDrawerItem {
    val color = getColor(R.color.accent)
    return PrimaryDrawerItem()
            .withName(getString(R.string.drawerHome))
            .withIcon(Ionicons.Icon.ion_home)
            .withSelectedIconColor(color)
            .withSelectedColor(adjustColorAlpha(color, 0.9F)) // Set some transparancy for selection to make badge and tag shine through
            .withSelectedIconColor(color)
            .withSelectedTextColor(Color.WHITE)
}

fun Context.tagSymbol(): IconicsDrawable {
    return IconicsDrawable(this)
            .icon(Ionicons.Icon.ion_pound)
            .color(Color.WHITE)
            .sizeDp(24)
}

fun Context.getImageContentUri(imageFile: File): Uri? {
    val filePath = imageFile.getAbsolutePath()
    val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA + "=? ",
            arrayOf(filePath), null)

    if (cursor != null && cursor.moveToFirst()) {
        val id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
        cursor.close()
        return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id)
    } else {
        if (imageFile.exists()) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATA, filePath)
            return contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } else {
            return null
        }
    }
}

// first element is primary, so second (probably -.-) is sd
fun Context.discoverSDPath() {
    val stores = getExternalFilesDirs(Environment.DIRECTORY_PICTURES)
    if (stores.size > 1) {
        prefs.sdCardRootPath = stores[1].absolutePath.split("/Android")[0]
    }
}
