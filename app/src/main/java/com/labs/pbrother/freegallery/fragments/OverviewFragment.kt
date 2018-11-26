package com.labs.pbrother.freegallery.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.adapters.OverviewRecyclerViewAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.labs.pbrother.freegallery.viewModels.MainActivityViewModel
import kotlinx.android.synthetic.main.fragment_overview.*
import kotlinx.android.synthetic.main.fragment_overview.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class OverviewFragment : Fragment(), OverviewRecyclerViewAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener {

    // interaction interface
    private var listener: OnMainFragmentInteractionListener? = null

    interface OnMainFragmentInteractionListener {
        fun openCollectionView(position: Int, id: String)
        fun setToolbarDefaultColor()
        fun setToolbarDefaultName()
    }

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var adapter: OverviewRecyclerViewAdapter
    private var actionMode: ActionMode? = null
    private val actionModeCallback = ActionModeCallback()
    private var actionModeCollectionItems = ArrayList<CollectionItem>()
    private var selection: List<Int>? = null
    private var rv: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        doAsync {
            viewModel.refreshDrawerItems()
            viewModel.refreshOverviewItems()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // dont rebuild if still existing, to keep scrolling position
        if (rv == null) {
            rv = inflater.inflate(R.layout.fragment_overview, container, false)
            rv?.overviewRecycler?.apply {
                setHasFixedSize(true)
                val ctx = activity as Context
                layoutManager = GridLayoutManager(ctx, prefs.mainColumnsInPortrait)
                addItemDecoration(ItemOffsetDecoration(ctx, R.dimen.collection_picture_padding, prefs.mainColumnsInPortrait))
            }
            rv?.swipeRefreshMain?.setOnRefreshListener { refresh() }
        }
        return rv
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if(!swipeRefreshMain.isRefreshing) swipeRefreshMain.isRefreshing = true

        listener?.setToolbarDefaultColor()
        listener?.setToolbarDefaultName()

        viewModel.overviewItems.observe(viewLifecycleOwner, Observer { overviewItems ->
            if (overviewItems != null) {
                val fract = activity as FragmentActivity
                adapter = OverviewRecyclerViewAdapter(this, fract, overviewItems, Provider())
                adapter.setHasStableIds(true)
                overviewRecycler.adapter = adapter
                swipeRefreshMain.isRefreshing = false
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_overview, menu)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMainFragmentInteractionListener) {
            listener = context
            return
        }
        throw RuntimeException(context.toString() + " must implement OnMainFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_refresh -> {
                swipeRefreshMain.isRefreshing = true
                refresh()
                return true
            }
            R.id.menu_collectionZoomViewIn -> {
                applyZoom(-1)
                return true
            }
            R.id.menu_collectionZoomViewOut -> {
                applyZoom(+1)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // clicks on item in main view
    override fun onItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
            return
        }
        listener?.openCollectionView(position, adapter.getItemStringId(position))
    }

    override fun onItemLongClicked(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        toggleSelection(position)
        return true
    }

    // Toggle the selection state of an item.
    // If the item was the last one in the selection and is unselected, the selection is stopped.
    // Note that the selection must already be started (actionMode must not be null).
    private fun toggleSelection(position: Int) {
        adapter.toggleSelection(position)
        val total = adapter.itemCount
        val count = adapter.selectedItemCount

        if (count == 0) {
            actionMode?.finish()
        } else {
            actionMode?.title = (resources.getString(
                    R.string.collectionSelection)
                    + " "
                    + total.toString()
                    + " / "
                    + count.toString())
            actionMode?.invalidate()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Functionality
    // Callbacks
    override fun colorCancel() {}

    override fun colorOk(color: Int) {
        viewModel.colorizeMultiple(selection ?: ArrayList<Int>(), color)
        adapter.notifyDataSetChanged()
        selection = null
        actionModeCollectionItems.clear()
    }

    // trigger refresh of data
    private fun refresh() {
        swipeRefreshMain.isRefreshing = true
        doAsync {
            viewModel.refreshDrawerItems()
            viewModel.refreshOverviewItems()
            uiThread {
                swipeRefreshMain.isRefreshing = false
            }
        }
    }

    // zoom in or out
    private fun applyZoom(zoom: Int) {
        var cols = prefs.mainColumnsInPortrait
        cols += zoom
        if (cols < 1) cols = 1
        prefs.mainColumnsInPortrait = cols
        overviewRecycler.layoutManager = GridLayoutManager(activity, cols)
    }

    private inner class ActionModeCallback : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_main_overviewselected, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            selection = adapter.getSelectedItems()
            when (item.itemId) {
                R.id.overviewselection_menu_hidegroup -> {
                    mode.finish()
                    return true
                }
                R.id.overviewselection_menu_colorizegroup -> {
                    ColorizeDialogFragment().show(childFragmentManager, "colorizedialog")
                    mode.finish()
                    return true
                }
                else -> {
                    actionModeCollectionItems.clear()
                    selection = null
                    return false
                }
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            adapter.clearSelection()
            actionMode = null
        }
    }
}
