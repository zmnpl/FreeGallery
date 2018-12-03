package com.labs.pbrother.freegallery.fragments


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.LicenseActivity
import com.labs.pbrother.freegallery.prefs
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.backgroundColor


class AboutFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(prefs.theme)
        //setContentView(R.layout.activity_about)
        //main_toolbar.backgroundColor = prefs.colorPrimary
        //setSupportActionBar(main_toolbar)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    public fun showLicensesClickFoo(view: View) {
        val intent = Intent(activity, LicenseActivity::class.java)
        this.startActivity(intent)
    }

}
