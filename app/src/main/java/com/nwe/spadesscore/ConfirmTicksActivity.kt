package com.nwe.spadesscore

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.color.MaterialColors
import com.nwe.spadesscore.ui.animateFill
import com.nwe.spadesscore.ui.bindRoundHeader
import com.nwe.spadesscore.ui.confirm.ConfirmTicksUiState
import com.nwe.spadesscore.ui.confirm.ConfirmTicksViewModel
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.shake
import java.util.Locale

class ConfirmTicksActivity : SpadesAppCompatActivity() {

    private val viewModel: ConfirmTicksViewModel by viewModels {
        viewModelFactory { initializer { ConfirmTicksViewModel(gameRepository) } }
    }

    private lateinit var rowLayouts: List<LinearLayout>
    private lateinit var nameViews: List<TextView>
    private lateinit var tricksViews: List<TextView>
    private lateinit var pointsViews: List<TextView>
    private lateinit var checkViews: List<TextView>

    /** Per-player hit state — toggled by tapping a row or its check button. */
    private val hitStates = BooleanArray(4) { false }

    private lateinit var startButton: Button
    private lateinit var warnSubtitle: TextView

    // Colors resolved from theme in initializeUIComponents(); used for lock/unlock transitions.
    private var colorActive = 0      // appCtaBg  — CTA background when enabled
    private var colorDeactive = 0    // appStepBg — CTA background when locked
    private var ctaFgColor = 0       // appCtaFg  — CTA text color when enabled
    private var ctaMutedColor = 0    // appMuted  — CTA text color when locked

    /** True while the current selection is allowed (at least one visible player is a miss). */
    private var lastStateValid = true

    override fun initContentView() {
        setContentView(R.layout.activity_confirm_tricks)
    }

    override fun initializeUIComponents() {
        rowLayouts = listOf(
            findViewById(R.id.player1_layout),
            findViewById(R.id.player2_layout),
            findViewById(R.id.player3_layout),
            findViewById(R.id.player4_layout),
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
        checkViews = listOf(
            findViewById(R.id.player1_check),
            findViewById(R.id.player2_check),
            findViewById(R.id.player3_check),
            findViewById(R.id.player4_check),
        )

        startButton = findViewById(R.id.start_Button)
        warnSubtitle = findViewById(R.id.warn_subtitle)

        colorActive   = MaterialColors.getColor(startButton, R.attr.appCtaBg)
        colorDeactive = MaterialColors.getColor(startButton, R.attr.appStepBg)
        ctaFgColor    = MaterialColors.getColor(startButton, R.attr.appCtaFg)
        ctaMutedColor = MaterialColors.getColor(startButton, R.attr.appMuted)
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        val playerCount = state.players.size

        if (playerCount == 3) {
            rowLayouts[3].visibility = View.GONE
        }

        bindRoundHeader(
            round = state.round,
            totalRounds = state.amountOfRounds,
            playerNames = state.players.map { it.name },
            playerScores = state.players.map { it.score },
        )

        for (index in 0 until playerCount) {
            rowLayouts[index].setOnClickListener { toggleHit(index) }
            checkViews[index].setOnClickListener { toggleHit(index) }
        }

        render(state)

        (startButton.background as GradientDrawable).setColor(colorActive)
        startButton.setTextColor(ctaFgColor)
        startButton.setOnClickListener { startNextRound() }

        updateConfirmLockState()
    }

    private fun render(state: ConfirmTicksUiState) {
        state.players.forEachIndexed { index, player ->
            nameViews[index].text = player.name
            val trickWord =
                if (player.prediction == 1) getString(R.string.trick) else getString(R.string.tricks)
            tricksViews[index].text =
                String.format(Locale.getDefault(), "%d %s", player.prediction, trickWord)
            pointsViews[index].text = " · ${getString(R.string.points_added, player.pointsIfHit)}"
            applyHitVisuals(index, isHit = false)
        }
    }

    private fun toggleHit(index: Int) {
        hitStates[index] = !hitStates[index]
        applyHitVisuals(index, isHit = hitStates[index])
        updateConfirmLockState()
    }

    /**
     * Locks the "Next round" CTA when EVERY visible player is marked as a hit:
     * the trick-declaration rule guarantees not everyone can hit, so at least one
     * player must remain a miss. Drives CTA enabled-state, background (animated),
     * text color, the warn subtitle, and a shake on the transition into locked.
     */
    private fun updateConfirmLockState() {
        val playerCount = viewModel.uiState().players.size
        val allHit = (0 until playerCount).all { hitStates[it] }
        val isValid = !allHit

        if (!isValid) {
            // ── LOCKED: everyone is a hit — impossible, force at least one miss ──
            warnSubtitle.text = getString(R.string.confirm_all_hit_warning)
            warnSubtitle.visibility = View.VISIBLE
            startButton.isEnabled = false
            startButton.setTextColor(ctaMutedColor)
            if (lastStateValid) {
                animateFill(startButton, colorActive, colorDeactive)
                startButton.shake()
            }
        } else {
            // ── OK: at least one miss — round can be confirmed ──
            warnSubtitle.visibility = View.GONE
            startButton.isEnabled = true
            startButton.setTextColor(ctaFgColor)
            if (!lastStateValid) {
                animateFill(startButton, colorDeactive, colorActive)
            }
        }
        lastStateValid = isValid
    }

    /**
     * Applies hit or miss visuals to a player row:
     *  - Hit:  appHitBg row background (rounded), bg_check_on + "✓", points in appAccent.
     *  - Miss: bg_card row background, bg_check (empty), points in appMuted.
     */
    private fun applyHitVisuals(index: Int, isHit: Boolean) {
        val row = rowLayouts[index]
        val density = resources.displayMetrics.density

        if (isHit) {
            val hitColor = MaterialColors.getColor(row, R.attr.appHitBg)
            row.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * density
                setColor(hitColor)
            }
        } else {
            row.setBackgroundResource(R.drawable.bg_card)
        }

        checkViews[index].apply {
            setBackgroundResource(if (isHit) R.drawable.bg_check_on else R.drawable.bg_check)
            text = if (isHit) "✓" else ""
        }

        val accentColor = MaterialColors.getColor(row, R.attr.appAccent)
        val mutedColor = MaterialColors.getColor(row, R.attr.appMuted)
        pointsViews[index].setTextColor(if (isHit) accentColor else mutedColor)
    }

    private fun startNextRound() {
        val playerCount = viewModel.uiState().players.size
        val hits = (0 until playerCount).map { hitStates[it] }
        val gameOver = viewModel.confirm(hits)
        val next = if (gameOver) ResultScreenActivity::class.java else DealCardsActivity::class.java
        startActivity(Intent(this, next))
    }
}
