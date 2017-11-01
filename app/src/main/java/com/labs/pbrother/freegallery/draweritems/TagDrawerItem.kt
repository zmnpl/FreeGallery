package com.labs.pbrother.freegallery.draweritems

import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.labs.pbrother.freegallery.R
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.BaseDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.ColorfulBadgeable
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.mikepenz.materialize.util.UIUtils

/**
 * Created by simon on 05.09.17.
 */

class TagDrawerItem : BaseDrawerItem<TagDrawerItem, TagDrawerItem.ViewHolder>(), ColorfulBadgeable<TagDrawerItem> {

    internal var tagLetter: StringHolder? = null
        private set
    internal var tagColor: ColorHolder? = null
        private set
    private var mBadge: StringHolder? = null
    private var mBadgeStyle = BadgeStyle()


    fun withTagLetter(tagLetter: String): TagDrawerItem {
        this.tagLetter = StringHolder(tagLetter)
        return this
    }

    fun withTagLetterRes(@StringRes tagLetterRes: Int): TagDrawerItem {
        this.tagLetter = StringHolder(tagLetterRes)
        return this
    }

    fun tagColor(@ColorInt color: Int): TagDrawerItem {
        this.tagColor = ColorHolder.fromColor(color)
        return this
    }

    fun tagColorRes(@ColorRes colorRes: Int): TagDrawerItem {
        this.tagColor = ColorHolder.fromColorRes(colorRes)
        return this
    }

    override fun getBadgeStyle(): BadgeStyle {
        return mBadgeStyle
    }

    override fun withBadge(badge: StringHolder): TagDrawerItem {
        this.mBadge = badge
        return this
    }

    override fun withBadge(badge: String): TagDrawerItem {
        this.mBadge = StringHolder(badge)
        return this
    }

    override fun withBadge(@StringRes badgeRes: Int): TagDrawerItem {
        this.mBadge = StringHolder(badgeRes)
        return this
    }

    override fun withBadgeStyle(badgeStyle: BadgeStyle): TagDrawerItem {
        this.mBadgeStyle = badgeStyle
        return this
    }

    override fun getBadge(): StringHolder? {
        return mBadge
    }

    override fun getType(): Int {
        return R.id.tag_drawer_item
    }

    @LayoutRes
    override fun getLayoutRes(): Int {
        return R.layout.tag_drawer_item
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    override fun bindView(viewHolder: ViewHolder, payloads: List<*>?) {
        super.bindView(viewHolder, payloads)
        val ctx = viewHolder.itemView.context

        //set the identifier from the drawerItem here. It can be used to run tests
        viewHolder.itemView.id = hashCode()

        //set the item selected if it is
        viewHolder.itemView.isSelected = isSelected

        //get the correct color for the background
        val selectedColor = getSelectedColor(ctx)

        //get the correct color for the text
        val color = getColor(ctx)
        val selectedTextColor = getSelectedTextColor(ctx)

        //set the background for the item
        UIUtils.setBackground(viewHolder.view, UIUtils.getSelectableBackground(ctx, selectedColor, true))

        //set the text for the name
        StringHolder.applyTo(this.getName(), viewHolder.name)
        StringHolder.applyTo(this.tagLetter, viewHolder.tagSymbol)

        //set the colors for textViews
        viewHolder.name.setTextColor(getTextColorStateList(color, selectedTextColor))
        //set the tagLetter text color
        ColorHolder.applyToOr(tagColor, viewHolder.tagSymbol, getTextColorStateList(color, selectedTextColor))

        //define the typeface for our textViews
        if (getTypeface() != null) {
            viewHolder.name.typeface = getTypeface()
        }

        //for android API 17 --> Padding not applied via xml
        DrawerUIUtils.setDrawerVerticalPadding(viewHolder.view)

        //set the text for the badge or hide
        val badgeVisible = StringHolder.applyToOrHide(mBadge, viewHolder.badge)
        //style the badge if it is visible
        if (badgeVisible) {
            mBadgeStyle.style(viewHolder.badge, getTextColorStateList(getColor(ctx), getSelectedTextColor(ctx)))
            viewHolder.badgeContainer.visibility = View.VISIBLE
        } else {
            viewHolder.badgeContainer.visibility = View.GONE
        }

        //define the typeface for our textViews
        if (getTypeface() != null) {
            viewHolder.badge.typeface = getTypeface()
        }

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, viewHolder.itemView)
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val badgeContainer: View
        val badge: TextView
        val name: TextView
        val tagSymbol: TextView


        init {
            this.tagSymbol = view.findViewById<View>(R.id.material_drawer_tagsymbol) as TextView
            this.name = view.findViewById<View>(R.id.material_drawer_name) as TextView
            this.badgeContainer = view.findViewById(R.id.material_drawer_badge_container)
            this.badge = view.findViewById<View>(R.id.material_drawer_badge) as TextView
        }
    }
}
