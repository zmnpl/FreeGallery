package com.labs.pbrother.freegallery.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v4.app.NavUtils
import android.support.v4.content.FileProvider
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
import com.labs.pbrother.freegallery.controller.*
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.dialogs.TagDialogFragment
import com.labs.pbrother.freegallery.settings.DeviceConfiguration
import com.labs.pbrother.freegallery.settings.SettingsHelper
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.*
import java.io.File

class CollectionActivity : AppCompatActivity(), CollectionRecyclerViewAdapter.ViewHolder.ClickListener, TagDialogFragment.TagDialogListener, DrawerTagListAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener {

    // wiring
    private var serviceBound = false
    private lateinit var settings: SettingsHelper
    private lateinit var service: MyService
    private lateinit var mConnection: ServiceConnection
    // data
    private lateinit var collectionId: String
    private lateinit var collectionItem: CollectionItem
    private lateinit var items: ArrayList<Item>
    private lateinit var drawerItems: ArrayList<CollectionItem>
    // ui
    private var colCount = 4
    private val actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private lateinit var adapter: CollectionRecyclerViewAdapter
    private lateinit var drawerResult: Drawer
    //private var drawerToggle: ActionBarDrawerToggle? = null
    private lateinit var drawerAdapter: DrawerTagListAdapter
    private var onTablet = false
    // helper
    private var dataChanged = false
    private var sortOrder = SORT_ITEMS_DESC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // helper for settings
        settings = SettingsHelper(applicationContext)
        setTheme(settings.theme)

        setContentView(R.layout.activity_collection)

        if (tabletCollection != null) {
            onTablet = true
        }

        // recycler list
        colCount = columns
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
        collection_shareFloatingActionButton.image = tagSymbol(applicationContext)

        // swipe fullRefresh
        swipeRefreshCollection.setOnRefreshListener {
            buildUiSafe()
        }

        val input = intent
        collectionId = input.getStringExtra(EXTRA_COLLECTIONID)
    }

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

            if (onTablet) {
                //sectionHeader(getString(R.string.drawer_tagsection)) { }
            }

            footer {
                primaryItem(getString(R.string.menu_settings)) {
                    icon = R.drawable.ic_settings_white_24dp
                }.withOnDrawerItemClickListener { view, position, drawerItem ->
                    this@CollectionActivity.startActivity<SettingsActivity>()
                    false
                }
            }
        }
        if (!settings.hideDrawerHeader()) drawerResult.header?.drawerTopArea?.backgroundColor = settings.primaryColor

        if (onTablet) {
            drawerResult.slider.elevation = (-16).toFloat()
            nav_tablet_collection?.addView(drawerResult.slider)
        }

        // make items
        val tagLetter = getString(R.string.tagLetter)
        drawerItems.forEach {
            this@CollectionActivity
                    .drawerResult
                    .addItem(primaryDrawerItemFromItem(applicationContext, it, tagLetter)
                            .withOnDrawerItemClickListener { view, position, drawerItem ->
                                if (it.id != collectionId) {
                                    if (!swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = true
                                    collectionId = it.id
                                    refresh()
                                }
                                false
                            })
        }
    }

    private val columns: Int
        get() = if (DeviceConfiguration.instance.getRotation(this@CollectionActivity) === DeviceConfiguration.PORTRAIT || DeviceConfiguration.instance.getRotation(this@CollectionActivity) === DeviceConfiguration.REVERSE_PORTRAIT) {
            settings.columnsInPortrait
        } else {
            (settings.columnsInPortrait * 1.5).toInt()
        }

    // User Interface Building
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // checks service boundary and data status
    // arrogantly not checking for permissions on sub screens ^^
    // if all good -> populate ui
    // if not, service probably needs to be connected

    private fun buildUiSafe() {
        if (!swipeRefreshCollection.isRefreshing) swipeRefreshCollection.isRefreshing = true

        mConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val binder = service as MyService.LocalBinder
                this@CollectionActivity.service = binder.service
                serviceBound = true
                fullRefresh()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                serviceBound = false
            }
        }

        val intent = Intent(this, MyService::class.java)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    private fun fullRefresh() {
        doAsync {
            collectionItem = service.cachedCollectionItem(collectionId)
            items = service.itemsForCollection(collectionItem, sortOrder)
            drawerItems = this@CollectionActivity.service.drawerItems

            uiThread {
                populateUi()
                makeDrawer()
                swipeRefreshCollection.isRefreshing = false
            }
        }
    }

    private fun refresh() {
        doAsync {
            collectionItem = service.cachedCollectionItem(collectionId)
            items = service.itemsForCollection(collectionItem, sortOrder)
            drawerItems = this@CollectionActivity.service.drawerItems

            uiThread {
                populateUi()
                swipeRefreshCollection.isRefreshing = false
            }
        }
    }


    private fun populateUi() {
        supportActionBar?.title = collectionItem.displayNameDetail
        // Recycler
        adapter = CollectionRecyclerViewAdapter(this@CollectionActivity, this@CollectionActivity, items, service)
        collection_rclPictureCollection.adapter = adapter
        // Toolbar
        if (settings.colorizeTitlebar()) {
            main_toolbar.setBackgroundColor(collectionItem.color)
        }
    }

    private fun applyZoom(zoom: Int) {
        colCount += zoom
        if (colCount < 1) colCount = 1
        settings.columnsInPortrait = colCount
        collection_rclPictureCollection.layoutManager = GridLayoutManager(this@CollectionActivity, colCount)
    }

    // Lifecycle
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onResume() {
        super.onResume()
        if (!serviceBound) {
            buildUiSafe()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == IMAGE_SLIDE_ACTIVITY) {
            if (resultCode == DELETED_SMTH) {
                buildUiSafe()
            }
        }
    }

    override fun onDestroy() {
        if (serviceBound) {
            unbindService(mConnection)
            serviceBound = false
        }

        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // change columns on orientation change...
        (collection_rclPictureCollection.layoutManager as GridLayoutManager).spanCount = columns
    }

    override fun onBackPressed() {
        val intent = Intent()
        if (dataChanged) {
            setResult(DATA_CHANGED, intent)
        } else {
            setResult(-1, intent)
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater

        if (collectionId == getString(R.string.trashName)) {
            inflater.inflate(R.menu.menu_collection_trash, menu)
        } else {
            inflater.inflate(R.menu.menu_collection, menu)
            if (service.cachedCollectionItem(collectionId).type == TYPE_TAG) {
                val deleteTagMenuItem = menu.findItem(R.id.menu_deleteTag)
                deleteTagMenuItem?.isVisible = true
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
                if (service.deleteTag(collectionItem.id)) {
                    setResult(DATA_CHANGED, Intent())
                    finish()
                }
                return true
            }
            R.id.menu_refresh -> {
                swipeRefreshCollection.isRefreshing = true
                buildUiSafe()
                return true
            }
            R.id.menu_colorize -> {
                colorize()
                return true
            }
            R.id.menu_trash_emptyTrash -> {
                dataChanged = true
                val builder = AlertDialog.Builder(this)
                builder.setMessage(R.string.EmtyTrashQuestion)
                builder.setPositiveButton(R.string.EmtpyTrashOk) { dialog, id ->
                    doAsync {
                        service.emptyTrash()
                        uiThread {
                            //fullRefresh()
                            setResult(DATA_CHANGED, Intent())
                            finish()
                        }
                    }
                }
                builder.setNegativeButton(R.string.EmptyTrashCancel) { dialog, id ->
                    // user cancel
                }

                val dialog = builder.create()
                dialog.show()

                return true
            }
            R.id.menu_collectionSortAsc -> {
                sortOrder = SORT_ITEMS_ASC
                buildUiSafe()
                return true
            }
            R.id.menu_collectionSortDesc -> {
                sortOrder = SORT_ITEMS_DESC
                buildUiSafe()
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

    // Functionality

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.type = "image/jpg"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        val uris = ArrayList<Uri>()

        for (i in adapter.getSelectedItems()) {
            val (_, path) = items[i]
            uris.add(FileProvider.getUriForFile(this, packageName + ".provider", File(path)))
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        startActivity(Intent.createChooser(intent, resources.getString(R.string.shareinsult)))

        actionMode?.finish()
    }

    private fun restore() {
        swipeRefreshCollection.isRefreshing = true
        val restoreList = ArrayList<Item>()
        doAsync {
            for (i in adapter.getSelectedItems()) {
                restoreList.add(items[i])
            }
            service.restore(restoreList)

            uiThread {
                actionMode?.finish()
                setResult(DATA_CHANGED, Intent())
                fullRefresh()
            }
        }
    }

    // Starts tag dialog
    // Callback method colorOk does the actual work
    private fun tag() {
        val std = TagDialogFragment()
        std.setTags(service.tags())
        std.show(this.fragmentManager, "tagdialog")
    }

    // Starts dialog which asks for confirmation
    // Callback method deleteOk() does the actual deletion job
    private fun delete() {
        dataChanged = true
        val deletionItems = ArrayList<Item>()
        for (i in adapter.getSelectedItems()) {
            deletionItems.add(items[i])
        }

        // remove items from ui
        adapter.removeMultiple(adapter.getSelectedItems())
        var id = 0
        doAsync {
            id = service.trashItems(deletionItems)
            print("foo")
            uiThread {
                val mySnackbar = Snackbar.make(collectionParentCoordinator, R.string.DeleteSnackbarSingleInfo, Snackbar.LENGTH_LONG)
                mySnackbar.setAction(R.string.DeleteSnackbarUndo) {
                    swipeRefreshCollection.isRefreshing = true
                    doAsync {
                        service.undoTrashing(id)
                        uiThread {
                            buildUiSafe()
                        }
                    }
                }
                mySnackbar.show()
            }
        }

        actionMode?.finish()
    }

    private fun colorize() {
        ColorizeDialogFragment().show(this.fragmentManager, "colorizedialog")
    }

    // Handling user actions

    override fun onItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
        } else {
            startActivityForResult(intentFor<ImageSlideActivity>(
                    EXTRA_COLLECTIONID to collectionId,
                    EXTRA_ITEM_INDEX to position,
                    EXTRA_STARTING_POINT to STARTED_FROM_ACTIVITY), IMAGE_SLIDE_ACTIVITY)
        }
    }

    override fun onItemLongClicked(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
        toggleSelection(position)

        return true
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
            if (!onTablet) drawerCollection.closeDrawers()
            collectionId = drawerAdapter.getItemStringId(position)
            buildUiSafe()
        }
    }

    override fun onDrawerItemLongClicked(position: Int): Boolean {
        return false
    }

    // Callbacks
    override fun tagCancel() {}

    override fun tagOk(tag: String) {
        dataChanged = true
        for (i in adapter.getSelectedItems()) {
            service.tagItem(items[i], tag)
        }
        actionMode?.finish()
    }

    override fun colorCancel() {}
    override fun colorOk(color: Int) {
        service.colorizeCollection(collectionItem, color)
        if (settings.colorizeTitlebar()) {
            main_toolbar.setBackgroundColor(collectionItem.color)
        }
        val toast = Toast.makeText(this, "Set color to " + color.toString(), Toast.LENGTH_LONG)
        toast.show()
    }

    companion object {
        private val IMAGE_SLIDE_ACTIVITY = 1
        val DATA_CHANGED = 1338
    }
}
