package com.labs.pbrother.freegallery.fragments

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.GridLayoutManager
import android.view.*
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.MainActivity
import com.labs.pbrother.freegallery.activities.MainActivityViewModel
import com.labs.pbrother.freegallery.adapters.OverviewRecyclerViewAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import kotlinx.android.synthetic.main.fragment_overview.*
import kotlinx.android.synthetic.main.fragment_overview.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class OverviewFragment : android.support.v4.app.Fragment(), OverviewRecyclerViewAdapter.ViewHolder.ClickListener {

    private lateinit var viewModel: MainActivityViewModel
    private var listener: OnMainFragmentInteractionListener? = null
    private lateinit var adapter: OverviewRecyclerViewAdapter
    private var actionMode: ActionMode? = null
    private val actionModeCallback = ActionModeCallback()
    private var selection: List<Int>? = null
    private var actionModeCollectionItems = ArrayList<CollectionItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_overview, container, false)
        rootView.overviewRecycler.apply {
            setHasFixedSize(true)
            val ctx = activity as Context
            layoutManager = GridLayoutManager(ctx, prefs.mainColumnsInPortrait)
            addItemDecoration(ItemOffsetDecoration(ctx, R.dimen.collection_picture_padding, prefs.mainColumnsInPortrait))
        }
        rootView.swipeRefreshMain.setOnRefreshListener { refresh() }
        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMainFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnMainFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun bindViewModel() {
        viewModel = ViewModelProviders.of(activity as MainActivity).get(MainActivityViewModel::class.java!!)

        viewModel.overviewItems.observe(activity as MainActivity, Observer { overviewItems ->
            populateAdapter(overviewItems)
        })
    }

    private fun populateAdapter(overviewItems: ArrayList<CollectionItem>?) {
        if (null != overviewItems) {
            val fract = activity as FragmentActivity
            adapter = OverviewRecyclerViewAdapter(this, fract, overviewItems, Provider(fract.application))
            adapter.setHasStableIds(true)
            overviewRecycler.adapter = adapter
        }
    }

    private fun refresh() {
        swipeRefreshMain.isRefreshing = true
        doAsync {
            viewModel.refresh()
            uiThread {
                swipeRefreshMain.isRefreshing = false
            }
        }
    }

    // clicks on item in main view
    override fun onItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
        } else {
            listener?.openCollectionView(position, adapter.getItemStringId(position))
        }
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
    private fun colorize() {
        ColorizeDialogFragment().show(activity?.fragmentManager, "colorizedialog")
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnMainFragmentInteractionListener {
        fun openCollectionView(position: Int, id: String)
    }

    private inner class ActionModeCallback : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_main_overviewselected, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            selection = adapter.getSelectedItems()
            when (item.itemId) {
                R.id.overviewselection_menu_hidegroup -> {
                    mode.finish()
                    return true
                }
                R.id.overviewselection_menu_colorizegroup -> {
                    colorize()
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
