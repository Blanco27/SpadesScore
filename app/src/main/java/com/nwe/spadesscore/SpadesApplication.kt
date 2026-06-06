package com.nwe.spadesscore

import android.app.Application
import com.nwe.spadesscore.di.AppContainer

class SpadesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
