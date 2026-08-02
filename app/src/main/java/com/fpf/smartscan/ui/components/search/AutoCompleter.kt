package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun AutoCompleter(
    isVisible: Boolean,
    autoCompleteResults: List<String>,
    query: String,
    onSelect:(String) -> Unit,
    label: String? = null
){
    if (!isVisible) return

    val cleanedQuery = if (query.startsWith("#") && query.length > 1) query.substring(1) else query

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        label?.let { item { Text(it, style = MaterialTheme.typography.labelLarge) } }
        items(autoCompleteResults) { result ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )

                val annotated = buildAnnotatedString {
                    if (cleanedQuery.isEmpty()) {
                        append(result)
                    } else {
                        val lowerTag = result.lowercase()
                        val lowerQuery = cleanedQuery.lowercase()
                        if (lowerTag.startsWith(lowerQuery)) {
                            val matchLength = lowerQuery.length.coerceAtMost(result.length)
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append(result.substring(0, matchLength))
                            }
                            if (matchLength < result.length) {
                                append(result.substring(matchLength))
                            }
                        } else {
                            append(result)
                        }
                    }
                }

                TextButton({ onSelect(annotated.toString()) }){
                    Text(annotated, color = MaterialTheme.colorScheme.onSurface,  style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
