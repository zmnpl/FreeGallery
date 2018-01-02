package com.labs.pbrother.freegallery.adapters

import android.content.Context
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.tagSymbol
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.MetaUpdatorizer
import kotlinx.android.synthetic.main.collection_item.view.*
import java.io.File
import java.util.*

/**
 * Created by simon on 05.12.15.
 */
class CollectionRecyclerViewAdapter(private val clickListener: ViewHolder.ClickListener,
                                    private val context: Context,
                                    private val items: ArrayList<Item>,
                                    private val metaUpdater: MetaUpdatorizer)
    : SelectableAdapter<CollectionRecyclerViewAdapter.ViewHolder>() {

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, items.size)
    }

    fun removeMultiple(positions: List<Int>) {
        // reverse sortOrder to delete from the last to the first
        Collections.sort(positions, Collections.reverseOrder<Any>())
        for (position in positions) {
            items.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, items.size)
        }
    }

    override fun getItemCount(): Int = items.size

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        // create a new view
        val itemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.collection_item, viewGroup, false)
        return ViewHolder(itemView, clickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items.get(position)
        val img = File(itm.path) // TODO - loading via file faster?
        //Glide.with(context).load(itm.fileUrl()).into(holder.pic);
        Glide.with(context).load(img).into(holder.pic)
        holder.selectedOverlay.visibility = if (isSelected(position)) View.VISIBLE else View.INVISIBLE
        holder.videoIconOverlay.visibility = if (itm.type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) View.VISIBLE else View.INVISIBLE
        holder.tagIndicator.background = tagSymbol(context)
        holder.tagIndicator.visibility = if (itm.isTagged) View.VISIBLE else View.INVISIBLE
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder(v: View, private val listener: ClickListener?) : RecyclerView.ViewHolder(v), View.OnClickListener, View.OnLongClickListener {
        var pic: ImageView = v.CollectionItem_imgPicture
        var videoIconOverlay = v.CollectionItem_videoItemOverlay
        var selectedOverlay = v.CollectionItem_selectedOverlay
        var tagIndicator = v.CollectionItem_tagIndicator

        init {
            v.setOnClickListener(this)
            v.setOnLongClickListener(this)
        }

        interface ClickListener {
            fun onItemClicked(position: Int)
            fun onItemLongClicked(position: Int): Boolean
        }

        override fun onClick(v: View) {
            listener?.onItemClicked(adapterPosition)
        }

        override fun onLongClick(v: View): Boolean =
                listener?.onItemLongClicked(adapterPosition) == true
    }
}
