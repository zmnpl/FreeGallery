package com.labs.pbrother.freegallery.activities

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.NavUtils
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.adapters.CollectionRecyclerViewAdapter
import com.labs.pbrother.freegallery.adapters.DrawerTagListAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.controller.TYPE_TAG
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.dialogs.TagDialogFragment
import com.labs.pbrother.freegallery.extension.primaryDrawerItemFromItem
import com.labs.pbrother.freegallery.extension.tagSymbol
import com.labs.pbrother.freegallery.settings.DeviceConfiguration
import com.labs.pbrother.freegallery.settings.SettingsHelper
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.*

class CollectionActivity : AppCompatActivity(), CollectionRecyclerViewAdapter.ViewHolder.ClickListener, TagDialogFragment.TagDialogListener, DrawerTagListAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener {

    // wiring
    private lateinit var settings: SettingsHelper
    private lateinit var viewModel: CollectionActivityViewModel

    // instance sates
    private val CID: String = "cid"
    private val DATA_CHANGED: String = "changed"

    // parameters
    private lateinit var collectionId: String

    // helper
    private var dataChanged = false
    private var onTablet = false

    // ~ ui
    private val actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private lateinit var adapter: CollectionRecyclerViewAdapter
    private lateinit var drawerResult: Drawer
    private lateinit var drawerAdapter: DrawerTagListAdapter

    // result for caller
    private val resultIntent = Intent()

    // region oncreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_OK, resultIntent)

        if (tabletCollection != null) onTablet = true

        collectionId = intent.getStringExtra(EXTRA_COLLECTIONID)
        savedInstanceState?.apply {
            collectionId = savedInstanceState.getString(CID)
            dataChanged = savedInstanceState.getBoolean(DATA_CHANGED)
        }

        // helper for settings
        settings = SettingsHelper(applicationContext)
        setTheme(settings.theme)

        setContentView(R.layout.activity_collection)

        // recycler list
        val colCount = columns
        collection_rclPictureCollection.apply {
            addItemDecoration(ItemOffsetDecoration(this@CollectionActivity, R.dimen.collection_picture_padding, colCount))
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@CollectionActivity, colCount)
            isSaveEnabled = true
        }

        // set toolbar
        setSupportActionBar(main_toolbar)

        // provide up navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // floating action button
        collection_shareFloatingActionButton.setOnClickListener { tag() }
        collection_shareFloatingActionButton.image = tagSymbol()

        // swipe fullRefresh
        swipeRefreshCollection.setOnRefreshListener {
            refresh(true, true, true, false)
        }

        makeDrawer()
        bindViewModel()
        refresh(true, true, true, false)
    }
    // endregion

    // region lyfecycle rest
    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.apply {
            putString(CID, collectionId)
            putBoolean(DATA_CHANGED, dataChanged)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == IMAGE_SLIDE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK &&
                (data.getBooleanExtra(DELETION, false) || data.getBooleanExtra(CROP_SAVED, false))) {
            refresh(false, false, true, false)
            informCallerOfChange()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // change columns on orientation change...
        (collection_rclPictureCollection.layoutManager as GridLayoutManager).spanCount = columns
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK, resultIntent)
        super.onBackPressed()
    }
    // endregion

    //region build drawer
    private fun makeDrawer() {
        drawerResult = drawer {
            if (onTablet) buildViewOnly = true

            toolbar = main_toolbar

            displayBelowStatusBar = true
            translucentStatusBar = false

            actionBarDrawerToggleEnabled = true
            actionBarDrawerToggleAnimated = true

            drawerLayoutRes = R.layout.material_drawer

            if (!settings.hideDrawerHeader()) headerViewRes = R.layout.drawer_header

            footer {
                primaryItem(getString(R.string.menu_settings)) {
                    icon = R.drawable.ic_settings_white_24dp
                }.withOnDrawerItemClickListener { view, position, drawerItem ->
                    val bar = drawerResult.getDrawerItem(viewModel.collectionId)
                    if (null != bar) drawerResult.setSelection(bar)
                    this@CollectionActivity.startActivity<SettingsActivity>()
                    false
                }
            }

            onSlide { drawerView, slideOffset ->
                /*when(window.statusBarColor) {
                    getColor(R.color.primary_dark) -> {
                        colorizeTitlebar(viewModel.liveColor.value!!)
                    }
                    else -> {
                        window.statusBarColor = settings.colorPrimaryDark
                    }
                }*/
            }
        }

        if (!settings.hideDrawerHeader()) drawerResult.header?.drawerTopArea?.backgroundColor = settings.primaryColor

        if (onTablet) {
            drawerResult.slider.elevation = (-16).toFloat()
            nav_tablet_collection?.addView(drawerResult.slider)
        }
    }

    private fun addDrawerItems(drawerItems: ArrayList<CollectionItem>) {
        drawerResult.removeAllItems()

        drawerItems.forEach {
            val itm = primaryDrawerItemFromItem(it, getString(R.string.tagLetter))
                    .withOnDrawerItemClickListener { view, position, drawerItem ->
                        if (it.id != collectionId) {
                            if (!swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = true
                            collectionId = it.id
                            refresh(true, false, true, false)
                            invalidateOptionsMenu()
                        }
                        false
                    }
            this@CollectionActivity.drawerResult.addItem(itm)

            if (it.id == collectionId) this@CollectionActivity.drawerResult.setSelection(itm)
        }
    }
    // endregion

    // region ui behaviour
    private fun bindViewModel() {
        viewModel = ViewModelProviders.of(this).get(CollectionActivityViewModel::class.java)

        viewModel.drawerItems.observe(this, Observer { drawerItems ->
            if (null != drawerItems) addDrawerItems(drawerItems)
        })

        viewModel.items.observe(this, Observer { items ->
            if (null != items) populateAdapter(items)
        })

        viewModel.collectionItem.observe(this, Observer { collectionItem ->
            if (null != collectionItem) supportActionBar?.title = collectionItem.displayNameDetail
        })

        viewModel.liveColor.observe(this, Observer { color ->
            if (null != color) changeColor(color)
        })
    }

    private fun populateAdapter(items: ArrayList<Item>) {
        adapter = CollectionRecyclerViewAdapter(this@CollectionActivity, this@CollectionActivity, items, Provider(application))
        collection_rclPictureCollection.adapter = adapter
    }

    private fun changeColor(color: Int) {
        setTheme(settings.theme)
        if (settings.colorizeTitlebar()) colorizeTitlebar(color)
        // refresh items in drawer, to make color change for tag collection visible
        if (viewModel.collectionType == TYPE_TAG) {
            doAsync {
                viewModel.refreshDrawerItems()
            }
        }
    }

    private fun colorizeTitlebar(color: Int) {
        main_toolbar.setBackgroundColor(color)

        if (color != settings.defaultCollectionColor) {
            window.statusBarColor = adjustColorAlpha(color, 0.8f)
        } else {
            //window.statusBarColor = adjustColorAlpha(settings.colorPrimaryDark, 0.8f)
            window.statusBarColor = settings.colorPrimaryDark
        }
    }

    private val columns: Int
        get() = if (DeviceConfiguration.instance.getRotation(this@CollectionActivity) === DeviceConfiguration.PORTRAIT || DeviceConfiguration.instance.getRotation(this@CollectionActivity) === DeviceConfiguration.REVERSE_PORTRAIT) {
            settings.columnsInPortrait
        } else {
            (settings.columnsInPortrait * 1.5).toInt()
        }

    private fun refresh(collection: Boolean, drawer: Boolean, items: Boolean, cached: Boolean = false) {
        if (!swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = true
        doAsync {
            viewModel.refresh(collection, drawer, items, collectionId, cached)
            uiThread {
                swipeRefreshCollection.isRefreshing = false
            }
        }
    }
    // endregion

    //region menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater

        if (collectionId == getString(R.string.trashName)) {
            inflater.inflate(R.menu.menu_collection_trash, menu)
        } else {
            inflater.inflate(R.menu.menu_collection, menu)
            if (viewModel.collectionType == TYPE_TAG && collectionId != getString(R.string.timelineName)) {
                menu.findItem(R.id.menu_deleteTag)?.isVisible = true
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            R.id.menu_deleteTag -> {
                deleteTag()
                return true
            }
            R.id.menu_refresh -> {
                refresh(true, true, true, true)
                return true
            }
            R.id.menu_colorize -> {
                colorize()
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
    // endregion

    // Functionality

    private fun informCallerOfChange() {
        resultIntent.putExtra(SHOULD_RELOAD, dataChanged)
    }

    private fun deleteTag() {
        if (viewModel.deleteTag()) {
            dataChanged = true
            informCallerOfChange()
            finish()
        }
    }

    private fun emptyTrash() {
        dataChanged = true
        informCallerOfChange()
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.EmtyTrashQuestion)
        builder.setPositiveButton(R.string.EmtpyTrashOk) { dialog, id ->
            doAsync {
                viewModel.emptyTrash()
                uiThread {
                    dataChanged = true
                    informCallerOfChange()
                    finish()
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
        settings.columnsInPortrait = colCount
        collection_rclPictureCollection.layoutManager = GridLayoutManager(this@CollectionActivity, colCount)
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

    private fun restore() {
        informCallerOfChange()
        swipeRefreshCollection.isRefreshing = true
        doAsync {
            viewModel.restoreItems(viewModel.selectedItems(adapter.getSelectedItems()))

            uiThread {
                actionMode?.finish()
                dataChanged = true
                informCallerOfChange()
                refresh(true, false, true, false)
            }
        }
    }

    // Starts tag dialog
    // Callback method colorOk does the actual work
    private fun tag() {
        val std = TagDialogFragment()
        std.setTags(viewModel.tags)
        std.show(this.fragmentManager, "tagdialog")
    }

    // Starts dialog which asks for confirmation
    // Callback method deleteOk() does the actual deletion job
    private fun delete() {
        dataChanged = true
        informCallerOfChange()

        val deletionItems = viewModel.selectedItems(adapter.getSelectedItems())
        if (deletionItems.count() > 25) {
            Toast.makeText(this, getString(R.string.TrashingStarted), Toast.LENGTH_SHORT).show()
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
                            refresh(true, true, true, false)
                        }
                    }
                }
                mySnackbar.show()
            }
        }

        actionMode?.finish()
    }

    private fun colorize() {
        val foo = ColorizeDialogFragment()
        foo.show(this.fragmentManager, "colorizedialog")
    }

    val collectionColor: Int?
        get() = viewModel.liveColor.value

    // Handling user actions

    override fun onItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
        } else {
            startActivityForResult(intentFor<ImageSlideActivity>(
                    EXTRA_COLLECTIONID to collectionId,
                    EXTRA_ITEM_INDEX to position,
                    EXTRA_STARTING_POINT to STARTED_FROM_ACTIVITY), IMAGE_SLIDE_ACTIVITY_REQUEST_CODE)
        }
    }

    override fun onItemLongClicked(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
        toggleSelection(position)

        return true
    }

    private fun selectAll() {
        if (null == actionMode) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
        adapter.clearSelection()
        var i = 0;
        while (i < adapter.itemCount) {
            adapter.toggleSelection(i)
            i++
        }
    }

    // Toggle the selection state of an item.
    // If the item was the last one in the selection and is unselected, the selection is stopped.
    // Note that the selection must already be started (actionMode must not be null)
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

    private inner class ActionModeCallback : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (this@CollectionActivity.collectionId == getString(R.string.trashName)) {
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

    // clicks on item in navigation drawer
    override fun onDrawerItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
        } else {
            if (!onTablet) drawerCollection?.closeDrawers()
            collectionId = drawerAdapter.getItemStringId(position)
            //refresh(true)
        }
    }

    override fun onDrawerItemLongClicked(position: Int): Boolean {
        return false
    }

    // Callbacks
    override fun tagCancel() {}

    override fun tagOk(tag: String) {
        doAsync {
            viewModel.tagItems(viewModel.selectedItems(adapter.getSelectedItems()), tag)
        }
        dataChanged = true
        informCallerOfChange()
        actionMode?.finish()
    }

    override fun colorCancel() {}
    override fun colorOk(color: Int) {
        doAsync {
            viewModel.colorizeCollection(color)
            uiThread {
                val toast = Toast.makeText(this@CollectionActivity, "Set color to " + color.toString(), Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }

}
