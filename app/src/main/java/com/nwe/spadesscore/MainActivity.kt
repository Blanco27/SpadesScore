package com.nwe.spadesscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nwe.spadesscore.ui.game.GameViewModel
import com.nwe.spadesscore.ui.setup.SetupScreen
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme
import java.util.Locale

/**
 * Launcher. Hostet aktuell noch nur den [SetupScreen] (NavHost folgt in einem späteren Task) und
 * überbrückt „Weiter" vorerst per startActivity in den Legacy-Flow. Sprache lebt im
 * [GameViewModel]; Locale wird vor setContent angewandt, Wechsel via recreate().
 */
class MainActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels { GameViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLocale(localeCodeFor(gameViewModel.uiState.value.language))

        enableEdgeToEdge()
        setContent {
            SpadesScoreTheme {
                val state by gameViewModel.uiState.collectAsStateWithLifecycle()
                SetupScreen(
                    playerCount = state.playerCount,
                    language = state.language,
                    onSelectPlayerCount = gameViewModel::selectPlayerCount,
                    onSelectLanguage = { changeLanguage(it) },
                    onNext = { startActivity(Intent(this, PlayerNamesActivity::class.java)) }
                )
            }
        }
    }

    private fun changeLanguage(language: Languages) {
        if (gameViewModel.uiState.value.language == language) return
        gameViewModel.selectLanguage(language)
        applyLocale(localeCodeFor(language))
        recreate()
    }

    private fun localeCodeFor(language: Languages?): String =
        if (language == Languages.GERMAN) "de" else "en"

    @Suppress("DEPRECATION")
    private fun applyLocale(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
