package com.labs.pbrother.freegallery.settings

import android.content.Context
import android.view.Surface
import android.view.WindowManager

/**
 * Created by simon on 29.12.15.
 */
class DeviceConfiguration private constructor() {

    fun getNavBarWidth(context: Context): Int {
        val r = context.resources
        val id = r.getIdentifier("navigation_bar_width", "dimen", "android")
        return r.getDimensionPixelSize(id)
    }

    fun getNavBarHeight(context: Context): Int {
        val r = context.resources
        val id = r.getIdentifier("navigation_bar_height", "dimen", "android")
        return r.getDimensionPixelSize(id)
    }

    fun getRotation(context: Context): String {
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.orientation
        return when (rotation) {
            Surface.ROTATION_0 -> PORTRAIT
            Surface.ROTATION_90 -> LANDSCAPE
            Surface.ROTATION_180 -> REVERSE_PORTRAIT
            else -> REVERSE_LANDSCAPE
        }
    }

    companion object {
        @JvmStatic
        var instance = DeviceConfiguration()
        @JvmStatic
        val PORTRAIT = "p"
        @JvmStatic
        val LANDSCAPE = "l"
        @JvmStatic
        val REVERSE_PORTRAIT = "rp"
        @JvmStatic
        val REVERSE_LANDSCAPE = "rl"
    }
}
