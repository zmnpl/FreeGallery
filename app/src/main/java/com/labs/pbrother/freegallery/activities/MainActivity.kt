package com.labs.pbrother.freegallery.activities

import android.Manifest
import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
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
import com.labs.pbrother.freegallery.controller.MyService
import com.labs.pbrother.freegallery.dialogs.ColorizeDialogFragment
import com.labs.pbrother.freegallery.settings.SettingsHelper
import com.labs.pbrother.freegallery.uiother.ItemOffsetDecoration
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.*

class MainActivity : AppCompatActivity(), OverviewRecyclerViewAdapter.ViewHolder.ClickListener, DrawerTagListAdapter.ViewHolder.ClickListener, ColorizeDialogFragment.ColorDialogListener {

    private var serviceBound = false
    private var permissionsGood = false
    private lateinit var settings: SettingsHelper
    private lateinit var service: MyService
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyService.LocalBinder
            this@MainActivity.service = binder.service
            serviceBound = true
            refresh()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
        }
    }

    private lateinit var overviewItems: ArrayList<CollectionItem>
    private lateinit var drawerItems: ArrayList<CollectionItem>
    private lateinit var timeline: CollectionItem
    private lateinit var trash: CollectionItem
    private var actionModeCollectionItems = ArrayList<CollectionItem>()

    // ui stuff
    private var colCount = 2
    private var onTablet = false
    private val actionModeCallback = ActionModeCallback()
    private var actionMode: ActionMode? = null
    private lateinit var adapter: OverviewRecyclerViewAdapter
    private lateinit var drawerResult: Drawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = SettingsHelper(applicationContext)
        setTheme(settings.theme)

        setContentView(R.layout.activity_main)

        //main_toolbar.setPadding(0, getStatusBarHeight(this), 0, 0)
        setSupportActionBar(main_toolbar)

        if (tabletMain != null) {
            onTablet = true
        }

        overviewRecycler.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@MainActivity, colCount)
            addItemDecoration(ItemOffsetDecoration(this@MainActivity, R.dimen.collection_picture_padding, colCount))
        }

        swipeRefreshMain.setOnRefreshListener {
            buildUiSafe()
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

    // User Interface Building
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // checks for permissions, service boundary and data status
    // if all good -> populate ui
    // if not, service probably needs to be connected
    private fun buildUiSafe() {
        swipeRefreshMain.isRefreshing = true

        if (permissionsGood) {
            if(serviceBound) {
                refresh()
                return
            }

            val intent = Intent(this, MyService::class.java)
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun refresh() {
        doAsync {
            overviewItems = this@MainActivity.service.overviewItems
            drawerItems = this@MainActivity.service.drawerItems
            timeline = this@MainActivity.service.timeline
            trash = this@MainActivity.service.trash

            uiThread {
                populateUi()
            }
        }
    }

    private fun populateUi() {
        adapter = OverviewRecyclerViewAdapter(this@MainActivity, this@MainActivity, overviewItems, service)
        adapter.setHasStableIds(true)
        overviewRecycler.adapter = adapter
        val tagLetter = getString(R.string.tagLetter)

        makeDrawer()
        drawerItems.forEach {
            this@MainActivity
                    .drawerResult
                    .addItem(primaryDrawerItemFromItem(applicationContext, it, tagLetter)
                            .withOnDrawerItemClickListener { view, position, drawerItem ->
                                startActivityForResult(
                                        intentFor<CollectionActivity>(
                                                EXTRA_COLLECTION_INDEX to position,
                                                EXTRA_COLLECTIONID to it.id),
                                        COLLECTION_ACTIVITY)
                                false
                            })
        }

        swipeRefreshMain.isRefreshing = false
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
        if (!serviceBound) {
            requestPermissions()
            buildUiSafe()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(mConnection)
            serviceBound = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == COLLECTION_ACTIVITY && resultCode == CollectionActivity.DATA_CHANGED) buildUiSafe()
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
            actionModeCollectionItems = ArrayList()
            for (i in adapter.getSelectedItems()) {
                actionModeCollectionItems.add(overviewItems[i])
            }

            when (item.itemId) {
                R.id.overviewselection_menu_hidegroup -> {
                    hide()
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
        for (item in actionModeCollectionItems) {
            service.colorizeCollection(item, color)
            adapter.notifyDataSetChanged()
        }
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
