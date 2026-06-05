package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun ConfirmTricksScreen(
    game: GameState,
    onDone: (made: List<Boolean>) -> Unit,
    modifier: Modifier = Modifier
) {
    val made = remember(game) { mutableStateListOf<Boolean>().apply { repeat(game.playerCount) { add(false) } } }
    if (game.tickPredictions.size < game.playerCount) return

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.round, game.currentRound), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { game.currentRound.toFloat() / game.amountOfRounds },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            for (i in 0 until game.playerCount) {
                val prediction = game.tickPredictions[i]
                val tricksWord = stringResource(if (prediction == 1) R.string.trick else R.string.tricks)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.playerNames[i], style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$prediction $tricksWord",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.points_added, prediction + 5),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (made[i]) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(checked = made[i], onCheckedChange = { made[i] = it })
                    }
                }
            }
        }
        Button(
            onClick = { onDone(made.toList()) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.done), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmTricksScreenPreview() {
    SpadesScoreTheme {
        ConfirmTricksScreen(game = previewGameState(), onDone = {})
    }
}
