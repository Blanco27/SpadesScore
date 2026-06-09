package com.nwe.spadesscore

import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.bindRoundHeader
import com.nwe.spadesscore.ui.deal.DealCardsViewModel
import com.nwe.spadesscore.ui.gameRepository

class DealCardsActivity : SpadesAppCompatActivity() {

    private val viewModel: DealCardsViewModel by viewModels {
        viewModelFactory { initializer { DealCardsViewModel(gameRepository) } }
    }

    private lateinit var dealerSubtitle: TextView
    private lateinit var cardCount: TextView
    private lateinit var cardsPerPlayerLabel: TextView
    private lateinit var dealNextButton: Button

    override fun initContentView() {
        setContentView(R.layout.activity_deal_cards)
    }

    override fun initializeUIComponents() {
        dealerSubtitle = findViewById(R.id.dealer_subtitle)
        cardCount = findViewById(R.id.card_count)
        cardsPerPlayerLabel = findViewById(R.id.cards_per_player_label)
        dealNextButton = findViewById(R.id.deal_next_button)
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        bindRoundHeader(
            round = state.round,
            totalRounds = state.amountOfRounds,
            playerNames = state.players.map { it.name },
            playerScores = state.players.map { it.score },
        )
        dealerSubtitle.text = getString(R.string.deal_subtitle, state.dealerName)
        cardCount.text = state.cardAmount.toString()
        cardsPerPlayerLabel.text =
            resources.getQuantityString(R.plurals.cards_per_player, state.cardAmount)
        dealNextButton.setOnClickListener {
            startActivity(Intent(this, DeclareTricksActivity::class.java))
        }
    }
}
