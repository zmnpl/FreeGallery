package com.labs.pbrother.freegallery.extension

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.labs.pbrother.freegallery.activities.READ_REQUEST_CODE

fun AppCompatActivity.openSAFTreeSelection() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    //intent.addCategory(Intent.CATEGORY_OPENABLE)1
    // intent.type = "image/*"
    startActivityForResult(intent, READ_REQUEST_CODE)
}
