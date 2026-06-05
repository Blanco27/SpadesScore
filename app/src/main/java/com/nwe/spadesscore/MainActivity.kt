package com.nwe.spadesscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nwe.spadesscore.ui.setup.GameViewModel
import com.nwe.spadesscore.ui.setup.SetupScreen
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme
import java.util.Locale

/**
 * Launcher screen, now in Compose. Hosts the stateless [SetupScreen] wired to [GameViewModel].
 * Player count / language flow through the ViewModel into the SpadesGame singleton; "Next"
 * bridges into the still-legacy [PlayerNamesActivity]. Language changes apply a locale and
 * recreate the activity, matching the previous behaviour.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = SpadesGame.getInstance()
        if (game.languages == null) {
            game.setLanguages(Languages.ENGLISH)
        }
        applyLocale(localeCodeFor(game.languages))

        enableEdgeToEdge()
        setContent {
            SpadesScoreTheme {
                val viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SetupScreen(
                    state = state,
                    onSelectPlayerCount = viewModel::selectPlayerCount,
                    onSelectLanguage = { language -> changeLanguage(viewModel, language) },
                    onNext = { startActivity(Intent(this, PlayerNamesActivity::class.java)) }
                )
            }
        }
    }

    private fun changeLanguage(viewModel: GameViewModel, language: Languages) {
        if (viewModel.uiState.value.language == language) return
        viewModel.selectLanguage(language)
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
