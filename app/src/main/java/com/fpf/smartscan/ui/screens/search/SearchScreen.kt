package com.fpf.smartscan.ui.screens.search

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.R
import com.fpf.smartscan.events.SearchEventType
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.core.search.SearchQuery
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.action.SearchAction
import com.fpf.smartscan.ui.components.common.LoadingIndicator
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.common.SelectionHeaderRow
import com.fpf.smartscan.ui.components.common.SlideRevealBox
import com.fpf.smartscan.ui.components.pickers.DatePickerModal
import com.fpf.smartscan.ui.components.modals.BottomSheet
import com.fpf.smartscan.ui.components.search.AutoCompleter
import com.fpf.smartscan.ui.components.search.ImageSearcher
import com.fpf.smartscan.ui.components.search.SearchBar
import com.fpf.smartscan.ui.components.tags.TagAdder
import com.fpf.smartscan.ui.components.common.ActionBar
import com.fpf.smartscan.ui.action.ActionConfig
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.media.MediaItemsList
import com.fpf.smartscan.ui.components.search.RecentSearchesList
import com.fpf.smartscan.ui.components.search.SearchFilterControls
import com.fpf.smartscan.ui.shared.MediaViewModel
import com.fpf.smartscan.utils.toEpochSeconds
import com.fpf.smartscan.utils.createTrashRequest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import org.koin.compose.viewmodel.koinViewModel
import kotlin.collections.map
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds


@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(
    hasIndexedImages: Boolean?,
    hasIndexedVideos: Boolean?,
    hasStoragePermission: Boolean,
    appSettings:  StateFlow<AppSettings>,
    onIndex: (mediaType: MediaType?) -> Unit,
    onOpenSettings: () -> Unit,
    onViewCollection: (MediaCollection) -> Unit,
    onTopBarChange: (TopBarState) -> Unit,
    intentSearchQuery: SearchQuery? = null,
    searchViewModel: SearchViewModel = koinViewModel(),
    mediaViewModel: MediaViewModel = koinViewModel(),
) {
    val appSettings by appSettings.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    // Search state
    val searchResults = searchViewModel.searchResults.collectAsLazyPagingItems()
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
    val searchResultsVisible = searchResults.itemCount > 0

    var isAddingTag by remember { mutableStateOf(false) }
    var tagAutoCompleteTagResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingDeleteItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showRecentSearches by remember { mutableStateOf(false) }

    val trashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteItems?.let{mediaViewModel.trash(it)}
            searchResults.refresh()
            pendingDeleteItems = null
        }else{
            pendingDeleteItems = null
        }
    }

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
            onClick = {
                searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(state.selection.selectedItems.first().uri))
                      },
            enabled = state.selection.selectedItems.size == 1 && state.mediaType == MediaType.IMAGE,
            icon = Icons.Filled.Search
        ),
        ActionConfig(
            label = stringResource(R.string.add_tag_action),
            onClick = { isAddingTag = true },
            icon = Icons.Filled.Tag),
        ActionConfig(
            label = stringResource(R.string.more_action),
            onClick = { showMoreActions = true },
            icon = Icons.Filled.MoreVert
        )
    )

    val moreActions: List<MenuActionConfig> = listOf(
        MenuActionConfig.Button(
            label = stringResource(R.string.copy_to_clipboard_action),
            onClick = { searchViewModel.onAction(SearchAction.CopyResult(clipboard, context)) },
            enabled = state.selection.selectedItems.size == 1 && state.selection.selectedItems.first().type == MediaType.IMAGE
        ),
        MenuActionConfig.Button(
            label = stringResource(R.string.delete_action),
            onClick = { searchViewModel.onAction(SearchAction.Delete{ items ->
                pendingDeleteItems = items
                trashLauncher.launch(
                    createTrashRequest(context, items.map{it.uri})
                )
            }) },
        ),
    )

    // Dynamic hide animation
    val isActionBarVisible =  state.selection.isSelecting && state.selection.selectedCount > 0
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val actionBarHeight = 70
    val actionBarHeightPx = with(density) {actionBarHeight.dp.toPx() }
    val searchBarHeightPx = with(density) { (if(state.queryImage != null) 200 else 120).dp.toPx() }
    val maxCollapsePx = max(actionBarHeightPx, searchBarHeightPx).toInt()

    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val screenTitle = stringResource(R.string.title_explore)

    LaunchedEffect(Unit) {
        when {
            hasIndexedImages ==true && hasIndexedVideos==false -> searchViewModel.onAction(SearchAction.SetMediaTypeFilter(MediaType.IMAGE))
            hasIndexedVideos == true && hasIndexedImages==false -> searchViewModel.onAction(SearchAction.SetMediaTypeFilter(MediaType.VIDEO))
        }
    }

    LaunchedEffect(hasIndexedVideos, hasIndexedImages, state.mediaType, hasStoragePermission) {
        if ( !hasStoragePermission) return@LaunchedEffect

        val firstImageIndexRequired = state.mediaType == MediaType.IMAGE && hasIndexedImages ==false
        val firstVideoIndexRequired = state.mediaType == MediaType.VIDEO && hasIndexedVideos==false
        val bothRequired = hasIndexedImages ==false && hasIndexedVideos==false

        when {
            bothRequired -> onIndex(null)
            firstImageIndexRequired || firstVideoIndexRequired -> onIndex(state.mediaType)
        }
    }

    LaunchedEffect(state.resultIds) {
        if(state.resultIds.isEmpty()) offset = 0
    }

    LaunchedEffect(Unit) {
        intentSearchQuery?.let { searchViewModel.externalSearch(it) }
    }

    LaunchedEffect(Unit) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
                actions = {
                    IconButton(
                        onClick = {onOpenSettings()}
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "settings"
                        )
                    }
                    IconButton(
                        onClick = {showFilters = true}
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = null
                        )
                    }
                }
            )
        )
    }

    LaunchedEffect(Unit) {
        snapshotFlow { searchViewModel.searchFieldState.text }
            .debounce(50.milliseconds)
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
                SearchEventType.IMAGE_QUERY,
                SearchEventType.TEXT_QUERY -> searchViewModel.handleQueryEvent(event)
            }
        }
    }

    BackHandler(enabled = state.selection.isSelecting || state.resultIds.isNotEmpty()) {
        searchViewModel.onAction(SearchAction.Reset)
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
            if(!hasStoragePermission && !state.loading){
                Text(text = stringResource(R.string.search_storage_permissions), color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }
            if (state.queryImage != null) {
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
                                checked = (state.selection.selectAll && state.selection.excludedItems.isEmpty()) || (state.selection.selectedItems.size == state.totalResults),
                                onSelectAllChange = {searchViewModel.onAction(SearchAction.SetSelectAll(it))}
                            )
                        }

                        ImageSearcher(
                            uri = state.queryImage,
                            mediaType = state.mediaType,
                            imageSize = 140.dp,
                            onSearch = {
                                searchViewModel.onAction(SearchAction.Search)
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
                                enabled = hasStoragePermission && !state.loading ,
                                placeholders = searchBarPlaceholders,
                                onSearch = { searchViewModel.onAction(SearchAction.Search) },
                                onSearchImage = { searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(it)) },
                                onClearResults = { searchViewModel.onAction(SearchAction.Reset) },
                                onFocusedChange = {showRecentSearches = it},
                            )
                        }
                    }
                }
                RecentSearchesList(
                    isVisible = state.recentSearches.isNotEmpty() && searchViewModel.searchFieldState.text.isBlank() && showRecentSearches,
                    recentSearches = state.recentSearches.toList(),
                    onSelect = {
                        searchViewModel.searchFieldState.edit { replace(0, searchViewModel.searchFieldState.text.length, it) }
                        searchViewModel.onAction(SearchAction.Search)
                    },
                    onRemoveRecentSearch = {searchViewModel.onAction(SearchAction.RemoveRecentSearch(it))},
                    onClearAll = {searchViewModel.onAction(SearchAction.ClearRecentSearches)},
                    label = stringResource(R.string.search_recent_searches_label)
                )
                AutoCompleter(
                    isVisible = tagAutoCompleteTagResults.isNotEmpty() && !isAddingTag,
                    autoCompleteResults = tagAutoCompleteTagResults,
                    query = searchViewModel.searchFieldState.text.toString(),
                    onSelect = searchViewModel::onSelectAutoCompleteResult,
                    label = stringResource(R.string.search_tags_label)
                )
            }

                LoadingIndicator(isVisible = state.loading, size = 48.dp, strokeWidth = 4.dp, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp))

            state.error?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(vertical=8.dp))
            }

            SearchPlaceholderDisplay(isVisible = !searchResultsVisible)

            MediaItemsList(
                headerTitle = "${state.totalResults} Results",
                isVisible = searchResultsVisible,
                numGridColumns = appSettings.resultsPerRow,
                items = searchResults,
                isSelecting = state.selection.isSelecting,
                selectAll = state.selection.selectAll,
                excludedItems = state.selection.excludedItems,
                selectedItems = state.selection.selectedItems,
                onItemClick = {
                    if(state.selection.isSelecting){
                        searchViewModel.onAction(SearchAction.ToggleSelectedResult(it))
                    }else{
                        searchViewModel.onAction(SearchAction.ViewResult(it))
                    }
                              },
                onLongItemClick = {
                    searchViewModel.onAction(SearchAction.ToggleSelectionMode)
                    searchViewModel.onAction(SearchAction.ToggleSelectedResult(it))
                    offset = 0
                },
                onOffsetChange = { offset = it },
                maxCollapsePx = maxCollapsePx,
                onError = mediaViewModel::onErrorAsyncImage
            )
        }

        state.resultToView?.let { item ->
            val mediaItems by remember {
                derivedStateOf {
                    List(searchResults.itemCount) { index -> searchResults[index] }.filterNotNull()
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300))
            ) {
                MediaViewer(
                    items = mediaItems,
                    initialIndex = mediaItems.indexOf(item),
                    onClose = { searchViewModel.onAction(SearchAction.ClearResultView)},
                    onUpdateSearchImage = {
                        searchViewModel.onAction(SearchAction.ClearResultView)
                        searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(item.uri))
                    },
                    onSaveUpdatedItem = { updatedMedia ->
                        mediaViewModel.updateDescription(updatedMedia)
                        searchResults.refresh()
                    },
                    onGetCollections = mediaViewModel::getCollectionsMatchingMedia,
                    onCollectionClick = { id, type ->
                        mediaViewModel.viewCollection(id, type){ collection->
                            onViewCollection(collection)
                        }
                    }
                )
            }
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
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ActionBar(
                    actions = actionBarActions,
                    modifier = Modifier.height(actionBarHeight.dp),
                )
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    DropDownMenuWrapper(
                        expanded = showMoreActions,
                        actions = moreActions,
                        offset = DpOffset(x = 0.dp, y = -(actionBarHeight).dp),
                        onClose = { showMoreActions = false }
                    )
                }
            }
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
        initialDateMillis = state.filter.startDate?.times(1000)

    )

    DatePickerModal(
        show = showEndDatePicker,
        onDismiss = { showEndDatePicker = false },
        onDateSelected = { y, m, d ->
            val endDate = toEpochSeconds(y, m, d)
            searchViewModel.onAction(SearchAction.SetEndDateFilter(endDate))
            showEndDatePicker = false
        },
        initialDateMillis = state.filter.endDate?.times(1000)
    )

    BottomSheet(
        show = showFilters,
        onDismiss = { showFilters = false }
    ) {
        SearchFilterControls(
            filter = state.filter,
            label = stringResource(R.string.filter_action),
            onSetMediaType = { searchViewModel.onAction(SearchAction.SetMediaTypeFilter(it))},
            onSetDuplicateFilter = { searchViewModel.onAction(SearchAction.SetDuplicateFilter(it))},
            onSelectStartDate = {showStartDatePicker = true},
            onSelectEndDate = {showEndDatePicker = true},
            onRemoveStartDate = {searchViewModel.onAction(SearchAction.ClearStartDateFilter)},
            onRemoveEndDate = {searchViewModel.onAction(SearchAction.ClearEndDateFilter)},
            onResetFilters = { searchViewModel.onAction(SearchAction.ResetFilters)}
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
}


