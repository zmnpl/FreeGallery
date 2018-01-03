package com.labs.pbrother.freegallery.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import com.labs.pbrother.freegallery.R
import com.labs.pbrother.freegallery.settings.SettingsHelper


/**
 * Created by simon on 03.12.16.
 */

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences,
                                           key: String) {
        val settings = SettingsHelper(activity)
        settings.reactToSettingChange(key)
    }
}
