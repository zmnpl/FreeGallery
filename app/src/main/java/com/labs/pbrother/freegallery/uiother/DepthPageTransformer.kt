package com.labs.pbrother.freegallery.uiother

import android.support.v4.view.ViewPager
import android.view.View

/**
 * Created by simon on 03.05.17.
 * Copy from Android Developer Tutorial
 * https://developer.android.com/training/animation/screen-slide.html
 */

class DepthPageTransformer : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width

        when {
            position < -1 -> // [-Infinity,-1)
                // This page is way off-screen to the left.
                page.alpha = 0f
            position <= 0 -> { // [-1,0]
                // Use the default slide transition when moving to the left page
                page.alpha = 1f
                page.translationX = 0f
                page.scaleX = 1f
                page.scaleY = 1f

            }
            position <= 1 -> { // (0,1]
                // Fade the page out.
                page.alpha = 1 - position

                // Counteract the default slide transition
                page.translationX = pageWidth * -position

                // Scale the page down (between MIN_SCALE and 1)
                val scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position))
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor

            }
            else -> // (1,+Infinity]
                // This page is way off-screen to the right.
                page.alpha = 0f
        }
    }

    companion object {
        private val MIN_SCALE = 0.75f
    }

}

