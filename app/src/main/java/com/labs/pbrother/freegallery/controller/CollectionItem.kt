package com.labs.pbrother.freegallery.controller

import android.graphics.Color

/**
 * Created by simon on 01.12.15.
 */
data class CollectionItem constructor(val id: String = "",
                                      var type: String = "",
                                      var thumb: String = "",
                                      var count: Int = 0,
                                      var color: Int = 0,
                                      var isLoved: Boolean = false
) : Comparable<CollectionItem> {

    var displayName: String = ""

    init {
        makeDisplayName()
    }

    private fun makeDisplayName() {
        if (type === TYPE_FOLDER) {
            val parts = id.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            displayName = parts[parts.size - 1]
            return
        }
        displayName = id
    }

    fun displayNameDetail(): String {
        return if (type === TYPE_TAG) {
            displayName
        } else nicePathNotation()
    }

    private fun nicePathNotation(): String {
        val t = id.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var nicepath = "/"
        for (i in t.indices) {
            if (t[i].isNotEmpty()) {
                if (i == t.size - 1) {
                    nicepath += t[i]
                } else {
                    nicepath = nicepath + t[i].substring(0, 1) + "/"
                }
            }
        }
        return nicepath
    }

    /**
     * @return Name of collection. Can be physical folder name, or name of virtual collection.
     */
    fun displayName(): String {
        return if (type === TYPE_TAG) {
            displayName
        } else "../" + displayName
    }

    /**
     * @return File url to thumbnail; default from first image in collection.
     */
    fun thumbUrl(): String = "file://" + thumb

    /**
     * Love this collection
     *
     * @param lv
     */
    fun love(lv: Boolean) {
        isLoved = lv
    }

    /**
     * @return ColorId
     */
    fun color(): Int = adjustAlpha(color, 1.0f) // leaving that in; might be helpful sometime

    /**
     * Setter method
     *
     * @param colorId
     */
    fun colorize(colorId: Int) {
        this.color = colorId
    }

    fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    override operator fun compareTo(ccitem: CollectionItem): Int = sortRegular(ccitem)

    private fun sortRegular(ccitem: CollectionItem): Int {
        return when {
            isLoved && !ccitem.isLoved -> -1
            !isLoved && ccitem.isLoved -> 1
            displayName == ccitem.displayName -> -1
            else -> displayName.compareTo(ccitem.displayName)
        }
    }
}

