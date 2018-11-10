package com.labs.pbrother.freegallery

import android.app.Application
import com.labs.pbrother.freegallery.extension.discoverSDPath
import com.labs.pbrother.freegallery.settings.SettingsHelper

val prefs: SettingsHelper by lazy {
    App.prefs
}

val app: Application by lazy {
    App.instance
}

class App : Application() {

    companion object {
        lateinit var prefs: SettingsHelper
        lateinit var instance: Application
    }

    override fun onCreate() {
        prefs = SettingsHelper(applicationContext)
        instance = this
        discoverSDPath()
        super.onCreate()
    }
}
