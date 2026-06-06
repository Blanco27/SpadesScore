package com.nwe.spadesscore

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.main.MainViewModel
import java.util.Locale

class MainActivity : SpadesAppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel(gameRepository) } }
    }

    private lateinit var btn3Players: Button
    private lateinit var btn4Players: Button
    private lateinit var btnLanguageEnglish: Button
    private lateinit var btnLanguageGerman: Button
    private lateinit var startGameButton: Button

    private var colorActive = 0
    private var colorDeactive = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val desired = if (viewModel.uiState().language == Language.GERMAN) "de" else "en"
        if (resources.configuration.locales[0].language != desired) {
            setLocale(desired)
        }
    }

    override fun initContentView() {
        setContentView(R.layout.activity_main)
        colorActive = ContextCompat.getColor(this, R.color.background_button_enabled)
        colorDeactive = ContextCompat.getColor(this, R.color.background_button_disabled)
    }

    override fun initializeUIComponents() {
        btn3Players = findViewById(R.id.btn3Players)
        btn4Players = findViewById(R.id.btn4Players)
        btnLanguageEnglish = findViewById(R.id.btnLanguageEnglish)
        btnLanguageGerman = findViewById(R.id.btnLanguageGerman)
        startGameButton = findViewById(R.id.start_game_button)
    }

    override fun setupUI() {
        val state = viewModel.uiState()

        if (state.playerCount == 4) {
            btn4Players.isSelected = true
            btn3Players.isSelected = false
            (btn4Players.background as GradientDrawable).setColor(colorActive)
            (btn3Players.background as GradientDrawable).setColor(colorDeactive)
        } else {
            btn4Players.isSelected = false
            btn3Players.isSelected = true
            (btn4Players.background as GradientDrawable).setColor(colorDeactive)
            (btn3Players.background as GradientDrawable).setColor(colorActive)
        }

        if (state.language == Language.ENGLISH) {
            btnLanguageEnglish.isSelected = true
            btnLanguageGerman.isSelected = false
            (btnLanguageEnglish.background as GradientDrawable).setColor(colorActive)
            (btnLanguageGerman.background as GradientDrawable).setColor(colorDeactive)
        } else {
            btnLanguageEnglish.isSelected = false
            btnLanguageGerman.isSelected = true
            (btnLanguageEnglish.background as GradientDrawable).setColor(colorDeactive)
            (btnLanguageGerman.background as GradientDrawable).setColor(colorActive)
        }

        btn3Players.setOnClickListener {
            if (viewModel.uiState().playerCount == 3) return@setOnClickListener
            btn3Players.isSelected = true
            btn4Players.isSelected = false
            viewModel.setPlayerCount(3)
            select(btn3Players)
            deselect(btn4Players)
        }
        btn4Players.setOnClickListener {
            if (viewModel.uiState().playerCount == 4) return@setOnClickListener
            btn4Players.isSelected = true
            btn3Players.isSelected = false
            viewModel.setPlayerCount(4)
            select(btn4Players)
            deselect(btn3Players)
        }
        btnLanguageEnglish.setOnClickListener {
            if (viewModel.uiState().language == Language.ENGLISH) return@setOnClickListener
            btnLanguageEnglish.isSelected = true
            btnLanguageGerman.isSelected = false
            viewModel.setLanguage(Language.ENGLISH)
            select(btnLanguageEnglish)
            deselect(btnLanguageGerman)
            setLocale("en")
        }
        btnLanguageGerman.setOnClickListener {
            if (viewModel.uiState().language == Language.GERMAN) return@setOnClickListener
            btnLanguageGerman.isSelected = true
            btnLanguageEnglish.isSelected = false
            viewModel.setLanguage(Language.GERMAN)
            select(btnLanguageGerman)
            deselect(btnLanguageEnglish)
            setLocale("de")
        }
        startGameButton.setOnClickListener {
            startActivity(Intent(this, PlayerNamesActivity::class.java))
        }
    }

    private fun select(button: Button) = animateFill(button, colorDeactive, colorActive)

    private fun deselect(button: Button) = animateFill(button, colorActive, colorDeactive)

    private fun animateFill(view: View, fromColor: Int, toColor: Int) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = 250
            addUpdateListener { animation ->
                (view.background as GradientDrawable).setColor(animation.animatedValue as Int)
            }
            start()
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }
}
