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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun PlayerNamesScreen(
    playerCount: Int,
    onStart: (names: List<String>, randomDealer: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val names = remember(playerCount) { mutableStateListOf<String>().apply { repeat(playerCount) { add("") } } }
    val errors = remember(playerCount) { mutableStateListOf<Int?>().apply { repeat(playerCount) { add(null) } } }
    var randomDealer by remember(playerCount) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.set_player_names), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            for (i in 0 until playerCount) {
                OutlinedTextField(
                    value = names[i],
                    onValueChange = { names[i] = it; errors[i] = null },
                    label = { Text(stringResource(playerLabelRes(i))) },
                    singleLine = true,
                    isError = errors[i] != null,
                    supportingText = { errors[i]?.let { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Checkbox(checked = randomDealer, onCheckedChange = { randomDealer = it })
                Text(stringResource(R.string.random_first_dealer))
            }
        }
        Button(
            onClick = {
                var ok = true
                for (i in 0 until playerCount) {
                    val name = names[i].trim()
                    when {
                        name.isEmpty() -> { errors[i] = R.string.name_empty_error; ok = false }
                        name.length > 10 -> { errors[i] = R.string.name_too_long_error; ok = false }
                        else -> errors[i] = null
                    }
                }
                if (ok) onStart((0 until playerCount).map { names[it].trim() }, randomDealer)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.next), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun playerLabelRes(index: Int): Int = when (index) {
    0 -> R.string.player1_name
    1 -> R.string.player2_name
    2 -> R.string.player3_name
    else -> R.string.player4_name
}

@Preview(showBackground = true)
@Composable
private fun PlayerNamesScreenPreview() {
    SpadesScoreTheme {
        PlayerNamesScreen(playerCount = 4, onStart = { _, _ -> })
    }
}
