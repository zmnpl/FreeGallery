package com.labs.pbrother.freegallery.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.CollectionItem
import kotlinx.android.synthetic.main.drawer_tagitem.view.*
import java.util.*

/**
 * Created by simon on 05.12.15.
 */
class DrawerTagListAdapter(private val clickListener: ViewHolder.ClickListener,
                           private val context: Context,
                           private val collections: ArrayList<CollectionItem>)
    : SelectableAdapter<DrawerTagListAdapter.ViewHolder>() {

    override fun getItemCount(): Int = collections.size

    override fun getItemId(position: Int): Long = position.toLong()

    fun getItemStringId(position: Int): String = collections[position].id

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        // create a new view
        val itemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.drawer_tagitem, viewGroup, false)

        return ViewHolder(itemView, clickListener)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(overviewViewHolder: ViewHolder, position: Int) {
        val collection = this.collections[position]
        val dname = collection.displayName()
        overviewViewHolder.tagIcon.setTextColor(collection.color)
        overviewViewHolder.text.text = if (dname.startsWith(context.getString(R.string.tagLetter))) dname.substring(1) else dname
        overviewViewHolder.count.text = collection.count.toString()
    }

    class ViewHolder(v: View, private val listener: ClickListener?) : RecyclerView.ViewHolder(v), View.OnClickListener, View.OnLongClickListener {
        var tagIcon = v.drawerTagListItemTagSymbol
        var text = v.drawerTagListItemText
        var count = v.drawerTagListItemCount

        init {
            v.setOnClickListener(this)
            v.setOnLongClickListener(this)
            //itemFrame.setOnClickListener(this);
        }

        override fun onClick(v: View) {
            listener?.onDrawerItemClicked(adapterPosition)
        }

        override fun onLongClick(v: View): Boolean =
                listener?.onDrawerItemLongClicked(adapterPosition) == true

        interface ClickListener {
            fun onDrawerItemClicked(position: Int)
            fun onDrawerItemLongClicked(position: Int): Boolean
        }
    }
}
