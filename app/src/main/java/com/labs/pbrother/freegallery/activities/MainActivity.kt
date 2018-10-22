package com.labs.pbrother.freegallery.activities

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.adapters.DrawerTagListAdapter
import com.labs.pbrother.freegallery.adapters.OverviewRecyclerViewAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.controller.Provider
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.extension.openSAFTreeSelection
import com.labs.pbrother.freegallery.extension.primaryDrawerItemFromItem
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.*


class MainActivity : AppCompatActivity(), OverviewRecyclerViewAdapter.ViewHolder.ClickListener, DrawerTagListAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener {

    private lateinit var viewModel: MainActivityViewModel

    private var onTablet = false
    private var reloadPlz = false
    private var permissionsGood = false

    private val actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private var selection: List<Int>? = null
    private var actionModeCollectionItems = ArrayList<CollectionItem>()
    private lateinit var adapter: OverviewRecyclerViewAdapter
    private lateinit var drawerResult: Drawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reloadPlz = true
        if (tabletMain != null) onTablet = true

        setTheme(prefs.theme)

        setContentView(R.layout.activity_main)
        //main_toolbar.setPadding(0, getStatusBarHeight(this), 0, 0)
        setSupportActionBar(main_toolbar)
        //main_toolbar.backgroundColor = getColor(R.color.nerd_primary)

        overviewRecycler.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, prefs.mainColumnsInPortrait)
            addItemDecoration(ItemOffsetDecoration(this@MainActivity, R.dimen.collection_picture_padding, prefs.mainColumnsInPortrait))
        }

        bindViewModel()
        swipeRefreshMain.setOnRefreshListener { buildUiSafe() }
    }

    private fun populateAdapter(overviewItems: ArrayList<CollectionItem>?) {
        if (null != overviewItems) {
            adapter = OverviewRecyclerViewAdapter(this@MainActivity, this@MainActivity, overviewItems, Provider(application))
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

            if (!prefs.hideDrawerHeader) headerViewRes = R.layout.drawer_header

            if (onTablet) {
                //sectionHeader(getString(R.string.drawer_tagsection)) { }
            }

            footer {
                primaryItem(getString(R.string.menu_settings)) {
                    icon = R.drawable.ic_settings_white_24dp
                }.withOnDrawerItemClickListener { view, position, drawerItem ->
                    this@MainActivity.startActivity<SettingsActivity>()
                    drawerResult.setSelectionAtPosition(-1)
                    false
                }
            }
        }
        if (!prefs.hideDrawerHeader) drawerResult.header?.drawerTopArea?.backgroundColor = prefs.primaryColor

        if (onTablet) {
            nav_tablet.addView(drawerResult.slider)
        }
    }

    private fun addDrawerItems(drawerItems: ArrayList<CollectionItem>) {
        drawerItems.forEach {
            this@MainActivity
                    .drawerResult
                    .addItem(primaryDrawerItemFromItem(it, getString(R.string.tagLetter))
                            .withOnDrawerItemClickListener { view, position, drawerItem ->
                                startActivityForResult(
                                        intentFor<CollectionActivity>(
                                                EXTRA_COLLECTION_INDEX to position,
                                                EXTRA_COLLECTIONID to it.id),
                                        COLLECTION_ACTIVITY_REQUEST_CODE)
                                false
                            })
        }
    }

    private fun bindViewModel() {
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java!!)

        viewModel.overviewItems.observe(this, Observer { overviewItems ->
            populateAdapter(overviewItems)
        })

        viewModel.drawerItems.observe(this, Observer { drawerItems ->
            makeDrawer()
            if (null != drawerItems) addDrawerItems(drawerItems)
        })
    }

    // User Interface Building
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // checks for permissions, service boundary and data status
    // if all good -> populate ui
    // if not, service probably needs to be connected
    private fun buildUiSafe() {
        if (permissionsGood) {
            refresh()
            reloadPlz = false
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

    private fun applyZoom(zoom: Int) {
        var cols = prefs.mainColumnsInPortrait
        cols += zoom
        if (cols < 1) cols = 1
        prefs.mainColumnsInPortrait = cols
        overviewRecycler.layoutManager = GridLayoutManager(this@MainActivity, cols)
    }

    // Lifecycle
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onResume() {
        super.onResume()
        requestPermissions()
        if (reloadPlz) buildUiSafe()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == COLLECTION_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data.getBooleanExtra(SHOULD_RELOAD, false)) buildUiSafe()

        // SD card uri selected
        if (requestCode === READ_REQUEST_CODE && resultCode === Activity.RESULT_OK) {
            var uri: Uri? = data?.getData()
            val takeFlags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            prefs.sdCardUri = uri.toString()
        }
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
                //openSAFTreeSelection()
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
            R.id.menu_takeSdCardPermission -> {
                openSAFTreeSelection()
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
                    COLLECTION_ACTIVITY_REQUEST_CODE)
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
            //        COLLECTION_ACTIVITY_REQUEST_CODE)
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

    //@TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissions() {
        val checkRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val checkWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (PackageManager.PERMISSION_GRANTED != checkRead || PackageManager.PERMISSION_GRANTED != checkWrite) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // TODO
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_READ_WRITE_STORAGE
                )
            }
        } else {
            permissionsGood = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_READ_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGood = true
                    buildUiSafe()
                } else {
                    Toast.makeText(application, getString(R.string.noReadPermissionToast), Toast.LENGTH_LONG)
                    finish()
                }
            }
        }
    }

}
