package com.labs.pbrother.freegallery

import android.app.Application
import com.labs.pbrother.freegallery.extension.discoverSDPath
import com.labs.pbrother.freegallery.settings.DeviceConfiguration.Companion.instance
import com.labs.pbrother.freegallery.settings.SettingsHelper

val prefs: SettingsHelper by lazy {
    App.prefs
}

class App : Application() {

    companion object {
        lateinit var prefs: SettingsHelper
    }

    override fun onCreate() {
        prefs = SettingsHelper(applicationContext)
        discoverSDPath()
        super.onCreate()
    }
}
