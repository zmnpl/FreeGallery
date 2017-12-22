package com.labs.pbrother.freegallery.activities

import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem

import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.settings.SettingsHelper
import kotlinx.android.synthetic.main.toolbar.*

/**
 * Created by simon on 03.12.16.
 */

class SettingsActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // helper for settings
        val settings = SettingsHelper(applicationContext)
        setTheme(settings.theme)

        setContentView(R.layout.activity_settings)
        setSupportActionBar(main_toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onStart() {
        super.onStart()
        supportActionBar!!.setTitle(R.string.title_activity_settings)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            NavUtils.navigateUpFromSameTask(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}
