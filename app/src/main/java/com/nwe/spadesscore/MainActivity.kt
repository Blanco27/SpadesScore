package com.nwe.spadesscore

import android.content.Intent
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.color.MaterialColors
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.main.MainViewModel

class MainActivity : SpadesAppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel(gameRepository) } }
    }

    private lateinit var btn3Players: TextView
    private lateinit var btn4Players: TextView
    private lateinit var startGameButton: Button
    private lateinit var settingsGear: ImageButton

    override fun initContentView() {
        setContentView(R.layout.activity_main)
    }

    override fun initializeUIComponents() {
        btn3Players = findViewById(R.id.btn3Players)
        btn4Players = findViewById(R.id.btn4Players)
        startGameButton = findViewById(R.id.start_game_button)
        settingsGear = findViewById(R.id.settings_gear)
    }

    override fun setupUI() {
        renderPlayerCount(viewModel.uiState().playerCount)

        btn3Players.setOnClickListener {
            if (viewModel.uiState().playerCount == 3) return@setOnClickListener
            viewModel.setPlayerCount(3)
            renderPlayerCount(3)
        }
        btn4Players.setOnClickListener {
            if (viewModel.uiState().playerCount == 4) return@setOnClickListener
            viewModel.setPlayerCount(4)
            renderPlayerCount(4)
        }
        settingsGear.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        startGameButton.setOnClickListener {
            startActivity(Intent(this, PlayerNamesActivity::class.java))
        }
    }

    private fun renderPlayerCount(count: Int) {
        val accentColor = MaterialColors.getColor(btn3Players, R.attr.appAccent)
        val mutedColor = MaterialColors.getColor(btn3Players, R.attr.appMuted)
        val activeElevation = 4f * resources.displayMetrics.density

        val (active, inactive) = if (count == 3) btn3Players to btn4Players
                                 else btn4Players to btn3Players

        active.setBackgroundResource(R.drawable.bg_segment_selected)
        active.setTextColor(accentColor)
        active.elevation = activeElevation

        inactive.setBackgroundResource(0)
        inactive.setTextColor(mutedColor)
        inactive.elevation = 0f
    }
}
