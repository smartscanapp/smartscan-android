package com.fpf.smartscan.ui.components.search

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.ui.components.common.LoadingIndicator

@Composable
fun MultiModalSearcher(
    searchBarFieldState: TextFieldState,
    queryImage: Uri?,
    mediaType: MediaType,
    loading: Boolean,
    searchEnabled: Boolean,
    searchBarPlaceholders: List<String> = emptyList(),
    onPasteImage: (Uri) -> Unit,
    onSearch: () -> Unit,
    onUploadImage: () -> Unit,
    onRemoveUploadedImage: () -> Unit,
    onClearResults: () -> Unit,
    onFocusedChange: ((focused: Boolean) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (queryImage != null) {
            ImageSearcher(
                uri = queryImage,
                mediaType = mediaType,
                imageSize = 110.dp,
                loading = loading,
                onSearch = onSearch,
                onRemoveImage = onRemoveUploadedImage
            )
        } else {
            SearchBar(
                searchFieldState = searchBarFieldState,
                enabled = searchEnabled,
                placeholders = searchBarPlaceholders,
                onSearch = onSearch,
                onPasteImage = onPasteImage,
                onClearResults = onClearResults,
                onFocusedChange = onFocusedChange,
                leadingIcon = {
                    IconButton(
                        onClick = { onUploadImage() },
                        enabled = searchEnabled,
                        modifier = Modifier
                            .padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Upload image",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (searchEnabled) 1f else 0.3f)
                        )
                    }
                },
                trailingIcon = if (loading) {
                    {
                        LoadingIndicator(
                            isVisible = true,
                            strokeWidth = 2.dp,
                        )
                    }
                } else null
            )
        }
    }
}