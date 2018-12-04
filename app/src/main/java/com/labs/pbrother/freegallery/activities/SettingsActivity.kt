package com.labs.pbrother.freegallery.activities

import android.os.Bundle
import androidx.core.app.NavUtils
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.prefs
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by simon on 03.12.16.
 */

class SettingsActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(prefs.theme)
        setContentView(R.layout.activity_settings)
        main_toolbar.backgroundColor = prefs.colorPrimary
        setSupportActionBar(main_toolbar)
    }

    public override fun onStart() {
        super.onStart()
        supportActionBar?.setTitle(R.string.title_activity_settings)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            NavUtils.navigateUpFromSameTask(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}
