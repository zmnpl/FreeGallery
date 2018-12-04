package com.labs.pbrother.freegallery.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.labs.pbrother.freegallery.CROP_SAVED
import com.labs.pbrother.freegallery.R
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File
import java.io.FileOutputStream

class EditActivity : AppCompatActivity(), CropImageView.OnCropImageCompleteListener {

    override fun onCropImageComplete(view: CropImageView?, result: CropImageView.CropResult?) {
        //foo
    }

    companion object {
        val EXTRA_URI_STRING = "itemuri"
        val ORIGINAL_PATH = "originalPath"
        val NEW_VERSION_PATH = "newVersionPath"
    }

    private lateinit var itemUri: Uri
    private lateinit var cropper: CropImageView
    private val resultIntent = Intent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_OK, resultIntent)
        setContentView(R.layout.activity_edit)

        setSupportActionBar(main_toolbar)
        supportActionBar?.title = getString(R.string.editToolbarText)

        itemUri = Uri.parse(intent.getStringExtra(EXTRA_URI_STRING) ?: "")
        resultIntent.putExtra(ORIGINAL_PATH, itemUri.path)

        savedInstanceState?.apply {

        }

        cropper = cropImageView
        cropper.setOnCropImageCompleteListener(this)
        cropper.guidelines = CropImageView.Guidelines.ON
    }

    override fun onStart() {
        super.onStart()
        cropper.setImageUriAsync(itemUri);
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                save()
                return true
            }
            R.id.menu_rotRight -> {
                cropper.rotateImage(90)
                return true
            }
            R.id.menu_flipHorizontal -> {
                cropper.flipImageHorizontally()
                return true;
            }
            R.id.menu_flipVertical -> {
                cropper.flipImageVertically()
                return true;
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun save() {
        val croppedBmp = cropper.croppedImage
        var newFileName = versionedOutputFileName(itemUri.path)
        doAsync {
            val outStream = FileOutputStream(newFileName)
            croppedBmp.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream?.close()
            val scanjob = arrayOf(newFileName)
            MediaScannerConnection.scanFile(applicationContext, scanjob, null, null)
            uiThread {
                toast(getString(R.string.saveCompleteToast))
            }
        }
        resultIntent.putExtra(CROP_SAVED, true)
        resultIntent.putExtra(NEW_VERSION_PATH, newFileName)
        finish()
    }

    private fun versionedOutputFileName(originalPath: String): String {
        var originalFile = File(originalPath)
        var version = 0;
        var hit = false
        var testfile: File?

        val extension = originalFile.extension
        val fileName = originalFile.name.removeSuffix("." + extension)
        val basePath = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + "/FreeGallery"
        File(basePath).mkdirs()
        while (!hit) {
            val testpath = basePath + "/" + fileName + "_v" + version.toString() + "." + extension
            testfile = File(testpath)
            if (testfile.exists()) {
                version += 1
                continue
            }
            hit = true
            return testfile.path
        }
        return ""
    }

}
