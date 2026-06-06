package com.nwe.spadesscore

import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.deal.DealCardsUiState
import com.nwe.spadesscore.ui.deal.DealCardsViewModel
import com.nwe.spadesscore.ui.gameRepository
import java.util.Locale

class DealCardsActivity : SpadesAppCompatActivity() {

    private val viewModel: DealCardsViewModel by viewModels {
        viewModelFactory { initializer { DealCardsViewModel(gameRepository) } }
    }

    private lateinit var roundTextView: TextView
    private lateinit var playerNameTextView: TextView
    private lateinit var amountTextView: TextView
    private lateinit var scoreViews: List<TextView>
    private lateinit var progressBar: ProgressBar

    override fun initContentView() {
        setContentView(R.layout.activity_deal_cards)
    }

    override fun initializeUIComponents() {
        amountTextView = findViewById(R.id.amount_TextView)
        roundTextView = findViewById(R.id.round_TextView)
        playerNameTextView = findViewById(R.id.player_name_TextView)
        progressBar = findViewById(R.id.progressBar)
        scoreViews = listOf(
            findViewById(R.id.current_score_player1_text_view),
            findViewById(R.id.current_score_player2_text_view),
            findViewById(R.id.current_score_player3_text_view),
            findViewById(R.id.current_score_player4_text_view),
        )
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        if (state.players.size == 3) {
            scoreViews[3].visibility = View.GONE
        }
        progressBar.max = state.amountOfRounds
        progressBar.progress = state.round
        findViewById<View>(R.id.start_Button).setOnClickListener {
            startActivity(Intent(this, DeclareTricksActivity::class.java))
        }
        render(state)
    }

    private fun render(state: DealCardsUiState) {
        roundTextView.text = getString(R.string.round, state.round)
        playerNameTextView.text = getString(R.string.player_must_deal_cards, state.dealerName)
        amountTextView.text = String.format(Locale.getDefault(), "%dx", state.cardAmount)
        state.players.forEachIndexed { index, player ->
            scoreViews[index].text = "${player.name}: ${player.score}"
        }
    }
}
