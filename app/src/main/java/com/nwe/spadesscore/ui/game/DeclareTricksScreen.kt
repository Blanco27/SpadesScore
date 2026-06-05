package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.nwe.spadesscore.domain.SpadesEngine
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun DeclareTricksScreen(
    game: GameState,
    onConfirm: (predictions: List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val predictions = remember(game) { mutableStateListOf<Int>().apply { repeat(game.playerCount) { add(0) } } }
    val possible = SpadesEngine.amountOfCards(game)
    val sum = predictions.sum()
    val valid = sum != possible
    val tricksWord = stringResource(if (possible == 1) R.string.trick else R.string.tricks)

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
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(game.playerNames[i], style = MaterialTheme.typography.titleMedium)
                            Text(
                                game.scores[i].last().toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Stepper(value = predictions[i], onValueChange = { predictions[i] = it }, max = possible)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = if (valid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.combined_trick_prediction, sum, possible, tricksWord),
                    modifier = Modifier.padding(12.dp),
                    color = if (valid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Button(
            onClick = { onConfirm(predictions.toList()) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.confirm_ticks), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeclareTricksScreenPreview() {
    SpadesScoreTheme {
        DeclareTricksScreen(game = previewGameState(), onConfirm = {})
    }
}
