package com.labs.pbrother.freegallery.activities

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.controller.TYPE_TAG
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.doAsync

class EditActivity : AppCompatActivity() {

    companion object {
        val EXTRA_URI_STRING = "itemuri"
    }

    private lateinit var itemUri: Uri
    private lateinit var cropper: CropImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        setSupportActionBar(main_toolbar)

        itemUri = Uri.parse(intent.getStringExtra(EXTRA_URI_STRING) ?: "")
        savedInstanceState?.apply {

        }

        cropper = cropImageView
    }

    override fun onStart() {
        super.onStart()
        cropper.setImageUriAsync(itemUri);
    }

    //region menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rotLeft -> {
                cropper.rotateImage(-90)
                return true
            }
            R.id.menu_rotRight -> {
                cropper.rotateImage(90)
                return true
            }
            R.id.menu_save -> {
                cropper.saveCroppedImageAsync(Uri.parse(""))
                return true
            }
        }
    }
    // endregion
}
