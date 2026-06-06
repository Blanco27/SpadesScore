package com.nwe.spadesscore

import android.content.Intent
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.confirm.ConfirmTicksUiState
import com.nwe.spadesscore.ui.confirm.ConfirmTicksViewModel
import com.nwe.spadesscore.ui.gameRepository
import java.util.Locale

class ConfirmTicksActivity : SpadesAppCompatActivity() {

    private val viewModel: ConfirmTicksViewModel by viewModels {
        viewModelFactory { initializer { ConfirmTicksViewModel(gameRepository) } }
    }

    private lateinit var roundTextView: TextView
    private lateinit var scoreViews: List<TextView>
    private lateinit var nameViews: List<TextView>
    private lateinit var tricksViews: List<TextView>
    private lateinit var pointsViews: List<TextView>
    private lateinit var checkboxes: List<CheckBox>
    private lateinit var progressBar: ProgressBar

    private var defaultColor = 0
    private var greenColor = 0

    override fun initContentView() {
        setContentView(R.layout.activity_confirm_tricks)
        greenColor = ContextCompat.getColor(this, R.color.plusPointsText)
    }

    override fun initializeUIComponents() {
        roundTextView = findViewById(R.id.round_TextView)
        progressBar = findViewById(R.id.progressBar)
        scoreViews = listOf(
            findViewById(R.id.current_score_player1_text_view),
            findViewById(R.id.current_score_player2_text_view),
            findViewById(R.id.current_score_player3_text_view),
            findViewById(R.id.current_score_player4_text_view),
        )
        nameViews = listOf(
            findViewById(R.id.player1_name_text),
            findViewById(R.id.player2_name_text),
            findViewById(R.id.player3_name_text),
            findViewById(R.id.player4_name_text),
        )
        tricksViews = listOf(
            findViewById(R.id.player1_amount_of_tricks_text),
            findViewById(R.id.player2_amount_of_tricks_text),
            findViewById(R.id.player3_amount_of_tricks_text),
            findViewById(R.id.player4_amount_of_tricks_text),
        )
        pointsViews = listOf(
            findViewById(R.id.player1_points_text),
            findViewById(R.id.player2_points_text),
            findViewById(R.id.player3_points_text),
            findViewById(R.id.player4_points_text),
        )
        checkboxes = listOf(
            findViewById(R.id.player1_checkbox),
            findViewById(R.id.player2_checkbox),
            findViewById(R.id.player3_checkbox),
            findViewById(R.id.player4_checkbox),
        )
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        val playerCount = state.players.size
        if (playerCount == 3) {
            scoreViews[3].visibility = View.GONE
            findViewById<View>(R.id.player4_layout).visibility = View.GONE
        }
        for (index in 0 until playerCount) {
            checkboxes[index].setOnClickListener {
                pointsViews[index].setTextColor(if (checkboxes[index].isChecked) greenColor else defaultColor)
            }
        }
        progressBar.max = state.amountOfRounds
        progressBar.progress = state.round
        findViewById<View>(R.id.start_Button).setOnClickListener { startNextRound() }
        defaultColor = pointsViews[0].textColors.defaultColor
        render(state)
    }

    private fun render(state: ConfirmTicksUiState) {
        roundTextView.text = getString(R.string.round, state.round)
        state.players.forEachIndexed { index, player ->
            scoreViews[index].text = "${player.name}: ${player.score}"
            nameViews[index].text = player.name
            val trickWord =
                if (player.prediction == 1) getString(R.string.trick) else getString(R.string.tricks)
            tricksViews[index].text =
                String.format(Locale.getDefault(), "%d %s", player.prediction, trickWord)
            pointsViews[index].text = getString(R.string.points_added, player.pointsIfHit)
        }
    }

    private fun startNextRound() {
        val playerCount = viewModel.uiState().players.size
        val hits = (0 until playerCount).map { checkboxes[it].isChecked }
        val gameOver = viewModel.confirm(hits)
        val next = if (gameOver) ResultScreenActivity::class.java else DealCardsActivity::class.java
        startActivity(Intent(this, next))
    }
}
