package com.nwe.spadesscore

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Space
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.result.ResultViewModel

class ResultScreenActivity : AppCompatActivity() {

    private val viewModel: ResultViewModel by viewModels {
        viewModelFactory { initializer { ResultViewModel(gameRepository) } }
    }

    private companion object {
        const val MAX_ROUNDS = 20
        const val PLAYER4_SPACE_COUNT = 21
        const val HIGHLIGHT_TEXT_SIZE_SP = 30f
        const val COLOR_GOLD = "#ffd700"
        const val COLOR_SILVER = "#e6e6e6"
        const val COLOR_BRONZE = "#bf8970"
    }

    // scoreCells[player][round]: Spieler 0..3, Runde 0..19
    private lateinit var scoreCells: List<List<TextView>>
    private lateinit var roundRows: List<TableRow>
    private lateinit var player4Spaces: List<Space>

    @SuppressLint("DiscouragedApi")
    private fun viewIdByName(name: String): Int = resources.getIdentifier(name, "id", packageName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_screen)

        scoreCells = (1..4).map { player ->
            (1..MAX_ROUNDS).map { round ->
                findViewById<TextView>(viewIdByName("score_player${player}_round$round"))
            }
        }
        roundRows = (1..MAX_ROUNDS).map { findViewById<TableRow>(viewIdByName("round${it}Scores")) }
        player4Spaces = (1..PLAYER4_SPACE_COUNT).map { findViewById<Space>(viewIdByName("player4_space$it")) }

        val state = viewModel.uiState()

        hidePlayer4IfNeeded(state.playerCount)
        setRowVisibility(state.visibleRoundCount)
        fillScores(state.scoresByPlayer)
        setPlayerNames(state.playerCount, state.playerNames)
        highlightLastColumn(state.placementByPlayer, state.highlightColumnIndex)

        val continueButton = findViewById<Button>(R.id.continue_button)
        continueButton.setOnClickListener {
            viewModel.startSecondHalf()
            startActivity(Intent(this, DealCardsActivity::class.java))
        }
        if (state.isSecondHalf) {
            continueButton.visibility = View.GONE
            findViewById<Space>(R.id.lowerSpace).visibility = View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* disable back */ }
        })
    }

    private fun hidePlayer4IfNeeded(playerCount: Int) {
        if (playerCount == 3) {
            scoreCells[3].forEach { it.visibility = View.GONE }
            player4Spaces.forEach { it.visibility = View.GONE }
            findViewById<TextView>(R.id.header_player4).visibility = View.GONE
        }
    }

    private fun setRowVisibility(visibleRoundCount: Int) {
        roundRows.forEach { it.visibility = View.GONE }
        for (i in 0 until visibleRoundCount) {
            roundRows[i].visibility = View.VISIBLE
        }
    }

    private fun fillScores(scoresByPlayer: List<List<Int>>) {
        scoresByPlayer.forEachIndexed { playerIndex, scores ->
            scores.forEachIndexed { roundIndex, score ->
                scoreCells[playerIndex][roundIndex].text = score.toString()
            }
        }
    }

    private fun setPlayerNames(playerCount: Int, names: List<String>) {
        findViewById<TextView>(R.id.header_player1).text = names[0]
        findViewById<TextView>(R.id.header_player2).text = names[1]
        findViewById<TextView>(R.id.header_player3).text = names[2]
        if (playerCount == 4) {
            findViewById<TextView>(R.id.header_player4).text = names[3]
        }
    }

    private fun highlightLastColumn(placementByPlayer: Map<Int, Int>, columnIndex: Int) {
        if (columnIndex < 0) return
        placementByPlayer.forEach { (playerIndex, place) ->
            val cell = scoreCells[playerIndex][columnIndex]
            cell.setTextColor(colorForPlace(place))
            cell.textSize = HIGHLIGHT_TEXT_SIZE_SP
        }
    }

    private fun colorForPlace(place: Int): Int = when (place) {
        1 -> Color.parseColor(COLOR_GOLD)
        2 -> Color.parseColor(COLOR_SILVER)
        3 -> Color.parseColor(COLOR_BRONZE)
        else -> scoreCells[0][1].textColors.defaultColor
    }
}
