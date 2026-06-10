package com.nwe.spadesscore

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Space
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.color.MaterialColors
import com.nwe.spadesscore.ui.applyPersistedLocale
import com.nwe.spadesscore.ui.applySystemBarInsetsAsPadding
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.result.ResultViewModel

class ResultScreenActivity : AppCompatActivity() {

    private val viewModel: ResultViewModel by viewModels {
        viewModelFactory { initializer { ResultViewModel(gameRepository) } }
    }

    private companion object {
        const val MAX_ROUNDS = 20
        const val PLAYER4_SPACE_COUNT = 21
    }

    // scoreCells[player][round]: player 0..3, round 0..19
    private lateinit var scoreCells: List<List<TextView>>
    private lateinit var roundRows: List<TableRow>
    private lateinit var player4Spaces: List<Space>

    // New: totals row views (index 0..3 → player 1..4)
    private lateinit var totalScoreViews: List<TextView>
    private lateinit var rankBadgeViews: List<TextView>
    private lateinit var crownViews: List<ImageView>

    @SuppressLint("DiscouragedApi")
    private fun viewIdByName(name: String): Int = resources.getIdentifier(name, "id", packageName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPersistedLocale()
        setContentView(R.layout.activity_result_screen)
        findViewById<View>(android.R.id.content).applySystemBarInsetsAsPadding()

        // ── Original getIdentifier loops — preserved exactly ──────────
        scoreCells = (1..4).map { player ->
            (1..MAX_ROUNDS).map { round ->
                findViewById<TextView>(viewIdByName("score_player${player}_round$round"))
            }
        }
        roundRows = (1..MAX_ROUNDS).map { findViewById<TableRow>(viewIdByName("round${it}Scores")) }
        player4Spaces = (1..PLAYER4_SPACE_COUNT).map { findViewById<Space>(viewIdByName("player4_space$it")) }

        // ── New view lookups via getIdentifier ─────────────────────────
        totalScoreViews = (1..4).map { i -> findViewById<TextView>(viewIdByName("total_$i")) }
        rankBadgeViews  = (1..4).map { i -> findViewById<TextView>(viewIdByName("rank_badge_$i")) }
        crownViews      = (1..4).map { i -> findViewById<ImageView>(viewIdByName("crown_$i")) }

        val state = viewModel.uiState()

        // ── Original operations ────────────────────────────────────────
        hidePlayer4IfNeeded(state.playerCount)
        // The totals row below already represents the last played round (its
        // cumulative score IS the final total), so render only the rounds before
        // it as plain grid rows — otherwise the final sum is shown twice.
        setRowVisibility(state.visibleRoundCount - 1)
        fillScores(state.scoresByPlayer)
        applyRowSpacing()
        setPlayerNames(state.playerCount, state.playerNames)

        // ── New operations ─────────────────────────────────────────────
        setResultHeader(state.visibleRoundCount)
        setTotals(state.scoresByPlayer, state.placementByPlayer, state.highlightColumnIndex)

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
            // Hide player-4 totals column and its preceding separator
            findViewById<View>(R.id.total_col_p4).visibility = View.GONE
            findViewById<View>(R.id.total_sep_p4).visibility = View.GONE
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

    /** Adds vertical breathing room between score rows so the table uses more of the
     *  screen height instead of looking compressed at the top (mockup rhythm). */
    private fun applyRowSpacing() {
        val pad = (14 * resources.displayMetrics.density).toInt()
        scoreCells.forEach { playerRows ->
            playerRows.forEach { cell ->
                cell.setPaddingRelative(cell.paddingStart, pad, cell.paddingEnd, pad)
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

    private fun setResultHeader(visibleRoundCount: Int) {
        // result_title text is static (@string/final_score set in XML)
        val subtitle = getString(R.string.rounds_played, visibleRoundCount)
        findViewById<TextView>(R.id.result_subtitle).text = subtitle
    }

    private fun setTotals(
        scoresByPlayer: List<List<Int>>,
        placementByPlayer: Map<Int, Int>,
        highlightColumnIndex: Int,
    ) {
        val accentColor = MaterialColors.getColor(this, R.attr.appAccent, 0)
        val inkColor    = MaterialColors.getColor(this, R.attr.appInk, 0)
        val mutedColor  = MaterialColors.getColor(this, R.attr.appMuted, 0)

        // Iterate only over players present in scoresByPlayer (3 or 4)
        scoresByPlayer.indices.forEach { playerIndex ->
            // Total = score at the last played round (highlightColumnIndex is 0-based)
            val scores = scoresByPlayer[playerIndex]
            val total = when {
                highlightColumnIndex >= 0 && highlightColumnIndex < scores.size ->
                    scores[highlightColumnIndex]
                scores.isNotEmpty() -> scores.last()
                else -> 0
            }

            totalScoreViews[playerIndex].text = total.toString()

            val placement = placementByPlayer[playerIndex] ?: (playerIndex + 1)
            rankBadgeViews[playerIndex].text = "$placement."

            if (placement == 1) {
                rankBadgeViews[playerIndex].setBackgroundResource(R.drawable.bg_rank_badge_lead)
                rankBadgeViews[playerIndex].setTextColor(accentColor)
                totalScoreViews[playerIndex].setTextColor(accentColor)
                crownViews[playerIndex].visibility = View.VISIBLE
            } else {
                rankBadgeViews[playerIndex].setBackgroundResource(R.drawable.bg_rank_badge)
                rankBadgeViews[playerIndex].setTextColor(mutedColor)
                totalScoreViews[playerIndex].setTextColor(inkColor)
                crownViews[playerIndex].visibility = View.GONE
            }
        }
    }
}
