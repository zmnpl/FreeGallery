package com.labs.pbrother.freegallery.fragments

import android.os.Bundle
import android.preference.PreferenceFragment

import com.labs.pbrother.freegallery.R

/**
 * Created by simon on 03.12.16.
 */

class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }
}
