package com.nwe.spadesscore

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.color.MaterialColors
import com.nwe.spadesscore.ui.bindRoundHeader
import com.nwe.spadesscore.ui.declare.DeclareTricksUiState
import com.nwe.spadesscore.ui.declare.DeclareTricksViewModel
import com.nwe.spadesscore.ui.gameRepository

class DeclareTricksActivity : SpadesAppCompatActivity() {

    private val viewModel: DeclareTricksViewModel by viewModels {
        viewModelFactory { initializer { DeclareTricksViewModel(gameRepository) } }
    }

    private lateinit var sumChip: TextView
    private lateinit var warnSubtitle: TextView
    private lateinit var nameViews: List<TextView>
    private lateinit var spinners: List<TrickSpinner>
    private lateinit var startButton: Button

    // Colors resolved from theme in initializeUIComponents(); used for lock/unlock transitions.
    private var colorActive = 0      // appCtaBg   — CTA background when enabled
    private var colorDeactive = 0    // appStepBg  — CTA background when locked
    private var accentColor = 0      // appAccent  — chip text color (normal)
    private var warnColor = 0        // appWarn    — chip + warn-subtitle text color (locked)
    private var ctaFgColor = 0       // appCtaFg   — CTA text color when enabled
    private var ctaMutedColor = 0    // appMuted   — CTA text color when locked

    private var lastTricksAreValid = true
    private var playerCount = 4
    private var cardAmount = 0

    // ── SpadesAppCompatActivity hooks ─────────────────────────────────────────

    override fun initContentView() {
        setContentView(R.layout.activity_declare_tricks)
    }

    override fun initializeUIComponents() {
        sumChip = findViewById(R.id.sum_chip)
        warnSubtitle = findViewById(R.id.warn_subtitle)
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

        // Resolve design-system theme colors for animated lock/unlock transitions.
        colorActive   = MaterialColors.getColor(startButton, R.attr.appCtaBg)
        colorDeactive = MaterialColors.getColor(startButton, R.attr.appStepBg)
        accentColor   = MaterialColors.getColor(sumChip, R.attr.appAccent)
        warnColor     = MaterialColors.getColor(sumChip, R.attr.appWarn)
        ctaFgColor    = MaterialColors.getColor(startButton, R.attr.appCtaFg)
        ctaMutedColor = MaterialColors.getColor(startButton, R.attr.appMuted)

        val state = viewModel.uiState()
        cardAmount = state.cardAmount
        playerCount = state.players.size
        spinners.forEach { it.setMaxAmount(state.cardAmount) }
    }

    override fun setupUI() {
        val state = viewModel.uiState()

        // Hide player-4 row for 3-player games (spinner4 is inside player4_layout).
        if (state.players.size == 3) {
            findViewById<View>(R.id.player4_layout).visibility = View.GONE
        }

        // Set initial CTA appearance (bg_cta is a GradientDrawable; we animate its color later).
        (startButton.background as GradientDrawable).setColor(colorActive)
        startButton.setTextColor(ctaFgColor)
        startButton.setOnClickListener { confirmTricks() }

        // Re-evaluate lock state on every spinner change.
        spinners.forEach { spinner ->
            spinner.setOnValueChangedListener { updateCombinedTricksTextView() }
        }

        render(state)
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private fun render(state: DeclareTricksUiState) {
        bindRoundHeader(
            round = state.round,
            totalRounds = state.amountOfRounds,
            playerNames = state.players.map { it.name },
            playerScores = state.players.map { it.score },
        )
        state.players.forEachIndexed { index, player ->
            nameViews[index].text = player.name
        }
        updateCombinedTricksTextView()
    }

    /**
     * Recomputes the combined prediction and drives ALL lock-related visuals in one place:
     *  - chip text + background + text-color
     *  - warn subtitle visibility + text
     *  - CTA enabled state + background color (animated) + text-color
     *  - shake animation on transition to locked
     */
    private fun updateCombinedTricksTextView() {
        val values = spinners.map { it.value }
        val combined = values[0] + values[1] + values[2] + if (playerCount == 4) values[3] else 0
        val possible = cardAmount

        // Update chip text regardless of state.
        sumChip.text = getString(
            R.string.combined_trick_prediction, combined, possible, getString(R.string.tricks),
        )

        val isValid = combined != possible
        if (!isValid) {
            // ── LOCKED: sum equals available tricks — bids must differ ──
            sumChip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_warn)
            sumChip.setTextColor(warnColor)
            warnSubtitle.text = getString(R.string.tricks_sum_warning, cardAmount)
            warnSubtitle.visibility = View.VISIBLE
            startButton.isEnabled = false
            startButton.setTextColor(ctaMutedColor)
            if (lastTricksAreValid) {
                // Transition INTO locked: animate CTA background + shake.
                animateFill(startButton, colorActive, colorDeactive)
                shakeView(startButton)
            }
        } else {
            // ── OK: bids differ from tricks — play is allowed ──
            sumChip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip)
            sumChip.setTextColor(accentColor)
            warnSubtitle.visibility = View.GONE
            startButton.isEnabled = true
            startButton.setTextColor(ctaFgColor)
            if (!lastTricksAreValid) {
                // Transition OUT of locked: animate CTA background back.
                animateFill(startButton, colorDeactive, colorActive)
            }
        }
        lastTricksAreValid = isValid
    }

    // ── Animation helpers ──────────────────────────────────────────────────────

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

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun confirmTricks() {
        viewModel.setPredictions(spinners.map { it.value })
        startActivity(Intent(this, ConfirmTicksActivity::class.java))
    }
}
