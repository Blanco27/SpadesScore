package com.nwe.spadesscore

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.color.MaterialColors
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.names.PlayerNamesViewModel
import com.nwe.spadesscore.ui.names.StartGameResult

class PlayerNamesActivity : SpadesAppCompatActivity() {

    private val viewModel: PlayerNamesViewModel by viewModels {
        viewModelFactory { initializer { PlayerNamesViewModel(gameRepository) } }
    }

    private lateinit var player1NameInput: EditText
    private lateinit var player2NameInput: EditText
    private lateinit var player3NameInput: EditText
    private lateinit var player4NameInput: EditText
    private lateinit var randomDealerSwitch: SwitchMaterial

    override fun initContentView() {
        setContentView(R.layout.activity_player_names)
    }

    override fun initializeUIComponents() {
        player1NameInput = findViewById(R.id.player1_name_input)
        player2NameInput = findViewById(R.id.player2_name_input)
        player3NameInput = findViewById(R.id.player3_name_input)
        player4NameInput = findViewById(R.id.player4_name_input)
        randomDealerSwitch = findViewById(R.id.random_dealer_checkBox)
    }

    override fun setupUI() {
        if (viewModel.playerCount == 3) {
            findViewById<View>(R.id.player4_card).visibility = View.GONE
        }

        findViewById<View>(R.id.start_game_button).setOnClickListener { startGame() }

        applySwitchTints()
        applyFieldFocusListeners()
    }

    // ── Switch tints ─────────────────────────────────────────────────────────

    private fun applySwitchTints() {
        val accentColor = MaterialColors.getColor(randomDealerSwitch, R.attr.appAccent)
        val trackOffColor = MaterialColors.getColor(randomDealerSwitch, R.attr.appTrack)
        randomDealerSwitch.trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accentColor, trackOffColor)
        )
        randomDealerSwitch.thumbTintList = ColorStateList.valueOf(Color.WHITE)
    }

    // ── Field focus highlight (bg_field ↔ bg_field_focus) ────────────────────

    private fun applyFieldFocusListeners() {
        val pairs = listOf(
            R.id.player1_card to player1NameInput,
            R.id.player2_card to player2NameInput,
            R.id.player3_card to player3NameInput,
            R.id.player4_card to player4NameInput
        )
        for ((cardId, editText) in pairs) {
            val card = findViewById<View>(cardId)
            editText.setOnFocusChangeListener { _, hasFocus ->
                card.setBackgroundResource(
                    if (hasFocus) R.drawable.bg_field_focus else R.drawable.bg_field
                )
            }
        }
    }

    // ── Text extraction ───────────────────────────────────────────────────────

    private fun nameOf(input: EditText): String = input.text?.toString().orEmpty()

    // ── Game start ────────────────────────────────────────────────────────────

    private fun startGame() {
        val names = buildList {
            add(nameOf(player1NameInput))
            add(nameOf(player2NameInput))
            add(nameOf(player3NameInput))
            if (viewModel.playerCount == 4) add(nameOf(player4NameInput))
        }
        when (viewModel.start(names, randomDealerSwitch.isChecked)) {
            StartGameResult.EmptyName ->
                showWarning(R.string.dialog_empty_name_title, R.string.dialog_empty_name_message)
            StartGameResult.NameTooLong ->
                showWarning(R.string.dialog_name_too_long_title, R.string.dialog_name_too_long_message)
            StartGameResult.Started ->
                startActivity(Intent(this, DealCardsActivity::class.java))
        }
    }

    private fun showWarning(@StringRes titleRes: Int, @StringRes messageRes: Int) {
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .create()
            .show()
    }
}
