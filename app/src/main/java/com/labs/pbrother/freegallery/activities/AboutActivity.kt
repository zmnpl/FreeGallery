package com.labs.pbrother.freegallery.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.prefs
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor

/**
 * Created by simon on 15.12.16.
 */

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(prefs.theme)
        setContentView(R.layout.activity_about)
        main_toolbar.backgroundColor = prefs.colorPrimary
        setSupportActionBar(main_toolbar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun showLicensesClick(view: View) {
        val intent = Intent(this, LicenseActivity::class.java)
        this.startActivity(intent)
    }
}
