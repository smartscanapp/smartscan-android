package com.fpf.smartscan.ui.screens.concepts


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.R
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.action.ConceptItemsAction
import com.fpf.smartscan.ui.components.common.SelectionHeaderRow
import com.fpf.smartscan.ui.components.common.SlideRevealBox
import com.fpf.smartscan.ui.components.common.ActionBar
import com.fpf.smartscan.ui.action.ActionConfig
import com.fpf.smartscan.ui.components.concepts.ConceptItemsList
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.pickers.OptionPicker
import com.fpf.smartscan.ui.components.placeholders.EmptyItemsScreen
import kotlinx.coroutines.FlowPreview
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

    val actionBarHeight = 70

    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    val state by viewModel.state.collectAsState()
    val appSettings by appSettings.collectAsState()

    val conceptItems = viewModel.conceptItems.collectAsLazyPagingItems()
    
    // actions
//    var showMediaTypeFilter by remember { mutableStateOf(false) }

    val mainActions: List<ActionConfig> = listOf(
        ActionConfig(
            label = stringResource(R.string.share_action),
            onClick = { viewModel.onAction(ConceptItemsAction.ShareMedia(context)) },
            icon=Icons.Filled.Share
        ),
        ActionConfig(
            label = stringResource(R.string.copy_to_clipboard_action),
            onClick = { viewModel.onAction(ConceptItemsAction.CopyMedia(clipboard, context)) },
            enabled = state.selection.selectedItems.size == 1 && state.selection.selectedItems.first().type == MediaType.IMAGE
        ),
    )

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
//                actions = {
//                    IconButton(
//                        onClick = {showMediaTypeFilter = true}
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.FilterList,
//                            contentDescription = null
//                        )
//                    }
//                }
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
            SlideRevealBox(
                isVisible = state.selection.isSelecting,
                reverse = true,
                offsetPx = offset,
                modifier = Modifier
                    .zIndex(1f)
                    .heightIn(max = maxCollapsablePx.dp)
                    .padding(bottom = 8.dp)
            ) {
                SelectionHeaderRow (
                    selectedCount = state.selection.selectedCount,
                    checked = state.selection.selectAll && state.selection.excludedItems.isEmpty(),
                    onSelectAllChange = {viewModel.onAction(ConceptItemsAction.SetSelectAll(it))}
                )
            }
            ConceptItemsList(
                isVisible = conceptItems.itemCount > 0,
                items = conceptItems,
                isSelecting = state.selection.isSelecting,
                selectAll = state.selection.selectAll,
                excludedItems = state.selection.excludedItems,
                selectedItems = state.selection.selectedItems,
                onViewItem = { uri -> viewModel.onAction(ConceptItemsAction.SetMediaToView(context, uri, appSettings.enableDirectGalleryOpen)) },
                onToggleSelected = { viewModel.onAction(ConceptItemsAction.ToggleSelectedMedia(it)) },
                onToggleSelectionMode = {
                    viewModel.onAction(ConceptItemsAction.ToggleSelectionMode)
                    offset = 0
                },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsablePx,
//                onError = viewModel::onErrorAsyncImage
            )

            EmptyItemsScreen(
                isVisible = conceptItems.itemCount == 0
            )
        }

        SlideRevealBox(
            isVisible = state.selection.isSelecting && state.selection.selectedCount > 0,
            offsetPx = offset,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .then(
                    if (offset != 0)
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}
                    else Modifier
                )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionBar(
                    actions = mainActions,
                    modifier = Modifier.height(actionBarHeight.dp),
                )
            }
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

//    OptionPicker(
//        isVisible = showMediaTypeFilter,
//        title = stringResource(R.string.media_type_title),
//        options =  listOf("All") + mediaTypeOptions.values.toList(),
//        selectedOption  = mediaTypeOptions[state.mediaType]?: "All",
//        onSelect = { selected ->
//            val mediaType = mediaTypeOptions.entries.find { it.value == selected }?.key
//            viewModel.onAction(ConceptItemsAction.SetMediaTypeFilter(mediaType))
//            showMediaTypeFilter = false
//        },
//        onClose = {showMediaTypeFilter = false}
//    )

}