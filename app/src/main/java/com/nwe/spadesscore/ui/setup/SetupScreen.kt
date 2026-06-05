package com.nwe.spadesscore.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.Languages
import com.nwe.spadesscore.R
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun SetupScreen(
    playerCount: Int,
    language: Languages,
    onSelectPlayerCount: (Int) -> Unit,
    onSelectLanguage: (Languages) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.spades_logo),
            contentDescription = stringResource(R.string.contentDescription),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
        Spacer(Modifier.weight(1f))

        SegmentedSelector(
            label = stringResource(R.string.number_of_players),
            options = listOf(
                3 to stringResource(R.string.three),
                4 to stringResource(R.string.four)
            ),
            selected = playerCount,
            onSelect = onSelectPlayerCount
        )
        Spacer(Modifier.height(24.dp))
        SegmentedSelector(
            label = stringResource(R.string.language),
            options = listOf(
                Languages.ENGLISH to stringResource(R.string.english),
                Languages.GERMAN to stringResource(R.string.deutsch)
            ),
            selected = language,
            onSelect = onSelectLanguage
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(R.string.next),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun <T> SegmentedSelector(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(text) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    SpadesScoreTheme {
        SetupScreen(
            playerCount = 4,
            language = Languages.ENGLISH,
            onSelectPlayerCount = {},
            onSelectLanguage = {},
            onNext = {}
        )
    }
}
