package com.labs.pbrother.freegallery.fragments


import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.activities.LicenseActivity
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_about.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick


class AboutFragment : androidx.fragment.app.Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rv = inflater.inflate(R.layout.fragment_about, container, false)

        rv.btnOpenLicenseTexts.onClick {
            val intent = Intent(activity, LicenseActivity::class.java)
            this@AboutFragment.startActivity(intent)
        }

        return rv
    }

}
