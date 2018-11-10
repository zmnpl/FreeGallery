package com.labs.pbrother.freegallery.uiother

/**
 * Created by simon on 06.12.15.
 */

import android.content.Context
import android.graphics.Rect
import android.support.annotation.DimenRes
import android.support.v7.widget.RecyclerView
import android.view.View

/**
 * https://gist.github.com/yqritc/ccca77dc42f2364777e1
 */

class ItemOffsetDecoration(private val itemOffset: Int = 1, private val colCount: Int = 2) : RecyclerView.ItemDecoration() {

    constructor(context: Context, @DimenRes itemOffsetId: Int, colCount: Int) : this(context.resources.getDimensionPixelSize(itemOffsetId), colCount)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.left = 0
        outRect.right = 0
        outRect.top = 0
        outRect.bottom = itemOffset
        val pos = parent.getChildAdapterPosition(view)
        if (pos % colCount > 0) {
            outRect.left = itemOffset
        }
    }
}
