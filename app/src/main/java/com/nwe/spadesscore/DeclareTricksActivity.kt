package com.nwe.spadesscore

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.declare.DeclareTricksUiState
import com.nwe.spadesscore.ui.declare.DeclareTricksViewModel
import com.nwe.spadesscore.ui.gameRepository

class DeclareTricksActivity : SpadesAppCompatActivity() {

    private val viewModel: DeclareTricksViewModel by viewModels {
        viewModelFactory { initializer { DeclareTricksViewModel(gameRepository) } }
    }

    private lateinit var roundTextView: TextView
    private lateinit var combinedTricksTextView: TextView
    private lateinit var scoreViews: List<TextView>
    private lateinit var nameViews: List<TextView>
    private lateinit var spinners: List<TrickSpinner>
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button

    private var colorActive = 0
    private var colorDeactive = 0
    private var warningColor = 0
    private var defaultColor = 0
    private var lastTricksAreValid = true
    private var playerCount = 4
    private var cardAmount = 0

    override fun initContentView() {
        setContentView(R.layout.activity_declare_tricks)
        colorActive = ContextCompat.getColor(this, R.color.background_button_enabled)
        colorDeactive = ContextCompat.getColor(this, R.color.background_button_disabled)
        warningColor = ContextCompat.getColor(this, R.color.warningText)
    }

    override fun initializeUIComponents() {
        roundTextView = findViewById(R.id.round_TextView)
        progressBar = findViewById(R.id.progressBar)
        combinedTricksTextView = findViewById(R.id.combined_tricks_textView)
        scoreViews = listOf(
            findViewById(R.id.current_score_player1_text_view),
            findViewById(R.id.current_score_player2_text_view),
            findViewById(R.id.current_score_player3_text_view),
            findViewById(R.id.current_score_player4_text_view),
        )
        nameViews = listOf(
            findViewById(R.id.player1_name_text_view),
            findViewById(R.id.player2_name_text_view),
            findViewById(R.id.player3_name_text_view),
            findViewById(R.id.player4_name_text_view),
        )
        spinners = listOf(
            findViewById(R.id.spinner1),
            findViewById(R.id.spinner2),
            findViewById(R.id.spinner3),
            findViewById(R.id.spinner4),
        )
        startButton = findViewById(R.id.start_Button)

        val state = viewModel.uiState()
        cardAmount = state.cardAmount
        playerCount = state.players.size
        spinners.forEach { it.setMaxAmount(state.cardAmount) }
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        if (state.players.size == 3) {
            scoreViews[3].visibility = View.GONE
            findViewById<View>(R.id.spinner4).visibility = View.GONE
            findViewById<View>(R.id.player4_layout).visibility = View.GONE
        }

        progressBar.max = state.amountOfRounds
        progressBar.progress = state.round

        (startButton.background as GradientDrawable).setColor(colorActive)
        defaultColor = combinedTricksTextView.textColors.defaultColor
        startButton.setOnClickListener { confirmTricks() }

        spinners.forEach { spinner ->
            spinner.setOnValueChangedListener { updateCombinedTricksTextView() }
        }

        render(state)
    }

    private fun render(state: DeclareTricksUiState) {
        roundTextView.text = getString(R.string.round, state.round)
        updateCombinedTricksTextView()
        state.players.forEachIndexed { index, player ->
            scoreViews[index].text = "${player.name}: ${player.score}"
            nameViews[index].text = player.name
        }
    }

    private fun updateCombinedTricksTextView() {
        val values = spinners.map { it.value }
        val combined = values[0] + values[1] + values[2] + if (playerCount == 4) values[3] else 0
        val possible = cardAmount

        combinedTricksTextView.text = getString(
            R.string.combined_trick_prediction, combined, possible, getString(R.string.tricks),
        )

        val isValid = combined != possible
        if (!isValid) {
            combinedTricksTextView.setTextColor(warningColor)
            startButton.isEnabled = false
            if (lastTricksAreValid) {
                animateFill(startButton, colorActive, colorDeactive)
                shakeView(startButton)
            }
        } else {
            combinedTricksTextView.setTextColor(defaultColor)
            startButton.isEnabled = true
            if (!lastTricksAreValid) {
                animateFill(startButton, colorDeactive, colorActive)
            }
        }
        lastTricksAreValid = isValid
    }

    private fun animateFill(view: View, fromColor: Int, toColor: Int) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = 250
            addUpdateListener { animation ->
                (view.background as GradientDrawable).setColor(animation.animatedValue as Int)
            }
            start()
        }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, 16f, -16f, 12f, -12f, 6f, -6f, 0f).apply {
            duration = 350
            start()
        }
    }

    private fun confirmTricks() {
        viewModel.setPredictions(spinners.map { it.value })
        startActivity(Intent(this, ConfirmTicksActivity::class.java))
    }
}
