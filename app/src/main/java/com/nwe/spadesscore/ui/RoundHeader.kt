package com.nwe.spadesscore.ui

import android.app.Activity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.nwe.spadesscore.R

/**
 * Binds the shared `include_round_header` block (brand row + per-player score row +
 * round title + progress bar). Decoupled from any screen's UiState — callers pass the
 * round, total rounds, and parallel name/score lists. Columns beyond the player count
 * are hidden, so the same header serves 3- and 4-player games.
 */
fun Activity.bindRoundHeader(
    round: Int,
    totalRounds: Int,
    playerNames: List<String>,
    playerScores: List<Int>,
) {
    findViewById<TextView>(R.id.round_progress).text =
        getString(R.string.round_progress, round, totalRounds)

    val cols = listOf(R.id.score_col_1, R.id.score_col_2, R.id.score_col_3, R.id.score_col_4)
    val names = listOf(R.id.score_name_1, R.id.score_name_2, R.id.score_name_3, R.id.score_name_4)
    val values = listOf(R.id.score_value_1, R.id.score_value_2, R.id.score_value_3, R.id.score_value_4)
    cols.indices.forEach { i ->
        if (i < playerNames.size) {
            findViewById<View>(cols[i]).visibility = View.VISIBLE
            findViewById<TextView>(names[i]).text = playerNames[i]
            findViewById<TextView>(values[i]).text = playerScores[i].toString()
        } else {
            findViewById<View>(cols[i]).visibility = View.GONE
        }
    }

    val roundTitle = findViewById<TextView>(R.id.round_title)
    val title = getString(R.string.round, round)
    val firstDigit = title.indexOfFirst { it.isDigit() }
    roundTitle.text = if (firstDigit >= 0) {
        SpannableString(title).apply {
            val accent = MaterialColors.getColor(roundTitle, R.attr.appAccent)
            setSpan(ForegroundColorSpan(accent), firstDigit, title.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    } else {
        title
    }

    findViewById<ProgressBar>(R.id.round_progress_bar).apply {
        max = totalRounds
        progress = round
    }
}
