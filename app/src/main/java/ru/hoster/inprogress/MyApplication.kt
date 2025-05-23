package ru.hoster.inprogress

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    // You can add Timber initialization or other app-wide setup here if needed
    override fun onCreate() {
        super.onCreate()
        // Example: Timber.plant(Timber.DebugTree())
    }
}
