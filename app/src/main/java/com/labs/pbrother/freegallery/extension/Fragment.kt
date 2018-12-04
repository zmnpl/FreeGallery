package com.labs.pbrother.freegallery.extension

import androidx.fragment.app.Fragment
import com.labs.pbrother.freegallery.prefs

val androidx.fragment.app.Fragment.columns: Int
    get() = if (activity?.getRotation() === PORTRAIT || activity?.getRotation() === REVERSE_PORTRAIT) {
        prefs.columnsInPortrait
    } else {
        (prefs.columnsInPortrait * 1.5).toInt()
    }
