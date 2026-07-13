package com.fpf.smartscan.ui.screens.concepts


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R

@Composable
fun EmptyConceptsScreen(
    isVisible: Boolean,
    isMainScanRequired: Boolean,
) {
    if (!isVisible) return

    val steps = listOf(
        stringResource(R.string.concepts_step_select_collections),
        stringResource(R.string.concepts_step_generate_summaries)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = "Lightbulb icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )

            Text(
                text = stringResource(R.string.concepts_empty_screen_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier
                    .padding(bottom = 32.dp)
            )
            if( isMainScanRequired){
                Text(text = stringResource(R.string.alert_initial_scan_required), color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }

            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.concepts_empty_screen_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index+1}. $step",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = stringResource(R.string.concepts_required_api_key_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
