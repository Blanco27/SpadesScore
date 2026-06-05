package com.nwe.spadesscore

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nwe.spadesscore.ui.game.ConfirmTricksScreen
import com.nwe.spadesscore.ui.game.DealCardsScreen
import com.nwe.spadesscore.ui.game.DeclareTricksScreen
import com.nwe.spadesscore.ui.game.GameViewModel
import com.nwe.spadesscore.ui.game.PlayerNamesScreen
import com.nwe.spadesscore.ui.game.ResultScreen
import com.nwe.spadesscore.ui.setup.SetupScreen
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

private object Routes {
    const val SETUP = "setup"
    const val PLAYERS = "players"
    const val DEAL = "deal"
    const val DECLARE = "declare"
    const val CONFIRM = "confirm"
    const val RESULT = "result"
}

/**
 * Single-Activity-Host. Ein NavHost trägt Setup + alle Spiel-Screens; ein gemeinsames
 * [GameViewModel] hält den Zustand. Zurück ist im gesamten Flow deaktiviert. Die Sprache wird über
 * [AppCompatDelegate] per-app locales gesetzt (autoStoreLocales persistiert sie; AppCompat ruft
 * recreate() selbst auf), nur auf dem Setup-Screen änderbar.
 */
class MainActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels { GameViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SpadesScoreTheme {
                BackHandler(enabled = true) { /* Zurück im gesamten Flow deaktiviert */ }
                val navController = rememberNavController()
                val state by gameViewModel.uiState.collectAsStateWithLifecycle()

                NavHost(navController = navController, startDestination = Routes.SETUP) {
                    composable(Routes.SETUP) {
                        SetupScreen(
                            playerCount = state.playerCount,
                            languageTag = currentLanguageTag(),
                            onSelectPlayerCount = gameViewModel::selectPlayerCount,
                            onSelectLanguage = { setLanguage(it) },
                            onNext = { navController.navTo(Routes.PLAYERS) }
                        )
                    }
                    composable(Routes.PLAYERS) {
                        PlayerNamesScreen(
                            playerCount = state.playerCount,
                            onStart = { names, randomDealer ->
                                gameViewModel.startGame(names, randomDealer)
                                navController.navTo(Routes.DEAL)
                            }
                        )
                    }
                    composable(Routes.DEAL) {
                        state.game?.let { game ->
                            DealCardsScreen(game = game, onNext = { navController.navTo(Routes.DECLARE) })
                        }
                    }
                    composable(Routes.DECLARE) {
                        state.game?.let { game ->
                            DeclareTricksScreen(
                                game = game,
                                onConfirm = { predictions ->
                                    gameViewModel.setPredictions(predictions)
                                    navController.navTo(Routes.CONFIRM)
                                }
                            )
                        }
                    }
                    composable(Routes.CONFIRM) {
                        state.game?.let { game ->
                            ConfirmTricksScreen(
                                game = game,
                                onDone = { made ->
                                    gameViewModel.confirmRound(made)
                                    val showResult = gameViewModel.uiState.value.game?.showResultScreen == true
                                    navController.navTo(if (showResult) Routes.RESULT else Routes.DEAL)
                                }
                            )
                        }
                    }
                    composable(Routes.RESULT) {
                        state.game?.let { game ->
                            ResultScreen(
                                game = game,
                                onContinue = {
                                    gameViewModel.continueSecondHalf()
                                    navController.navTo(Routes.DEAL)
                                },
                                onNewGame = {
                                    gameViewModel.newGame()
                                    navController.navTo(Routes.SETUP)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /** Setzt die App-Sprache; AppCompat persistiert (autoStoreLocales) und ruft recreate() selbst. */
    private fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Aktuell wirksamer Sprach-Tag für den Selektor: app-locale, sonst die aufgelöste Config-Locale. */
    private fun currentLanguageTag(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val locale = if (appLocales.size() > 0) appLocales.get(0) else resources.configuration.locales.get(0)
        return if (locale?.language == "de") "de" else "en"
    }
}

/** Vorwärts-Navigation ohne sichtbaren Back-Stack (Wizard). */
private fun NavHostController.navTo(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}
