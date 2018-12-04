package com.labs.pbrother.freegallery.extension

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.labs.pbrother.freegallery.READ_REQUEST_CODE

fun AppCompatActivity.openSAFTreeSelection() {
    val intent =
    //intent.addCategory(Intent.CATEGORY_OPENABLE)1
    // intent.type = "image/*"
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), READ_REQUEST_CODE)
}
