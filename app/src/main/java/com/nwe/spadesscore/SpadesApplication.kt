package com.nwe.spadesscore

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.nwe.spadesscore.di.AppContainer
import com.nwe.spadesscore.ui.toNightMode

class SpadesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AppCompatDelegate.setDefaultNightMode(container.themePreferences.themeMode.toNightMode())
    }
}
