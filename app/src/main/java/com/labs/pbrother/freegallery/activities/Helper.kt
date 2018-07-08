package com.labs.pbrother.freegallery.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v4.app.ActivityCompat.startActivityForResult
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem

/**
 * Created by simon on 10.09.17.
 */
val STARTED_FROM_ACTIVITY = 1
val PERMISSION_READ_WRITE_STORAGE = 1337

val READ_REQUEST_CODE = 42
val COLLECTION_ACTIVITY_REQUEST_CODE = 0
val IMAGE_SLIDE_ACTIVITY_REQUEST_CODE = 1
val EDIT_ACTIVITY_REQUEST_CODE = 2

val EXTRA_COLLECTIONID = "collectionId"
val EXTRA_COLLECTION_INDEX = "collectionIndex"
val EXTRA_ITEM_INDEX = "pic"
val EXTRA_STARTING_POINT = "startingPoint"
val EXTRA_SORT_ORDER = "sortOrder"
val DELETION = "deletion"
val SHOULD_RELOAD = "reload"
val CROP_SAVED = "savedCroppedImage"

fun adjustColorAlpha(color: Int, factor: Float): Int {
    val alpha = Math.round(Color.alpha(color) * factor)
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    return Color.argb(alpha, red, green, blue)
}

fun darkenColor(color: Int, factor: Float): Int {
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    return Color.rgb((red * factor).toInt(), (green * factor).toInt(), (blue * factor).toInt())
}
