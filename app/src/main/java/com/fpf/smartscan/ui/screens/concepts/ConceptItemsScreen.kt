package com.fpf.smartscan.ui.screens.concepts


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.action.ConceptItemsAction
import com.fpf.smartscan.ui.components.concepts.ConceptItemsList
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.placeholders.EmptyItemsScreen
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConceptItemsScreen(
    appSettings: StateFlow<AppSettings>,
    concept: Concept?,
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit,
    viewModel: ConceptItemsViewModel = koinViewModel(),
) {
    if(concept == null) return

    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val appSettings by appSettings.collectAsState()

    val conceptItems = viewModel.conceptItems.collectAsLazyPagingItems()

    // For dynamic smooth hiding effect of action bars and other components
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()
    val screenTitle = ""

    LaunchedEffect(concept) {
        viewModel.onAction(ConceptItemsAction.SetConceptToView(concept))
    }

    LaunchedEffect(Unit) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
            )
        )
    }


    BackHandler(enabled = state.selection.isSelecting) {
        viewModel.onAction(ConceptItemsAction.ResetSelection)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            ConceptItemsList(
                isVisible = conceptItems.itemCount > 0,
                items = conceptItems,
                onItemClick = { viewModel.onAction(ConceptItemsAction.SetMediaToView(context, it, appSettings.enableDirectGalleryOpen)) },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsablePx,
//                onError = viewModel::onErrorAsyncImage
            )

            EmptyItemsScreen(
                isVisible = conceptItems.itemCount == 0
            )
        }

        state.mediaToView?.let { item ->
            val mediaItems by remember {
                derivedStateOf {
                    List(conceptItems.itemCount) { index -> conceptItems[index] }.filterNotNull()
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(500)
                ),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(300)
                )
            ) {
                MediaViewer(
                    items = mediaItems,
                    initialIndex = mediaItems.indexOf(item),
                    onClose = { viewModel.onAction(ConceptItemsAction.SetMediaToView(context, null))},
                    onUpdateSearchImage = null,
                    onLoadMore = { val lastIndex = (conceptItems.itemCount - 1).coerceAtLeast(0)
                        conceptItems[lastIndex]}
                )
            }
        }
    }

}