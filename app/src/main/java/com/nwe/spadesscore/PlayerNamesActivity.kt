package com.nwe.spadesscore

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.CheckBox
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.textfield.TextInputLayout
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.names.PlayerNamesViewModel
import com.nwe.spadesscore.ui.names.StartGameResult

class PlayerNamesActivity : SpadesAppCompatActivity() {

    private val viewModel: PlayerNamesViewModel by viewModels {
        viewModelFactory { initializer { PlayerNamesViewModel(gameRepository) } }
    }

    private lateinit var player1NameInput: TextInputLayout
    private lateinit var player2NameInput: TextInputLayout
    private lateinit var player3NameInput: TextInputLayout
    private lateinit var player4NameInput: TextInputLayout
    private lateinit var randomDealerCheckBox: CheckBox

    override fun initContentView() {
        setContentView(R.layout.activity_player_names)
    }

    override fun initializeUIComponents() {
        player1NameInput = findViewById(R.id.player1_name_input)
        player2NameInput = findViewById(R.id.player2_name_input)
        player3NameInput = findViewById(R.id.player3_name_input)
        player4NameInput = findViewById(R.id.player4_name_input)
        randomDealerCheckBox = findViewById(R.id.random_dealer_checkBox)
    }

    override fun setupUI() {
        if (viewModel.playerCount == 3) {
            player4NameInput.visibility = View.GONE
            findViewById<View>(R.id.player4_space).visibility = View.GONE
        }
        findViewById<View>(R.id.start_game_button).setOnClickListener { startGame() }
    }

    private fun nameOf(input: TextInputLayout): String = input.editText?.text?.toString().orEmpty()

    private fun startGame() {
        val names = buildList {
            add(nameOf(player1NameInput))
            add(nameOf(player2NameInput))
            add(nameOf(player3NameInput))
            if (viewModel.playerCount == 4) add(nameOf(player4NameInput))
        }
        when (viewModel.start(names, randomDealerCheckBox.isChecked)) {
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
