package com.labs.pbrother.freegallery.fragments

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.GridLayoutManager
import android.view.*
import android.widget.Toast
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.*
import com.labs.pbrother.freegallery.adapters.CollectionRecyclerViewAdapter
import com.labs.pbrother.freegallery.app
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.controller.TYPE_TAG
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.dialogs.TagDialogFragment
import com.labs.pbrother.freegallery.extension.PORTRAIT
import com.labs.pbrother.freegallery.extension.REVERSE_PORTRAIT
import com.labs.pbrother.freegallery.extension.getRotation
import com.labs.pbrother.freegallery.extension.tagSymbol
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.labs.pbrother.freegallery.viewModels.CollectionViewModel
import kotlinx.android.synthetic.main.fragment_collection.*
import kotlinx.android.synthetic.main.fragment_collection.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.image
import org.jetbrains.anko.support.v4.intentFor
import org.jetbrains.anko.uiThread

private const val CID = "collectionId"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CollectionFragment.OnCollectionFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [CollectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class CollectionFragment : Fragment(), CollectionRecyclerViewAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener, TagDialogFragment.TagDialogListener {

    interface OnCollectionFragmentInteractionListener {
        fun onCollectionColorChange(color: Int)
        fun setToolbarTitle(title: String)
    }

    // parameters
    private lateinit var cid: String

    // other
    private lateinit var viewModel: CollectionViewModel
    private var listener: OnCollectionFragmentInteractionListener? = null
    private var dataChanged = false

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
            cid = it.getString(CID) ?: ""
        }

        // bind to viewmodel
        viewModel = ViewModelProviders.of(this).get(CollectionViewModel::class.java)
        viewModel.refreshCollection(cid)
        viewModel.refreshItems()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        if (rv == null) {
            rv = inflater.inflate(R.layout.fragment_collection, container, false)
            // recycler list
            val colCount = columns
            rv?.collection_rclPictureCollection?.apply {
                addItemDecoration(ItemOffsetDecoration(activity as Context, R.dimen.collection_picture_padding, colCount))
                setHasFixedSize(true)
                layoutManager = GridLayoutManager(activity as Context, colCount)
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

        swipeRefreshCollection.isRefreshing = true
        viewModel.items.observe(this, Observer { items ->
            if (null != items) {
                adapter = CollectionRecyclerViewAdapter(this@CollectionFragment, activity as Context, items, Provider(app))
                collection_rclPictureCollection.adapter = adapter
                if (swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = false
            }
        })

        viewModel.collectionItem.observe(this, Observer { collectionItem ->
            if (null != collectionItem) listener?.setToolbarTitle(collectionItem.displayNameDetail)
        })

        viewModel.liveColor.observe(this, Observer { color ->
            if (null != color) listener?.onCollectionColorChange(color)
        })

        collection_shareFloatingActionButton.setOnClickListener { tag() }
        collection_shareFloatingActionButton.image = activity?.tagSymbol()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCollectionFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnMainFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        if (cid == getString(R.string.trashName)) {
            inflater?.inflate(R.menu.menu_collection_trash, menu)
        } else {
            inflater?.inflate(R.menu.menu_collection, menu)
            if (viewModel.collectionType == TYPE_TAG && cid != getString(R.string.timelineName)) {
                menu?.findItem(R.id.menu_deleteTag)?.isVisible = true
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(activity as Activity) // TODO - does that work?
                return true
            }
            R.id.menu_deleteTag -> {
                deleteTag()
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
                    dataChanged = true
                    informCallerOfChange()
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
        if (!(swipeRefreshCollection?.isRefreshing
                        ?: true)) swipeRefreshCollection.isRefreshing = true
        doAsync {
            viewModel.refreshCollection(cid)
            viewModel.refreshItems()
            viewModel.refreshDrawerItems()
            uiThread {
                swipeRefreshCollection?.isRefreshing = false
            }
        }
    }

    // TODO - solve differently from fragment
    private fun informCallerOfChange() {
        //resultIntent.putExtra(SHOULD_RELOAD, dataChanged)
    }


    private fun emptyTrash() {
        dataChanged = true
        informCallerOfChange()
        val builder = AlertDialog.Builder(activity as Context)
        builder.setMessage(R.string.EmtyTrashQuestion)
        builder.setPositiveButton(R.string.EmtpyTrashOk) { dialog, id ->
            doAsync {
                viewModel.emptyTrash()
                uiThread {
                    dataChanged = true
                    informCallerOfChange()
                    //finish()
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
        collection_rclPictureCollection.layoutManager = GridLayoutManager(activity, colCount)
    }

    private fun deleteTag() {
        if (viewModel.deleteTag()) {
            dataChanged = true
            informCallerOfChange()
            //finish()
        }
    }

    private fun selectAll() {
        if (null == actionMode) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(actionModeCallback)
        }
        adapter.clearSelection()
        var i = 0;
        while (i < adapter.itemCount) {
            adapter.toggleSelection(i)
            i++
        }
    }

    private fun share() {
        val uris = viewModel.urisToShare(viewModel.selectedItems(adapter.getSelectedItems()))

        val intent = Intent()
        intent.type = "image/jpg"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        // some apps cannot react to ACTION_SEND_MULTIPLE
        // therefore, if only one is selected for sharing, use ACTION_SEND instead
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
        informCallerOfChange()
        swipeRefreshCollection.isRefreshing = true
        doAsync {
            viewModel.restoreItems(viewModel.selectedItems(adapter.getSelectedItems()))

            uiThread {
                actionMode?.finish()
                dataChanged = true
                informCallerOfChange()
                viewModel.refreshItems()
            }
        }
    }

    // Starts dialog which asks for confirmation
    // Callback method deleteOk() does the actual deletion job
    private fun delete() {
        dataChanged = true
        informCallerOfChange()

        val deletionItems = viewModel.selectedItems(adapter.getSelectedItems())
        if (deletionItems.count() > 25) {
            Toast.makeText(activity, getString(R.string.TrashingStarted), Toast.LENGTH_SHORT).show()
        }

        // remove items from ui
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
            actionMode?.title = resources.getString(R.string.collectionSelection) + " " + count.toString() + " / " + adapter.itemCount.toString()
            actionMode?.invalidate()
        }
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
        doAsync {
            viewModel.tagItems(viewModel.selectedItems(adapter.getSelectedItems()), tag)
        }
        dataChanged = true
        informCallerOfChange()
        actionMode?.finish()
    }

    // helper
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private val columns: Int
        get() = if (activity?.getRotation() === PORTRAIT || activity?.getRotation() === REVERSE_PORTRAIT) {
            prefs.columnsInPortrait
        } else {
            (prefs.columnsInPortrait * 1.5).toInt()
        }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
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


}
