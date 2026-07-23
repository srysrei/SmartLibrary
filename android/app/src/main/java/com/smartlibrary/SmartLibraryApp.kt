package com.smartlibrary

import android.app.Application

/**
 * Application entry point. Re-applies the user's saved UI theme (Light / Dark) before any
 * activity is shown so the app launches in the chosen mode.
 */
class SmartLibraryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.apply(this)
    }
}
