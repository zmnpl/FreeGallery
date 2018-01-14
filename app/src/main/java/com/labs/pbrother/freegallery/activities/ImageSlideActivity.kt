package com.labs.pbrother.freegallery.activities

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.EditActivity.Companion.NEW_VERSION_PATH
import com.labs.pbrother.freegallery.activities.EditActivity.Companion.ORIGINAL_PATH
import com.labs.pbrother.freegallery.controller.Item
import com.labs.pbrother.freegallery.controller.TPYE_VIDEO
import com.labs.pbrother.freegallery.controller.TYPE_IMAGE
import com.labs.pbrother.freegallery.dialogs.ImagePropertyDialogFragment
import com.labs.pbrother.freegallery.dialogs.TagDialogFragment
import com.labs.pbrother.freegallery.fragments.ImagePageFragment
import com.labs.pbrother.freegallery.settings.DeviceConfiguration
import com.labs.pbrother.freegallery.settings.SettingsHelper
import com.labs.pbrother.freegallery.uiother.DepthPageTransformer
import kotlinx.android.synthetic.main.activity_image_slide.*
import kotlinx.android.synthetic.main.singlepicture_toolbar.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast
import org.jetbrains.anko.uiThread
import java.io.File

class ImageSlideActivity : AppCompatActivity(), TagDialogFragment.TagDialogListener, ImagePropertyDialogFragment.ImagePropertyDialogListener {

    private val EDIT_ACTIVITY = 0

    // providers
    private var serviceBound = false
    private lateinit var viewModel: ImageSlideActivityViewModel

    // instance states
    private val CID: String = "collectionId"
    private val ITEM_INDEX: String = "itemIndex"
    private val ITEM_ID: String = "itemId"
    private val DELETED_SMTTH: String = "deletedSmth"

    // init
    private var collectionId: String = ""
    private var itemIndex: Int = 0
    private var deletedSmth = false

    // misc
    private var reloadPlz = true
    private val INITIAL_HIDE_DELAY = 10000
    private val resultIntent = Intent()
    private lateinit var settings: SettingsHelper
    private lateinit var decorView: View
    private lateinit var clickDetector: GestureDetector
    private var hideSystemUiHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            hideSystemUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reloadPlz = true
        collectionId = intent.getStringExtra(EXTRA_COLLECTIONID) ?: ""
        itemIndex = intent.getIntExtra(EXTRA_ITEM_INDEX, 0)
        savedInstanceState?.apply {
            collectionId = getString(CID)
            itemIndex = getInt(ITEM_INDEX)
            deletedSmth = getBoolean(DELETED_SMTTH)
            if (deletedSmth) resultIntent.putExtra(DELETION, true)
            finish() // TODO temporary - should not finish
        }

        // helper for settings
        settings = SettingsHelper(applicationContext)
        setTheme(settings.theme)

        // layout
        setContentView(R.layout.activity_image_slide)

        // transparent statusbar
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        // toolbar
        setSupportActionBar(singlepicture_Toolbar)
        singlepicture_Toolbar.popupTheme = settings.theme
        singlepicture_snakeTag.background = tagSymbol(this)

        // toolbar button clicks
        singlepicture_infoButton.setOnClickListener { showImageProperties() }
        singlepicture_snakeTagButton.setOnClickListener { tag() }
        singlepicture_shareButton.setOnClickListener { share() }

        // register GestureDetector for
        registerGestureDetector()

        // initialize DecorView
        initDecorView()

        // hide - show necessary to correctly display everything
        hideSystemUI()
        showSystemUI()
        setToolbarPadding()

        bindViewModel()
        refresh()
    }

    private fun bindViewModel() {
        viewModel = ViewModelProviders.of(this).get(ImageSlideActivityViewModel::class.java!!)

        viewModel.items.observe(this, Observer { items ->
            if (null != items) makeViewPager(items)
        })
    }

    // Lifecycle
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == EDIT_ACTIVITY && resultCode == Activity.RESULT_OK && data.getBooleanExtra(CROP_SAVED, false)) {
            doAsync {
                val from = data.getStringExtra(ORIGINAL_PATH)
                val to = data.getStringExtra(NEW_VERSION_PATH)
                viewModel.copyTags(from, to)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putString(CID, collectionId)
            putInt(ITEM_INDEX, itemIndex)
            putString(ITEM_ID, viewModel.itemIdOf(pager.currentItem))
            putBoolean(DELETED_SMTTH, deletedSmth)
        }
        super.onSaveInstanceState(outState)
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, resultIntent)
        super.finish()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(INITIAL_HIDE_DELAY)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setToolbarPadding()
    }

    override fun onStop() {
        super.onStop()
        // catch index of item, where we jumped away
        itemIndex = pager.currentItem
    }

    override fun onDestroy() {
        super.onDestroy()
        setResult(Activity.RESULT_OK, resultIntent)
    }

    // User Interface Building
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // checks service boundary and data status
    // arrogantly not checking for permissions on sub screens ^^
    // if all good -> populate ui
    // if not, service probably needs to be connected

    private fun refresh() {
        doAsync {
            if (intent.getIntExtra(EXTRA_STARTING_POINT, -1) == STARTED_FROM_ACTIVITY) {
                viewModel.refresh(collectionId)
            } else {
                doAsync {
                    viewModel.namingMethodsIsHard(intent.data.toString())
                }
            }
        }
    }

    private fun makeViewPager(items: List<Item>) {
        pager.offscreenPageLimit = 2
        pager.adapter = ScreenSlidePagerAdapter(items, supportFragmentManager)
        pager.currentItem = itemIndex
        pager.setPageTransformer(true, DepthPageTransformer())
    }

    // Actions, Dialogs and actions on those
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Create toolbar menu
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_singlepicture, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.singlepicture_setas -> {
                    setAs()
                    true
                }
                R.id.singlepicture_edit -> {
                    edit()
                    true
                }
                R.id.singlepicture_delete -> {
                    delete()
                    true
                }
                R.id.singlepicture_rot90 -> {
                    // TODO - rotation
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }


    // Shows dialog with image information
    private fun showImageProperties() {
        val diag = ImagePropertyDialogFragment()
        val which = viewModel.itemAt(pager.currentItem)
        if (null != which) {
            diag.setItem(which)
            diag.show(this.fragmentManager, "imagepropertydialog")
        }
    }

    // Callback when image property dialog gets closed via button
    override fun imgPropertyOk() {}

    // Starts dialog for snaketagging
    private fun tag() {
        val std = TagDialogFragment()
        std.setTags(viewModel.tags)
        std.show(this.fragmentManager, "snaketagdialog")
    }

    // Callback, receives result from tag dialog
    override fun tagOk(tag: String) = viewModel.tagItem(viewModel.itemAt(pager.currentItem), tag)

    // Callback if tag dialog was canceled
    override fun tagCancel() {}

    // Reaction to toolbar button click
    // Delete image ... (!)
    private fun delete() {
        val index = pager.currentItem
        val itemToDelete = viewModel.itemAt(index)

        if (null != itemToDelete) {
            resultIntent.putExtra(DELETION, true)

            doAsync {
                val remaining = viewModel.removeItem(itemToDelete)
                uiThread {
                    pager.adapter?.notifyDataSetChanged()
                    longToast(R.string.DeleteSnackbarSingleInfo)
                    if (0 == remaining) {
                        finish()
                    } else {
                        when {
                            index < remaining -> pager.currentItem = index
                            index == remaining -> pager.currentItem = index - 1
                        }
                    }
                }
            }
        }
    }

    // Reaction to toolbar button click
    // Shares currently displayed picture via intent so user can share with other apps
    private fun share() {
        val item = viewModel.itemAt(pager.currentItem)
        val intent = Intent(Intent.ACTION_SEND)
        if (item?.type == TPYE_VIDEO) {
            intent.type = "video/*"
        } else {
            intent.type = "image/jpg"
        }

        intent.putExtra(Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                        this,
                        packageName + ".provider",
                        File(item?.path)))
        startActivity(Intent.createChooser(intent, resources.getString(R.string.shareinsult)))
    }

    // Reaction to toolbar button click
    // Sends intent to set the image as ... something
    private fun setAs() {
        // Do nothing for videos
        val item = viewModel.itemAt(pager.currentItem)
        if (item?.type != TYPE_IMAGE) {
            Toast.makeText(this, getString(R.string.SetVideoAsMakesNoSense), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_ATTACH_DATA)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        val uri = Uri.parse(item.fileUrl)
        intent.setDataAndType(uri, "image/*")
        intent.putExtra("mimeType", "image/*")
        this.startActivity(Intent.createChooser(intent, resources.getString(R.string.setaswhat)))
    }

    private fun edit() {
        var uristring = viewModel.itemAt(pager.currentItem)?.fileUrl
        startActivityForResult(
                intentFor<EditActivity>(EditActivity.EXTRA_URI_STRING to uristring), EDIT_ACTIVITY)
    }


    // UI Behaviour
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // A simple pager adapter
    private inner class ScreenSlidePagerAdapter(val items: List<Item>, fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            val f = ImagePageFragment()
            f.setmGestureDetector(clickDetector)
            f.setItem(items[position])
            return f
        }

        override fun getCount() = items.size

        // This is called when notifyDataSetChanged() is called
        override fun getItemPosition(`object`: Any): Int = FragmentStatePagerAdapter.POSITION_NONE // refresh all fragments when data set changed
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // When the window loses focus (e.g. the action overflows is shown),
        // cancel any pending hide action. When the window gains focus,
        // hide the system UI.
        if (hasFocus) {
            delayedHide(INITIAL_HIDE_DELAY)
        } else {
            hideSystemUiHandler.removeMessages(0)
        }
    }

    // Sets padding for bottom toolbar in consideration of orientation
    // In portrait sets padding, so that the toolbar will be above the sofkeys
    // In landscape, right padding will be the width of the vertically right aligned softkeys
    private fun setToolbarPadding() {
        if (DeviceConfiguration.instance.getRotation(this@ImageSlideActivity) === DeviceConfiguration.PORTRAIT || DeviceConfiguration.instance.getRotation(this@ImageSlideActivity) === DeviceConfiguration.REVERSE_PORTRAIT) {
            singlepicture_Toolbar.setPadding(0, 0, 0, DeviceConfiguration.instance.getNavBarHeight(this@ImageSlideActivity))
        } else {
            singlepicture_Toolbar.setPadding(0, 0, DeviceConfiguration.instance.getNavBarWidth(this@ImageSlideActivity), 0)
        }
    }

    private fun initDecorView() {
        decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { flags ->
            val visible = flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
            // set state of views here

            setToolbarPadding()
            singlepicture_Toolbar.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private fun registerGestureDetector() {
        clickDetector = GestureDetector(this,
                object : GestureDetector.SimpleOnGestureListener() {
                    /**
                     * Triggers, when confident, that input was a single tap.
                     * @param e MotionEvent passed to the method
                     * @return
                     */
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        val visible = decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                        if (visible) {
                            hideSystemUI()
                        } else {
                            showSystemUI()
                        }
                        return true // event is consumed
                    }
                })

        singlepicture_main.setOnTouchListener { view, motionEvent -> clickDetector.onTouchEvent(motionEvent) }
    }

    override fun onStart() {
        super.onStart()
    }


    // Sends message to handler for hiding system bars with specified delay
    // delay in milli seconds
    private fun delayedHide(delayMillis: Int) {
        hideSystemUiHandler.removeMessages(0)
        hideSystemUiHandler.sendEmptyMessageDelayed(0, delayMillis.toLong())
    }

    // Hides the system barsmPager.
    private fun hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}
