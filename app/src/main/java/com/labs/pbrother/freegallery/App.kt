package com.labs.pbrother.freegallery

import android.app.Application
import com.labs.pbrother.freegallery.settings.SettingsHelper

class App : Application() {
    companion object {
        lateinit var prefs: SettingsHelper
    }

    override fun onCreate() {
        prefs = SettingsHelper(applicationContext)
        super.onCreate()
    }
}
