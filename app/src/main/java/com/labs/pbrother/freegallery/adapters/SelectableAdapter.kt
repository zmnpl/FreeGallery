package com.labs.pbrother.freegallery.adapters

import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import java.util.*

/**
 * Created by simon on 13.11.16.
 */

/**
 * Class in majority copied from tutorial from:
 * Copyright © 2016 - Marc Plano-Lesay
 * https://enoent.fr/blog/2015/01/18/recyclerview-basics/
 */


abstract class SelectableAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    /**
     * Indicates if the item at position position is selected
     *
     * @param position Position of the item to check
     * @return true if the item is selected, false otherwise
     */
    fun isSelected(position: Int): Boolean = getSelectedItems().contains(position)

    /**
     * Toggle the selection status of the item at a given position
     *
     * @param position Position of the item to toggle the selection status for
     */
    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    /**
     * Clear the selection status for all items
     */
    fun clearSelection() {
        val selection = getSelectedItems()
        selectedItems.clear()
        for (i in selection) {
            notifyItemChanged(i)
        }
    }

    /**
     * Count the selected items
     *
     * @return Selected items count
     */
    val selectedItemCount: Int
        get() = selectedItems.size()

    /**
     * Indicates the list of selected items
     *
     * @return List of selected items ids
     */
    fun getSelectedItems(): List<Int> {
        val items = ArrayList<Int>(selectedItems.size())
        (0 until selectedItems.size()).mapTo(items) { selectedItems.keyAt(it) }
        return items
    }

    companion object {
        private val TAG = SelectableAdapter::class.java.simpleName
    }
}
