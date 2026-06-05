package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Zustandsloser −/Wert/+ Stepper; klemmt auf [min]..[max]. Ersetzt die alte TrickSpinner-View. */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    max: Int,
    modifier: Modifier = Modifier,
    min: Int = 0
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(onClick = { if (value > min) onValueChange(value - 1) }, enabled = value > min) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp).padding(horizontal = 8.dp)
        )
        OutlinedIconButton(onClick = { if (value < max) onValueChange(value + 1) }, enabled = value < max) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
