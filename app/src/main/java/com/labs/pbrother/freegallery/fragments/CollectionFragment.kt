package com.labs.pbrother.freegallery.fragments

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.Fragment
import androidx.core.app.NavUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.GridLayoutManager
import android.view.*
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.labs.pbrother.freegallery.*
import com.labs.pbrother.freegallery.activities.ImageSlideActivity
import com.labs.pbrother.freegallery.adapters.CollectionRecyclerViewAdapter
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.controller.TYPE_TAG
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.dialogs.TagDialogFragment
import com.labs.pbrother.freegallery.extension.columns
import com.labs.pbrother.freegallery.extension.tagSymbol
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.labs.pbrother.freegallery.viewModels.MainActivityViewModel
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.fragment_collection.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.image
import org.jetbrains.anko.support.v4.intentFor
import org.jetbrains.anko.uiThread

private const val CID = "collectionId"

class CollectionFragment : Fragment(), CollectionRecyclerViewAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener, TagDialogFragment.TagDialogListener {

    // parameters
    private lateinit var cid: String
    // other
    private lateinit var viewModel: MainActivityViewModel
    // ui
    private var rv: View? = null
    private lateinit var adapter: CollectionRecyclerViewAdapter
    private var actionMode: ActionMode? = null
    private val actionModeCallback = ActionModeCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)

        arguments?.let {
            val safeArgs = CollectionFragmentArgs.fromBundle(it)
            cid = safeArgs.collectionId
        }

        activity?.run {
            viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        }

        doAsync {
            viewModel.refreshCollection(cid)
            viewModel.refreshItems()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (rv == null) {
            rv = inflater.inflate(R.layout.fragment_collection, container, false)
            // recycler list
            val colCount = columns
            rv?.collection_rclPictureCollection?.apply {
                addItemDecoration(ItemOffsetDecoration(activity as Context, R.dimen.collection_picture_padding, colCount))
                setHasFixedSize(true)
                layoutManager = androidx.recyclerview.widget.GridLayoutManager(activity as Context, colCount)
                isSaveEnabled = true
            }

            rv?.swipeRefreshCollection?.setOnRefreshListener {
                refresh()
            }
        }
        return rv
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = true

        viewModel.items.observe(this, Observer { items ->
            if (null != items) {
                adapter = CollectionRecyclerViewAdapter(this@CollectionFragment, activity as Context, items, Provider())
                collection_rclPictureCollection.adapter = adapter
                if (swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = false
            }
        })

        collection_shareFloatingActionButton.setOnClickListener { tag() }
        collection_shareFloatingActionButton.image = activity?.tagSymbol()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnCollectionFragmentInteractionListener) {
//            listener = context
//            return
//        }
//        throw RuntimeException(context.toString() + " must implement OnMainFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
//        listener = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (cid == getString(R.string.trashName)) {
            inflater.inflate(R.menu.menu_collection_trash, menu)
        } else {
            inflater.inflate(R.menu.menu_collection, menu)
            if (viewModel.collectionType == TYPE_TAG && cid != getString(R.string.timelineName)) {
                menu.findItem(R.id.menu_deleteTag)?.isVisible = true
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(activity as Activity) // TODO - does that work?
                return true
            }
            R.id.menu_deleteTag -> {
                viewModel.deleteTag()
                NavHostFragment.findNavController(this).navigate(CollectionFragmentDirections.action_go_to_overview())
                return true
            }
            R.id.menu_refresh -> {
                refresh()
                return true
            }
            R.id.menu_colorize -> {
                ColorizeDialogFragment().show(childFragmentManager, "colorizedialog")
                return true
            }
            R.id.menu_resetColor -> {
                doAsync {
                    viewModel.removeColor()
                }
                return true
            }
            R.id.menu_trash_emptyTrash -> {
                emptyTrash()
                return true
            }
            R.id.menu_collectionSortAsc -> {
                viewModel.setSortAsc()
                return true
            }
            R.id.menu_collectionSortDesc -> {
                viewModel.setSortDesc()
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
            R.id.menu_selectAll -> {
                selectAll()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun refresh() {
        swipeRefreshCollection?.apply {
            if(!isRefreshing) isRefreshing = true
        }

        doAsync {
            viewModel.refreshCollection(cid)
            viewModel.refreshItems()
            viewModel.refreshDrawerItems()
            uiThread {
                swipeRefreshCollection?.isRefreshing = false
            }
        }
    }

    private fun emptyTrash() {
        val builder = AlertDialog.Builder(activity as Context)
        builder.setMessage(R.string.EmtyTrashQuestion)
        builder.setPositiveButton(R.string.EmtpyTrashOk) { dialog, id ->
            doAsync {
                viewModel.emptyTrash()
                uiThread {
                    NavHostFragment.findNavController(this@CollectionFragment).navigate(CollectionFragmentDirections.action_go_to_overview())
                }
            }
        }
        builder.setNegativeButton(R.string.EmptyTrashCancel) { dialog, id ->
            // user cancel
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun applyZoom(zoom: Int) {
        var colCount = columns
        colCount += zoom
        if (colCount < 1) colCount = 1
        prefs.columnsInPortrait = colCount
        collection_rclPictureCollection.layoutManager = androidx.recyclerview.widget.GridLayoutManager(activity, colCount)
    }

    private fun selectAll() {
        if (null == actionMode) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        adapter.clearSelection()
        var i = 0
        while (i < adapter.itemCount) {
            adapter.toggleSelection(i)
            i++
        }
        showSelectionCountInActionMode(i)
    }

    private fun share() {
        val uris = viewModel.urisToShare(viewModel.selectedItems(adapter.getSelectedItems()))
        val intent = Intent()
        intent.type = "image/jpg"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        when (uris.size) {
            0 -> {
                actionMode?.finish()
                return
            }
            1 -> {
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_STREAM, uris[0])
            }
            else -> {
                intent.action = Intent.ACTION_SEND_MULTIPLE
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }

        startActivity(Intent.createChooser(intent, resources.getString(R.string.shareinsult)))
        actionMode?.finish()
    }

    private fun untag() {
        if (!swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = true
        doAsync {
            viewModel.untag(viewModel.selectedItems(adapter.getSelectedItems()))
            uiThread {
                adapter.removeMultiple(adapter.getSelectedItems())
                actionMode?.finish()
                swipeRefreshCollection.isRefreshing = false
            }
        }
    }

    // Callable from trash to restore items
    private fun restore() {
        swipeRefreshCollection.isRefreshing = true
        doAsync {
            viewModel.restoreItems(viewModel.selectedItems(adapter.getSelectedItems()))

            uiThread {
                actionMode?.finish()
                viewModel.refreshItems()
            }
        }
    }

    // Starts dialog which asks for confirmation
    // Callback method deleteOk() does the actual deletion job
    private fun delete() {
        val deletionItems = viewModel.selectedItems(adapter.getSelectedItems())
        // if it might take some time, inform the user
        if (deletionItems.count() > 25) {
            Toast.makeText(activity, getString(R.string.TrashingStarted), Toast.LENGTH_SHORT).show()
        }

        adapter.removeMultiple(adapter.getSelectedItems())
        doAsync {
            val id = viewModel.trashItems(deletionItems)
            uiThread {
                val mySnackbar = Snackbar.make(collectionParentCoordinator, R.string.DeleteSnackbarSingleInfo, Snackbar.LENGTH_LONG)
                mySnackbar.setAction(R.string.DeleteSnackbarUndo) {
                    swipeRefreshCollection.isRefreshing = true
                    doAsync {
                        viewModel.undoTrashing(id)
                        uiThread {
                            viewModel.refreshItems()
                        }
                    }
                }
                mySnackbar.show()
            }
        }
        actionMode?.finish()
    }

    // functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Starts tag dialog
    // Callback method colorOk does the actual work
    private fun tag() {
        val std = TagDialogFragment()
        std.setTags(viewModel.tags)
        std.show(childFragmentManager, "tagdialog")
    }

    override fun onItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
        } else {
            startActivityForResult(intentFor<ImageSlideActivity>(
                    EXTRA_COLLECTIONID to cid,
                    EXTRA_ITEM_INDEX to position,
                    EXTRA_STARTING_POINT to STARTED_FROM_ACTIVITY), IMAGE_SLIDE_ACTIVITY_REQUEST_CODE)
        }
    }

    override fun onItemLongClicked(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        toggleSelection(position)
        return true
    }

    private fun toggleSelection(position: Int) {
        adapter.toggleSelection(position)
        val count = adapter.selectedItemCount

        if (count == 0) {
            actionMode?.finish()
        } else {
            showSelectionCountInActionMode(count)
            actionMode?.invalidate()
        }
    }

    private fun showSelectionCountInActionMode(count: Int) {
        actionMode?.title = resources.getString(R.string.collectionSelection) + " " + count.toString() + " / " + adapter.itemCount.toString()
    }

    // Dialog Callbacks

    override fun colorCancel() {}
    override fun colorOk(color: Int) {
        doAsync {
            viewModel.colorizeCollection(color)
            uiThread {
                val toast = Toast.makeText(activity, "Set color to " + color.toString(), Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }

    override fun tagCancel() {}
    override fun tagOk(tag: String) {
        val selection = viewModel.selectedItems(adapter.getSelectedItems())
        doAsync {
            viewModel.tagItems(selection, tag)
        }
        actionMode?.finish()
    }

    private inner class ActionModeCallback : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (this@CollectionFragment.cid == getString(R.string.trashName)) {
                mode.menuInflater.inflate(R.menu.menu_multiimageselected_trash, menu) // TODO create menu for selction mode
                return true
            }
            mode.menuInflater.inflate(R.menu.menu_multiimageselected, menu) // TODO create menu for selction mode
            if (viewModel.collectionType == TYPE_TAG && viewModel.collectionId != getString(R.string.timelineName)) {
                menu.findItem(R.id.multiimageselection_menu_untag)?.isVisible = true
            }
            collection_shareFloatingActionButton.visibility = View.VISIBLE // TODO - better way to make it visible? A little animated?
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.multiimageselection_menu_share -> {
                    share()
                    return true
                }
                R.id.multiimageselection_menu_untag -> {
                    untag()
                    return true
                }
                R.id.multiimageselection_menu_delete -> {
                    delete()
                    return true
                }
                R.id.trash_multiimageselection_menuRestore -> {
                    restore()
                    return true
                }

                else -> return false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            adapter.clearSelection()
            collection_shareFloatingActionButton.visibility = View.INVISIBLE // TODO - better way to make it invisible? A little animated?
            actionMode = null
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param cid Id for collection to laod.
         * @return A new instance of fragment CollectionFragment.
         */
        @JvmStatic
        fun newInstance(cid: String) =
                CollectionFragment().apply {
                    arguments = Bundle().apply {
                        putString(CID, cid)
                    }
                }
    }
}
