package com.nwe.spadesscore

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.nwe.spadesscore.ui.applyPersistedLocale

abstract class SpadesAppCompatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPersistedLocale()
        initContentView()
        initializeUIComponents()
        setupUI()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* disable back */ }
        })
    }

    protected abstract fun initializeUIComponents()

    protected abstract fun setupUI()

    protected abstract fun initContentView()
}
