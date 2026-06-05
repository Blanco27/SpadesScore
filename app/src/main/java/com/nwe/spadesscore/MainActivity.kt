package com.nwe.spadesscore

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
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
import java.util.Locale

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
 * [GameViewModel] hält den Zustand. Zurück ist im gesamten Flow deaktiviert. Sprache lebt im VM;
 * Locale wird vor setContent angewandt, Wechsel via recreate().
 */
class MainActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels { GameViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLocale(localeCodeFor(gameViewModel.uiState.value.language))

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
                            language = state.language,
                            onSelectPlayerCount = gameViewModel::selectPlayerCount,
                            onSelectLanguage = { changeLanguage(it) },
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

/** Vorwärts-Navigation ohne sichtbaren Back-Stack (Wizard). */
private fun NavHostController.navTo(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}
