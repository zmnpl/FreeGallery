package com.labs.pbrother.freegallery.activities

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.labs.pbrother.freegallery.R
import kotlinx.android.synthetic.main.activity_edit.*

class EditActivity : AppCompatActivity() {

    companion object {
        val EXTRA_URI_STRING = "itemuri"
    }

    private lateinit var itemUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        itemUri = Uri.parse(intent.getStringExtra(EXTRA_URI_STRING) ?: "")
        savedInstanceState?.apply {

        }
    }

    override fun onStart() {
        super.onStart()
        cropImageView.setImageUriAsync(itemUri);
    }
}
