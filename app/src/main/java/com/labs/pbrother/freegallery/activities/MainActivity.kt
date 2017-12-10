package com.labs.pbrother.freegallery.activities

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.adapters.DrawerTagListAdapter
import com.labs.pbrother.freegallery.adapters.OverviewRecyclerViewAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Foo
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.settings.SettingsHelper
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.*


class MainActivity : AppCompatActivity(), OverviewRecyclerViewAdapter.ViewHolder.ClickListener, DrawerTagListAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener {

    private var permissionsGood = false
    private lateinit var settings: SettingsHelper
    private lateinit var viewModel: MainActivityViewModel
    private var selection: List<Int>? = null

    private var actionModeCollectionItems = ArrayList<CollectionItem>()

    // ui stuff
    private var colCount = 2
    private var onTablet = false
    private val actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private lateinit var adapter: OverviewRecyclerViewAdapter
    private lateinit var drawerResult: Drawer
    private var reloadPlz = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reloadPlz = true

        settings = SettingsHelper(applicationContext)
        setTheme(settings.theme)

        setContentView(R.layout.activity_main)

        //main_toolbar.setPadding(0, getStatusBarHeight(this), 0, 0)
        setSupportActionBar(main_toolbar)

        if (tabletMain != null) onTablet = true

        overviewRecycler.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, colCount)
            addItemDecoration(ItemOffsetDecoration(this@MainActivity, R.dimen.collection_picture_padding, colCount))
        }

        swipeRefreshMain.setOnRefreshListener { buildUiSafe() }

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java!!)

        viewModel.overviewItems.observe(this, Observer { overviewItems ->
            populateAdapter(overviewItems)
        })

        viewModel.drawerItems.observe(this, Observer { drawerItems ->
            makeDrawer()
            if (null != drawerItems) addDrawerItems(drawerItems)
        })
    }

    private fun populateAdapter(overviewItems: ArrayList<CollectionItem>?) {
        if (null != overviewItems) {
            adapter = OverviewRecyclerViewAdapter(this@MainActivity, this@MainActivity, overviewItems, Foo(application))
            adapter.setHasStableIds(true)
            overviewRecycler.adapter = adapter
        }
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
                    this@MainActivity.startActivity<SettingsActivity>()
                    false
                }
            }
        }
        if (!settings.hideDrawerHeader()) drawerResult.header?.drawerTopArea?.backgroundColor = settings.primaryColor

        if (onTablet) {
            nav_tablet.addView(drawerResult.slider)
        }
    }

    private fun addDrawerItems(drawerItems: ArrayList<CollectionItem>) {
        drawerItems.forEach {
            this@MainActivity
                    .drawerResult
                    .addItem(primaryDrawerItemFromItem(applicationContext, it, getString(R.string.tagLetter))
                            .withOnDrawerItemClickListener { view, position, drawerItem ->
                                startActivityForResult(
                                        intentFor<CollectionActivity>(
                                                EXTRA_COLLECTION_INDEX to position,
                                                EXTRA_COLLECTIONID to it.id),
                                        COLLECTION_ACTIVITY)
                                false
                            })
        }
    }

    // User Interface Building
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // checks for permissions, service boundary and data status
    // if all good -> populate ui
    // if not, service probably needs to be connected
    private fun buildUiSafe() {
        if (permissionsGood) refresh()
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

    private fun applyZoom(zoom: Int) {
        colCount += zoom
        if (colCount < 1) colCount = 1
        settings.mainColumnsInPortrait = colCount
        overviewRecycler.layoutManager = GridLayoutManager(this@MainActivity, colCount)
    }

    // Lifecycle
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onResume() {
        super.onResume()
        requestPermissions()
        if (reloadPlz) buildUiSafe()
        reloadPlz = false
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == COLLECTION_ACTIVITY && resultCode == Activity.RESULT_OK && data.getBooleanExtra(SHOULD_RELOAD, false)) buildUiSafe()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                swipeRefreshMain.isRefreshing = true
                buildUiSafe()
                return true
            }
            R.id.menu_settings -> {
                startActivity<SettingsActivity>()
                return true
            }
            R.id.menu_license -> {
                startActivity<AboutActivity>()
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

    // Click handler and action mode for multi selection
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // clicks on item in main view
    override fun onItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
        } else {
            startActivityForResult(
                    intentFor<CollectionActivity>(
                            EXTRA_ITEM_INDEX to position,
                            EXTRA_COLLECTIONID to adapter.getItemStringId(position)),
                    COLLECTION_ACTIVITY)
        }
    }

    override fun onItemLongClicked(position: Int): Boolean {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        }
        toggleSelection(position)

        return true
    }

    // clicks on item in navigation drawer
    override fun onDrawerItemClicked(position: Int) {
        if (actionMode != null) {
            toggleSelection(position)
        } else {
            //if (!onTablet) drawerLayoutMain.closeDrawers()
            //startActivityForResult(
            //        intentFor<CollectionActivity>("collectionIndex" to position, "collectionId" to drawerAdapter!!.getItemStringId(position)),
            //        COLLECTION_ACTIVITY)
        }
    }

    override fun onDrawerItemLongClicked(position: Int): Boolean {
        return false
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

    // Functionality

    private fun colorize() {
        ColorizeDialogFragment().show(this.fragmentManager, "colorizedialog")
    }

    private fun hide() {
        // TODO
    }

    // Callbacks

    override fun colorOk(color: Int) {
        viewModel.colorize(selection ?: ArrayList<Int>(), color)
        adapter.notifyDataSetChanged()
        selection = null
        actionModeCollectionItems.clear()
    }

    override fun colorCancel() {}

    // Permissions

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissions() {
        val check = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (PackageManager.PERMISSION_GRANTED != check) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                // TODO
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        SettingsHelper.PERMISSION_READ_STORAGE)
            }
        } else {
            permissionsGood = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == SettingsHelper.PERMISSION_READ_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay!
                permissionsGood = true
            } else {
                // permission denied, boo!
            }
            return
        }
    }

    companion object {
        private val COLLECTION_ACTIVITY = 0
    }
}
