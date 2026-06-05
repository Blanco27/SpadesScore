package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.domain.SpadesEngine
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun ResultScreen(
    game: GameState,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roundsPlayed = game.scores[0].size - 1
    val ranking = SpadesEngine.rankingForLastRound(game) // Indizes höchster Score zuerst
    val isFinal = game.secondHalf

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Kopfzeile: Spielernamen
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(32.dp))
            for (p in 0 until game.playerCount) {
                Text(
                    game.playerNames[p],
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for (round in 1..roundsPlayed) {
                val isLast = round == roundsPlayed
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        round.toString(),
                        modifier = Modifier.width(32.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (p in 0 until game.playerCount) {
                        val value = game.scores[p][round]
                        val place = ranking.indexOf(p)
                        if (isLast && place < 3) {
                            Box(
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                    .background(medalColor(place), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    value.toString(),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF1A1C18)
                                )
                            }
                        } else {
                            Text(
                                value.toString(),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = if (isLast) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = LocalContentColor.current
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (isFinal) {
            Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.new_game), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.continue_game), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Gold/Silber/Bronze als gut lesbarer Hintergrund-Chip (Verfeinerung der Spec-Farben fürs helle Theme). */
private fun medalColor(place: Int): Color = when (place) {
    0 -> Color(0xFFFFD700) // Gold
    1 -> Color(0xFFC0C0C0) // Silber
    else -> Color(0xFFCD7F32) // Bronze
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    SpadesScoreTheme {
        ResultScreen(game = previewGameState(), onContinue = {}, onNewGame = {})
    }
}
