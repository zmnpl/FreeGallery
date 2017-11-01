package com.labs.pbrother.freegallery.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import com.bumptech.glide.Glide
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.adjustColorAlpha
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.MetaUpdatorizer
import kotlinx.android.synthetic.main.overview_item.view.*
import java.util.*

/**
 * Created by simon on 05.12.15.
 */
class OverviewRecyclerViewAdapter(private val clickListener: ViewHolder.ClickListener, private val context: Context, private val collections: ArrayList<CollectionItem>, private val metaUpdater: MetaUpdatorizer) : SelectableAdapter<OverviewRecyclerViewAdapter.ViewHolder>() {

    override fun getItemCount(): Int = collections.size

    override fun getItemId(position: Int): Long = position.toLong()

    fun getItemStringId(position: Int): String = collections[position].id

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        // create a new view
        val itemView = LayoutInflater.from(viewGroup.context).inflate(R.layout.overview_item, viewGroup, false)

        return ViewHolder(itemView, clickListener, metaUpdater)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(overviewViewHolder: ViewHolder, position: Int) {
        val collection = this.collections[position]

        overviewViewHolder.headline.text = collection.displayName
        overviewViewHolder.picCount.text = collection.count.toString()
        overviewViewHolder.textBackground.setBackgroundColor(adjustColorAlpha(collection.color, 0.75F))
        overviewViewHolder.favToggle.isChecked = collection.isLoved
        overviewViewHolder.favToggle.setOnClickListener { v ->
            val ft = v as ToggleButton
            metaUpdater.loveCollection(collection, ft.isChecked)
        }

        // TODO - resizing fit to target size ?
        Glide.with(context).load(collection.thumbUrl).into(overviewViewHolder.thumb)

        overviewViewHolder.selectedOverlay.visibility = if (isSelected(position)) View.VISIBLE else View.INVISIBLE
    }

    class ViewHolder(v: View, private val listener: ClickListener?, metaUpdater: MetaUpdatorizer) : RecyclerView.ViewHolder(v), View.OnClickListener, View.OnLongClickListener {
        var headline = v.OverviewItem_txtHeadline
        var picCount = v.OverviewItem_txtCount
        var textBackground = v.OverviewItem_headerContainer
        var thumb = v.OverviewItem_imgThumb
        var favToggle = v.OverviewItem_FavouriteToggle
        var selectedOverlay = v.OverviewItem_selectedOverlay
        protected var itemFrame = v.OverviewItem_ItemFrame

        init {
            itemFrame.setOnClickListener(this)
            itemFrame.setOnLongClickListener(this)
            favToggle.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            if (listener != null) {
                when (v.id) {
                    R.id.OverviewItem_ItemFrame -> {
                        listener.onItemClicked(adapterPosition)
                    }
                }
            }
        }

        override fun onLongClick(v: View): Boolean =
                listener?.onItemLongClicked(adapterPosition) == true

        interface ClickListener {
            fun onItemClicked(position: Int)
            fun onItemLongClicked(position: Int): Boolean
        }
    }
}
