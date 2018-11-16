package com.labs.pbrother.freegallery.activities

import android.Manifest
import android.annotation.SuppressLint
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
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.adapters.DrawerTagListAdapter
import com.labs.pbrother.freegallery.controller.CollectionItem
import com.labs.pbrother.freegallery.extension.drawerHomeItem
import com.labs.pbrother.freegallery.extension.primaryDrawerItemFromItem
import com.labs.pbrother.freegallery.fragments.CollectionFragment
import com.labs.pbrother.freegallery.fragments.OverviewFragment
import com.labs.pbrother.freegallery.prefs
import com.labs.pbrother.freegallery.viewModels.MainViewModel
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.uiThread


class MainActivity : AppCompatActivity(), OverviewFragment.OnMainFragmentInteractionListener, CollectionFragment.OnCollectionFragmentInteractionListener, DrawerTagListAdapter.ViewHolder.ClickListener {

    private val TAG_HOME = "*HOME*"

    private lateinit var viewModel: MainViewModel

    private var onTablet = false
    private var reloadPlz = false
    private var permissionsGood = false
    private lateinit var drawerResult: Drawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reloadPlz = true
        if (tabletMain != null) onTablet = true

        setTheme(prefs.theme)
        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)
        //main_toolbar.setPadding(0, getStatusBarHeight(this), 0, 0)
        //main_toolbar.backgroundColor = getColor(R.color.nerd_primary)
        frame_container.backgroundColor = prefs.colorPrimary
        goHome()
        makeDrawer()
        bindViewModel()
    }

    private fun goHome() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()

        val home = supportFragmentManager.findFragmentByTag(TAG_HOME)
        if (home != null) {
            fragmentTransaction.replace(R.id.frame_container, home, TAG_HOME)
        } else {
            fragmentTransaction.replace(R.id.frame_container, OverviewFragment(), TAG_HOME)
        }

        fragmentTransaction.commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        drawerResult?.deselect()
//        val ft = supportFragmentManager.beginTransaction()
//        val home = supportFragmentManager.findFragmentByTag(TAG_HOME)
//        if (home != null && home.isAdded) {
//            finish() // TODO start finish counter (press back one more time)
//        } else {
//            goHome()
//        }
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

        addDrawerHomeItem()

        if (!prefs.hideDrawerHeader) drawerResult.header?.drawerTopArea?.backgroundColor = prefs.colorPrimary

        if (onTablet) {
            nav_tablet.addView(drawerResult.slider)
        }
    }

    private fun bindViewModel() {
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java!!)
        viewModel.drawerItems.observe(this, Observer { drawerItems ->
            if (null != drawerItems) addDrawerItems(drawerItems)
        })
    }

    private fun addDrawerHomeItem() {
        drawerResult.addItem(drawerHomeItem()
                .withOnDrawerItemClickListener({ view, position, drawerItem ->
                    goHome()
                    false
                }))
    }

    private fun addDrawerItems(drawerItems: ArrayList<CollectionItem>) {
        drawerItems.forEach {
            this@MainActivity
                    .drawerResult
                    .addItem(primaryDrawerItemFromItem(it, getString(R.string.tagLetter))
                            .withOnDrawerItemClickListener { view, position, drawerItem ->
                                val fragmentTransaction = supportFragmentManager.beginTransaction()
                                val tag = it.id
                                val target = supportFragmentManager.findFragmentByTag(tag)
                                if (target != null) {
                                    fragmentTransaction.replace(R.id.frame_container, target, tag)
                                } else {
                                    fragmentTransaction.replace(R.id.frame_container, CollectionFragment.newInstance(it.id), tag)
                                }
                                fragmentTransaction.addToBackStack(null).commit()
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
        if (permissionsGood) {
            refresh()
            reloadPlz = false
        }
    }

    private fun refresh() {
        //swipeRefreshMain.isRefreshing = true
        doAsync {
            viewModel.refresh()
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
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COLLECTION_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data?.getBooleanExtra(SHOULD_RELOAD, false) ?: false) buildUiSafe()

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
//            R.id.menu_settings -> {
//                startActivity<SettingsActivity>()
//                return true
//            }
            R.id.menu_license -> {
                startActivity<AboutActivity>()
                return true
            }
//            R.id.menu_takeSdCardPermission -> {
//                openSAFTreeSelection()
//                return true
//            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    // Click handler and action mode for multi selection
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Main view fragment callbacks
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("RestrictedApi") // TODO - try to remove every now and then
    override fun openCollectionView(position: Int, id: String) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.frame_container, CollectionFragment.newInstance(id))
                .addToBackStack(null)
                .commit()
        //supportActionBar?.setDefaultDisplayHomeAsUpEnabled(true)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //supportActionBar?.setHomeButtonEnabled(true)
    }

    override fun onCollectionColorChange(color: Int) {
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

    override fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun setToolbarDefaultColor() {
        onCollectionColorChange(prefs.defaultCollectionColor)
    }

    override fun setToolbarDefaultName() {
        supportActionBar?.title = getString(R.string.app_name)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

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
