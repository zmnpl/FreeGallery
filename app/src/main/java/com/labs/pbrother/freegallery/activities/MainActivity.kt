package com.labs.pbrother.freegallery.activities

import android.Manifest
import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import com.labs.pbrother.freegallery.*
import com.labs.pbrother.freegallery.adapters.DrawerTagListAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.extension.drawerHomeItem
import com.labs.pbrother.freegallery.extension.primaryDrawerItemFromItem
import com.labs.pbrother.freegallery.fragments.*
import com.labs.pbrother.freegallery.viewModels.MainActivityViewModel
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.uiThread


class MainActivity : AppCompatActivity(), DrawerTagListAdapter.ViewHolder.ClickListener {

    private lateinit var viewModel: MainActivityViewModel
    private var onTablet = false
    private var reloadPlz = false
    private var permissionsGood = false
    private lateinit var drawerResult: Drawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reloadPlz = true
        onTablet = main_layout_tablet != null

        setTheme(prefs.theme)
        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)
        //main_toolbar.setPadding(0, getStatusBarHeight(this), 0, 0)

        main_layout?.backgroundColor = prefs.colorPrimary
        main_layout_tablet?.backgroundColor = prefs.colorPrimary

        makeDrawer()
        bindViewModel()
    }

    override fun onBackPressed() {
        drawerResult.deselect()
        super.onBackPressed()
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

        addDrawerHomeItem()
        if (!prefs.hideDrawerHeader) drawerResult.header?.drawerTopArea?.backgroundColor = prefs.colorPrimary

        nav_tablet?.let {
            //sectionHeader(getString(R.string.drawer_tagsection)) { }
            val slider = drawerResult.slider
            (slider.parent as ViewGroup).removeView(slider)
            it.addView(slider)

            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            drawerResult.actionBarDrawerToggle.isDrawerIndicatorEnabled = false
        }
    }

    private fun bindViewModel() {
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        viewModel.drawerItems.observe(this, Observer { drawerItems ->
            if (null != drawerItems) addDrawerItems(drawerItems)
        })

        viewModel.toolbarColor.observe(this, Observer { color ->
            if (null != color) colorizeToolbar(color)
        })

        viewModel.toolbarText.observe(this, Observer { toolbarText ->
            if (null != toolbarText) setToolbarTitle(toolbarText)
        })
    }

    private fun addDrawerHomeItem() {
        drawerResult.addItem(drawerHomeItem()
                .withOnDrawerItemClickListener { view, position, drawerItem ->
                    navigateHome()
                    false
                })
    }

    private fun addDrawerItems(drawerItems: ArrayList<CollectionItem>) {
        drawerResult.removeAllItems()
        addDrawerHomeItem()
        drawerItems.forEach {
            this@MainActivity
                    .drawerResult
                    .addItem(primaryDrawerItemFromItem(it, getString(R.string.tagLetter))
                            .withOnDrawerItemClickListener { view, position, drawerItem ->
                                navigateToCollection(it.id)
                                false
                            })
        }
    }

    private fun navigateHome() {
        val currentFragment = nav_host_fragment.childFragmentManager.fragments[0]
        val navController = NavHostFragment.findNavController(nav_host_fragment)
        when (currentFragment) {
            is CollectionFragment -> {
                navController.navigate(CollectionFragmentDirections.actionGoToOverview())
            }
            is OverviewFragment -> {
                // Nothing to do here ... already there.
            }
            is AboutFragment -> {
                navController.navigate(AboutFragmentDirections.actionDestinationAboutToDestinationOverview())
            }
        }
    }

    private fun navigateToCollection(collectionId: String) {
        val currentFragment = nav_host_fragment.childFragmentManager.fragments[0]
        val navController = NavHostFragment.findNavController(nav_host_fragment)
        when (currentFragment) {
            is CollectionFragment -> {
                navController.navigate(CollectionFragmentDirections.actionCollectionFragmentSelf(collectionId))
            }
            is OverviewFragment -> {
                navController.navigate(OverviewFragmentDirections.actionOverviewFragmentToCollectionFragment(collectionId))
            }
            is AboutFragment -> {
                navController.navigate(AboutFragmentDirections.actionDestinationAboutToDestinationCollection(collectionId))
            }
        }
        //val args = Bundle()
        //args.putString("collectionId", collectionId)
    }

    // User Interface BuildingOnCollectionFragmentInteractionListener
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun buildUiSafe() {
        if (permissionsGood) {
            refresh()
            reloadPlz = false
        }
    }

    private fun refresh() {
        //swipeRefreshMain.isRefreshing = true
        doAsync {
            viewModel.refreshDrawerItems()
            viewModel.refreshOverviewItems()
            uiThread {
                //swipeRefreshMain.isRefreshing = false
            }
        }
    }

    // Lifecycle
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onResume() {
        super.onResume()
        requestPermissions()
        if (reloadPlz) buildUiSafe()
        main_toolbar.popupTheme = prefs.popupTheme
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        data?.let { dt ->
            if (requestCode == COLLECTION_ACTIVITY_REQUEST_CODE
                    && resultCode == Activity.RESULT_OK
                    && dt.getBooleanExtra(SHOULD_RELOAD, false)) buildUiSafe()

            // SD card uri selected
            if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                dt.data?.let {
                    val uri = it
                    val takeFlags = intent.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    prefs.sdCardUri = uri.toString()
                }
            }
        }
    }

    // Click handler and action mode for multi selection
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Main view fragment callbacks
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun colorizeToolbar(color: Int) {
        if (prefs.colorizeTitlebar) {
            if (color != prefs.defaultCollectionColor) {
                main_toolbar.backgroundColor = color
                window.statusBarColor = adjustColorAlpha(color, 0.8f)
                return
            }
            main_toolbar.backgroundColor = prefs.colorPrimary
            window.statusBarColor = prefs.colorPrimaryDark
        }
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    // Collection view fragment callbacks
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // clicks on item in navigation drawer

    override fun onDrawerItemClicked(position: Int) {
        TODO()
    }

    override fun onDrawerItemLongClicked(position: Int): Boolean {
        TODO()
        return false
    }

    private fun hide() {
        TODO()
    }

    // Permissions

    //@TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissions() {
        val checkRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val checkWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (PackageManager.PERMISSION_GRANTED != checkRead || PackageManager.PERMISSION_GRANTED != checkWrite) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                TODO()
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
