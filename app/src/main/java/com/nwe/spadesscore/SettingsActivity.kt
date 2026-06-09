package com.nwe.spadesscore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.activity.viewModels
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.ThemeMode
import com.nwe.spadesscore.ui.applyPersistedLocale
import com.nwe.spadesscore.ui.applySystemBarInsetsAsPadding
import com.nwe.spadesscore.ui.applyThemeMode
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.settings.SettingsViewModel
import com.nwe.spadesscore.ui.themePreferences
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory { initializer { SettingsViewModel(gameRepository, themePreferences) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPersistedLocale()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<android.view.View>(android.R.id.content).applySystemBarInsetsAsPadding()

        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }

        bindThemeSegment()
        bindLanguageRows()
        bindFooter()
    }

    private fun bindThemeSegment() {
        val map = mapOf(
            R.id.theme_system to ThemeMode.SYSTEM,
            R.id.theme_light to ThemeMode.LIGHT,
            R.id.theme_dark to ThemeMode.DARK,
        )
        renderThemeSelection(viewModel.uiState().themeMode)
        map.forEach { (id, mode) ->
            findViewById<android.view.View>(id).setOnClickListener {
                renderThemeSelection(mode)   // immediate feedback; setDefaultNightMode only recreates
                applyThemeMode(mode)         // when the effective light/dark appearance actually changes
            }
        }
    }

    private fun renderThemeSelection(selected: ThemeMode) {
        val ids = mapOf(
            ThemeMode.SYSTEM to R.id.theme_system,
            ThemeMode.LIGHT to R.id.theme_light,
            ThemeMode.DARK to R.id.theme_dark,
        )
        ids.forEach { (mode, id) ->
            val cell = findViewById<android.view.View>(id)
            cell.background = if (mode == selected)
                ContextCompat.getDrawable(this, R.drawable.bg_segment_selected) else null
        }
    }

    private fun bindLanguageRows() {
        renderLanguageSelection(viewModel.currentLanguage())
        findViewById<android.view.View>(R.id.lang_en).setOnClickListener { switchLanguage(Language.ENGLISH, "en") }
        findViewById<android.view.View>(R.id.lang_de).setOnClickListener { switchLanguage(Language.GERMAN, "de") }
    }

    private fun renderLanguageSelection(language: Language) {
        findViewById<android.view.View>(R.id.radio_en).setBackgroundResource(
            if (language == Language.ENGLISH) R.drawable.bg_radio_on else R.drawable.bg_radio)
        findViewById<android.view.View>(R.id.radio_de).setBackgroundResource(
            if (language == Language.GERMAN) R.drawable.bg_radio_on else R.drawable.bg_radio)
    }

    private fun switchLanguage(language: Language, code: String) {
        if (viewModel.currentLanguage() == language) return
        viewModel.setLanguage(language)
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    private fun bindFooter() {
        findViewById<TextView>(R.id.version_label).text =
            getString(R.string.app_name) + "  v" + BuildConfig.VERSION_NAME
        findViewById<android.view.View>(R.id.github_link).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url))))
        }
    }
}
