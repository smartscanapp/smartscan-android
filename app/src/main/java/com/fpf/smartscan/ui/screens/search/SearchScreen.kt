package com.fpf.smartscan.ui.screens.search

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.events.SearchEventType
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.search.IndexingStatus
import com.fpf.smartscan.search.QueryType
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.action.SearchAction
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.common.LoadingIndicator
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.common.ProgressBar
import com.fpf.smartscan.ui.components.common.SelectionHeaderRow
import com.fpf.smartscan.ui.components.common.SlideRevealBox
import com.fpf.smartscan.ui.components.pickers.DatePickerModal
import com.fpf.smartscan.ui.components.modals.BottomSheet
import com.fpf.smartscan.ui.components.search.AutoCompleter
import com.fpf.smartscan.ui.components.search.ImageSearcher
import com.fpf.smartscan.ui.components.search.SearchBar
import com.fpf.smartscan.ui.components.search.SearchResults
import com.fpf.smartscan.ui.components.TagAdder
import com.fpf.smartscan.ui.components.common.ActionBar
import com.fpf.smartscan.ui.action.ActionConfig
import com.fpf.smartscan.ui.components.pickers.OptionPicker
import com.fpf.smartscan.ui.permissions.RequestPermissions
import com.fpf.smartscan.ui.screens.search.SearchViewModel.Companion.RESULTS_BATCH_SIZE
import com.fpf.smartscan.utils.formatDate
import com.fpf.smartscan.utils.toEpochSeconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max


@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel = koinViewModel(),
    appSettings:  StateFlow<AppSettings>,
    onTopBarChange: (TopBarState) -> Unit,
    intentSearchQuery: SearchQuery? = null
) {
    val appSettings by appSettings.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current


    // Index state
    val imageIndexProgress by searchViewModel.imageIndexProgress.collectAsState(initial = 0f)
    val videoIndexProgress by searchViewModel.videoIndexProgress.collectAsState(initial = 0f)
    val imageIndexStatus by searchViewModel.imageIndexStatus.collectAsState()
    val videoIndexStatus by searchViewModel.videoIndexStatus.collectAsState()
    val isIndexing = imageIndexStatus == IndexingStatus.ACTIVE || videoIndexStatus == IndexingStatus.ACTIVE
    var showScanImagesDialog by remember { mutableStateOf(false) }
    var showScanVideosDialog by remember { mutableStateOf(false) }
    var showIndexAlert by remember { mutableStateOf(false) }


    // Search state
    val state by searchViewModel.state.collectAsState()
    val tags by searchViewModel.allTags.collectAsState()
    val searchBarPlaceholders = listOf(
        when (state.mediaType) {
            MediaType.IMAGE -> stringResource(R.string.placeholders_search_images)
            MediaType.VIDEO -> stringResource(R.string.placeholders_search_videos)
        },
       stringResource(R.string.placeholders_search_by_tag),
        stringResource(R.string.placeholders_search_by_tag_query),
    )

    var hasStoragePermission by remember { mutableStateOf(false) }
    var isAddingTag by remember { mutableStateOf(false) }
    var tagAutoCompleteTagResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSelectingMediaType by remember { mutableStateOf(false) }

    // action bar actions
    val actionBarActions: List<ActionConfig> = listOf(
        ActionConfig(
            label = stringResource(R.string.share_action),
            onClick = {
                searchViewModel.onAction(SearchAction.ShareResults(context))
            },
            icon = Icons.Filled.Share
        ),
        ActionConfig(
            label = stringResource(R.string.search_action),
            onClick = { searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(state.selection.selectedItems.first().uri, appSettings.imageSimilarityThreshold, appSettings.enableDedupe)) },
            enabled = state.selection.selectedItems.size == 1 && state.mediaType == MediaType.IMAGE,
            icon = Icons.Filled.Search
        ),
        ActionConfig(
            label = stringResource(R.string.copy_action),
            onClick = { searchViewModel.onAction(SearchAction.CopyResult(clipboard, context)) },
            icon = Icons.Filled.ContentCopy,
            enabled = state.selection.selectedItems.size == 1 && state.selection.selectedItems.first().type == MediaType.IMAGE
        ),
        ActionConfig(
            label = stringResource(R.string.add_tag_action),
            onClick = { isAddingTag = true },
            icon = Icons.Filled.Tag),
        )

    // Dynamic hide animation
    val isActionBarVisible =  state.selection.isSelecting && state.selection.selectedCount > 0
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val actionBarHeight = with(density) { 70.dp.toPx() }
    val searchBarHeight = with(density) { (if(state.queryType == QueryType.IMAGE) 200 else 120).dp.toPx() }
    val maxCollapsePx = max(actionBarHeight, searchBarHeight).toInt()

    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Menu
    var showMenu by remember { mutableStateOf(false) }

    val menuActions: List<MenuActionConfig> = listOf(
        MenuActionConfig.Button(label = stringResource(R.string.scan_images_action), { showScanImagesDialog = true }, enabled = !isIndexing),
        MenuActionConfig.Button(label = stringResource(R.string.scan_videos_action), { showScanVideosDialog = true }, enabled=!isIndexing)
    )

    RequestPermissions { _, storageGranted ->
        hasStoragePermission = storageGranted
    }

    val screenTitle = stringResource(R.string.title_search)

    LaunchedEffect(state.hasIndexedImages, state.hasIndexedVideos, isIndexing, state.mediaType, hasStoragePermission) {
        val isFirstImageScanNeeded = hasStoragePermission && state.hasIndexedImages == false && (state.mediaType == MediaType.IMAGE)
        val isFirstVideoScanNeeded = hasStoragePermission && state.hasIndexedVideos == false && (state.mediaType == MediaType.VIDEO)
        if( !isIndexing && (isFirstImageScanNeeded || isFirstVideoScanNeeded)){
            showIndexAlert = true
        }
    }

    LaunchedEffect(imageIndexStatus) {
        if (imageIndexStatus == IndexingStatus.COMPLETE) {
            searchViewModel.reloadIndex(MediaType.IMAGE)
        }
    }

    LaunchedEffect(videoIndexStatus) {
        if (videoIndexStatus == IndexingStatus.COMPLETE) {
            searchViewModel.reloadIndex(MediaType.VIDEO)
        }
    }

    LaunchedEffect(state.searchResults) {
        if(state.searchResults.isEmpty()) offset = 0
    }

    LaunchedEffect(Unit) {
        searchViewModel.externalSearch(intentSearchQuery, appSettings.similarityThreshold, appSettings.imageSimilarityThreshold, appSettings.enableDedupe)
    }

    LaunchedEffect(isIndexing) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
                actions = {
                    Box{
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "menu"
                            )
                        }
                        DropDownMenuWrapper(
                            expanded = showMenu,
                            actions = menuActions,
                            onClose = {showMenu = false}
                        )
                    }
                }
            )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            searchViewModel.onAction(SearchAction.Reset)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { searchViewModel.searchFieldState.text }
            .debounce(50)
            .collectLatest { query: CharSequence ->
                val subStringEnd = searchViewModel.searchFieldState.selection.end
                tagAutoCompleteTagResults = searchViewModel.handleAutoCompletionCheck(query, subStringEnd)
            }
    }

    LaunchedEffect(Unit) {
        searchViewModel.event.collect { event ->
            when (event.type) {
                SearchEventType.TAG -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    BackHandler(enabled = state.selection.isSelecting) {
        searchViewModel.onAction(SearchAction.ResetSelection)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            ProgressBar(
                label = "Indexing images ${"%.0f".format(imageIndexProgress * 100)}%",
                isVisible = imageIndexStatus == IndexingStatus.ACTIVE,
                progress = imageIndexProgress
            )

            ProgressBar(
                label = "Indexing videos ${"%.0f".format(videoIndexProgress * 100)}%",
                isVisible = videoIndexStatus == IndexingStatus.ACTIVE,
                progress = videoIndexProgress
            )
            if (state.queryType == QueryType.IMAGE) {
                SlideRevealBox(
                    reverse = true,
                    offsetPx = offset,
                    modifier = Modifier
                        .zIndex(1f)
                        .heightIn(max = 180.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Column {
                        if (state.selection.isSelecting) {
                            SelectionHeaderRow (
                                selectedCount = state.selection.selectedCount,
                                checked = state.selection.selectAll && state.selection.excludedItems.isEmpty(),
                                onSelectAllChange = {searchViewModel.onAction(SearchAction.SetSelectAll(it))}
                            )
                        }

                        ImageSearcher(
                            uri = state.queryImage,
                            mediaType = state.mediaType,
                            imageSize = 140.dp,
                            mediaTypeSelectorEnabled = (videoIndexStatus != IndexingStatus.ACTIVE && imageIndexStatus != IndexingStatus.ACTIVE), // prevent switching modes when indexing in progress
                            onSearch = {
                                searchViewModel.onAction(SearchAction.Search(appSettings.imageSimilarityThreshold, appSettings.enableDedupe))
                            },
                            onMediaTypeChange = { searchViewModel.onAction(SearchAction.SetMediaTypeFilter(it)) },
                            onRemoveImage = {
                                searchViewModel.onAction(SearchAction.RemoveUploadedImage)
                            }
                        )
                    }
                }
            } else {
                SlideRevealBox(
                    reverse = true,
                    offsetPx = offset,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .heightIn(max = 120.dp)

                ) {
                    Column {
                        if (state.selection.isSelecting) {
                            SelectionHeaderRow (
                                selectedCount = state.selection.selectedCount,
                                checked = state.selection.selectAll && state.selection.excludedItems.isEmpty(),
                                onSelectAllChange = {searchViewModel.onAction(SearchAction.SetSelectAll(it))}
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SearchBar(
                                modifier = Modifier.weight(1f),
                                searchFieldState = searchViewModel.searchFieldState,
                                enabled = hasStoragePermission && !state.loading && !isIndexing ,
                                onSearch = {
                                    searchViewModel.onAction(SearchAction.Search(appSettings.similarityThreshold, appSettings.enableDedupe))
                                },
                                onSearchImage = {
                                    searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(it, appSettings.imageSimilarityThreshold, appSettings.enableDedupe))
                                },
                                onClearResults = {
                                    searchViewModel.onAction(SearchAction.Reset)
                                },
                                placeholders = searchBarPlaceholders,
                                trailingIcon = {
                                    IconButton (
                                        enabled = !isIndexing,
                                        onClick = { isSelectingMediaType = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            tint = if (!isIndexing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 56.dp)
                                    .widthIn(min = 42.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        showFilters = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (state.startDateFilter != null || state.endDateFilter != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if(state.startDateFilter != null || state.endDateFilter != null){
                            TextButton(
                                onClick = { searchViewModel.onAction(SearchAction.ClearDateFilters) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
                AutoCompleter(
                    isVisible = tagAutoCompleteTagResults.isNotEmpty() && !isAddingTag,
                    autoCompleteResults = tagAutoCompleteTagResults,
                    query = searchViewModel.searchFieldState.text.toString(),
                    onSelect = searchViewModel::onSelectAutoCompleteResult,
                    label = "Tags",
                )
            }

                LoadingIndicator(isVisible = state.loading, size = 48.dp, strokeWidth = 4.dp, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp))

            state.error?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }

            if(!hasStoragePermission && !state.loading){
                Text(text = stringResource(R.string.storage_permissions), color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }

            SearchPlaceholderDisplay(isVisible = state.searchResults.isEmpty())

            SearchResults(
                isVisible = !state.loading && state.searchResults.isNotEmpty(),
                numGridColumns = appSettings.resultsPerRow,
                searchResults = state.searchResults,
                totalResults=state.totalResults,
                isSelecting = state.selection.isSelecting,
                selectAll = state.selection.selectAll,
                selectedResults = state.selection.selectedItems,
                excludedResults = state.selection.excludedItems,
                loadMoreBuffer = (RESULTS_BATCH_SIZE * 0.4).toInt(),
                onItemClick = { uri -> searchViewModel.onAction(SearchAction.ViewResult(context, uri, autoOpenInGallery = appSettings.enableDirectGalleryOpen )) },
                onLoadMore = searchViewModel::onLoadMore,
                onToggleSelected = { searchViewModel.onAction(SearchAction.ToggleSelectedResult(it)) },
                onToggleSelectionMode = {
                    searchViewModel.onAction(SearchAction.ToggleSelectionMode)
                    offset = 0
                                        },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsePx,
                onError = searchViewModel::onErrorAsyncImage,

            )
        }
        SlideRevealBox(
            isVisible = isActionBarVisible,
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
            ActionBar(
                modifier = Modifier.height(70.dp),
                actions = actionBarActions
            )
        }
    }

    state.resultToView?.let { item ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
        ) {
            MediaViewer(
                items = state.searchResults,
                initialIndex = state.searchResults.indexOf(item),
                onLoadMore = searchViewModel::onLoadMore,
                onClose = { searchViewModel.onAction(SearchAction.ClearResultView)},
                onUpdateSearchImage = {
                    searchViewModel.onAction(SearchAction.ClearResultView)
                    searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(item.uri, appSettings.imageSimilarityThreshold, appSettings.enableDedupe))
                }
            )
        }
    }

    DatePickerModal(
        show = showStartDatePicker,
        onDismiss = { showStartDatePicker = false },
        onDateSelected = { y, m, d ->
            val startDate = toEpochSeconds(y, m, d)
            searchViewModel.onAction(SearchAction.SetStartDateFilter(startDate))
            showStartDatePicker = false
        },
        initialDateMillis = state.startDateFilter?.times(1000)

    )

    DatePickerModal(
        show = showEndDatePicker,
        onDismiss = { showEndDatePicker = false },
        onDateSelected = { y, m, d ->
            val endDate = toEpochSeconds(y, m, d)
            searchViewModel.onAction(SearchAction.SetEndDateFilter(endDate))
            showEndDatePicker = false
        },
        initialDateMillis = state.endDateFilter?.times(1000)
    )

    BottomSheet(
        show = showFilters,
        onDismiss = { showFilters = false }
    ) {
        Text("Filters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Start date")
                TextButton(onClick = { showStartDatePicker = true }) {
                    Text(state.startDateFilter?.let { formatDate(it) } ?: "Any time")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("End date")
                TextButton(onClick = { showEndDatePicker = true }) {
                    Text(state.endDateFilter?.let { formatDate(it) } ?: "Any time")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton (
                onClick = {
                    searchViewModel.onAction(SearchAction.ClearDateFilters)
                }
            ) {
                Text("Clear")
            }
        }
    }

    if ( showIndexAlert) {
        val title = when(state.mediaType){
            MediaType.IMAGE -> stringResource(R.string.search_start_indexing_alert, "images")
            MediaType.VIDEO -> stringResource(R.string.search_start_indexing_alert, "videos")
        }
        val description = when(state.mediaType){
            MediaType.IMAGE -> stringResource(R.string.first_indexing, "image")
            MediaType.VIDEO -> stringResource(R.string.first_indexing, "video")
        }
        AlertDialog(
            onDismissRequest = { },
            title = { Text(title) },
            text = { Text(description) },
            dismissButton = {
                TextButton(onClick = {
                    showIndexAlert = false
                }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showIndexAlert = false
                    searchViewModel.onAction(SearchAction.Index)
                }) {
                    Text("Confirm")
                }
            }
        )
    }

    if (showScanImagesDialog || showScanVideosDialog) {
        val media = if (showScanImagesDialog) "images" else "videos"

        AlertDialog(
            onDismissRequest = {
                if (showScanImagesDialog) showScanImagesDialog = false else showScanVideosDialog = false
            },
            title = {
                Text(stringResource(R.string.alert_scan_index_title, media))
            },
            text = {
                Column {
                    Text(stringResource(R.string.alert_scan_index_description))

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            if (showScanImagesDialog) {
                                showScanImagesDialog = false
                                searchViewModel.onAction(SearchAction.RefreshIndex(MediaType.IMAGE))
                            } else {
                                showScanVideosDialog = false
                                searchViewModel.onAction(SearchAction.RefreshIndex(MediaType.VIDEO))
                            }
                        }
                    ) {
                        Text("Refresh")
                    }

                    TextButton(
                        onClick = {
                            if (showScanImagesDialog) {
                                showScanImagesDialog = false
                                searchViewModel.onAction(SearchAction.RebuildIndex(MediaType.IMAGE))
                            } else {
                                showScanVideosDialog = false
                                searchViewModel.onAction(SearchAction.RebuildIndex(MediaType.VIDEO))
                            }
                        }
                    ) {
                        Text("Rebuild")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (showScanImagesDialog) showScanImagesDialog = false else showScanVideosDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    TagAdder(
        isVisible = isAddingTag,
        onAddTag = {
            searchViewModel.onAction(SearchAction.TagItems(it))
            isAddingTag = false
        },
        onClose = {
            isAddingTag = false
        },
        onCheckAutoCompletion = searchViewModel::handleAutoCompletionCheck
    )

    OptionPicker(
        isVisible = isSelectingMediaType,
        title = "Media type",
        options = mediaTypeOptions.values.toList(),
        selectedOption = mediaTypeOptions[state.mediaType]!!,
        onSelect = { selected ->
            val mediaType = mediaTypeOptions.entries.find { it.value == selected }?.key ?: MediaType.IMAGE
            searchViewModel.onAction(SearchAction.SetMediaTypeFilter(mediaType))
            isSelectingMediaType = false
        },
        onClose = { isSelectingMediaType = false }
    )
}


