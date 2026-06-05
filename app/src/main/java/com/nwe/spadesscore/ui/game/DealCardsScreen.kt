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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun DealCardsScreen(
    game: GameState,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.player_must_deal_cards, game.playerNames[game.currentPlayer]),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${SpadesEngine.amountOfCards(game)}x",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            for (i in 0 until game.playerCount) {
                ScoreCard(name = game.playerNames[i], score = game.scores[i].last())
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.start), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ScoreCard(name: String, score: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(score.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DealCardsScreenPreview() {
    SpadesScoreTheme {
        DealCardsScreen(game = previewGameState(), onNext = {})
    }
}
