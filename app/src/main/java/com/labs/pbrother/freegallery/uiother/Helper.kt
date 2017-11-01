package com.labs.pbrother.freegallery.uiother

import android.support.v7.app.AppCompatActivity

/**
 * Created by simon on 27.08.17.
 */
fun getStatusBarHeight(a: AppCompatActivity): Int {
    var result = 0
    val resourceId = a.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = a.resources.getDimensionPixelSize(resourceId)
    }
    return result
}
