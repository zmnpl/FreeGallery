package com.labs.pbrother.freegallery.activities

import android.content.Context
import android.graphics.Color
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.ionicons_typeface_library.Ionicons
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem

/**
 * Created by simon on 10.09.17.
 */
val DELETED_SMTH = 1337
val STARTED_FROM_ACTIVITY = 1

val EXTRA_COLLECTIONID = "collectionId"
val EXTRA_COLLECTION_INDEX = "collectionIndex"
val EXTRA_ITEM_INDEX = "pic"
val EXTRA_STARTING_POINT = "startingpoint"

fun primaryDrawerItemFromItem(context: Context, item: CollectionItem, tagLetter: String): PrimaryDrawerItem {
    return PrimaryDrawerItem()
            .withName(item.displayName.removePrefix(tagLetter))
            .withIcon(Ionicons.Icon.ion_pound)
            .withBadge(item.count.toString())
            .withBadgeStyle(BadgeStyle(item.color, context.getColor(R.color.material_drawer_dark_background)))
            .withSelectedColor(adjustColorAlpha(item.color, 0.9F)) // Set some transparancy for selection to make badge and tag shine through
            .withSelectedIconColor(item.color)
}

fun tagSymbcol(context: Context): IconicsDrawable {
    return IconicsDrawable(context)
            .icon(Ionicons.Icon.ion_pound)
            .color(Color.WHITE)
            .sizeDp(24)
}

fun adjustColorAlpha(color: Int, factor: Float): Int {
    val alpha = Math.round(Color.alpha(color) * factor)
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    return Color.argb(alpha, red, green, blue)
}
