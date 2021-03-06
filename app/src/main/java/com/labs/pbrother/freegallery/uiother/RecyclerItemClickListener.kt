package com.labs.pbrother.freegallery.uiother

/**
 * Created by simon on 05.12.15.
 */

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

// Tutorial from http://sapandiwakar.in/recycler-view-item-click-handler/

class RecyclerItemClickListener(context: Context, private val listener: OnItemClickListener?) : androidx.recyclerview.widget.RecyclerView.OnItemTouchListener {

    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    private var mGestureDetector: GestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean = true
    })

    override fun onInterceptTouchEvent(view: androidx.recyclerview.widget.RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)
        if (childView != null && listener != null && mGestureDetector.onTouchEvent(e)) {
            listener.onItemClick(childView, view.getChildAdapterPosition(childView))
        }
        return false
    }

    override fun onTouchEvent(view: androidx.recyclerview.widget.RecyclerView, motionEvent: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}
