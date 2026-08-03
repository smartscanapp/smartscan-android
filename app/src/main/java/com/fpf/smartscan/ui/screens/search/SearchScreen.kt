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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.zIndex
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.events.SearchEventType
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.core.search.SearchOptions
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
import com.fpf.smartscan.ui.components.pickers.OptionPicker
import com.fpf.smartscan.ui.components.search.RecentSearchesList
import com.fpf.smartscan.ui.shared.MediaViewModel
import com.fpf.smartscan.utils.formatDate
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
    var isSelectingMediaType by remember { mutableStateOf(false) }
    var pendingDeleteItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showRecentSearches by remember { mutableStateOf(false) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteItems?.let{mediaViewModel.delete(it)}
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
            onClick = { searchViewModel.onAction(
                SearchAction.SetQueryImageAndSearch(
                    state.selection.selectedItems.first().uri,
                    SearchOptions( hideDuplicates = appSettings.enableDedupe ))
            )
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
                deleteLauncher.launch(
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
        intentSearchQuery?.let {
            val options =  SearchOptions(hideDuplicates = appSettings.enableDedupe )
            searchViewModel.externalSearch(it, options)
        }
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
                                searchViewModel.onAction(SearchAction.Search(SearchOptions(hideDuplicates = appSettings.enableDedupe)))
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
                                onSearch = {
                                    searchViewModel.onAction(SearchAction.Search(SearchOptions(hideDuplicates = appSettings.enableDedupe)))
                                },
                                onSearchImage = {
                                    searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(it, SearchOptions(hideDuplicates = appSettings.enableDedupe)))
                                },
                                onClearResults = {
                                    searchViewModel.onAction(SearchAction.Reset)
                                },
                                onFocusedChange = {showRecentSearches = it},
                                trailingIcon = {
                                    IconButton (
                                        onClick = { isSelectingMediaType = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            tint =  MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            )
                        }
                        if(state.filter.startDate != null || state.filter.endDate != null){
                            TextButton(
                                onClick = { searchViewModel.onAction(SearchAction.ClearDateFilters) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.reset_filters_action))
                            }
                        }
                    }
                }
                RecentSearchesList(
                    isVisible = state.recentSearches.isNotEmpty() && searchViewModel.searchFieldState.text.isBlank() && showRecentSearches,
                    recentSearches = state.recentSearches.toList(),
                    onSelect = {
                        searchViewModel.searchFieldState.edit { replace(0, searchViewModel.searchFieldState.text.length, it) }
                        searchViewModel.onAction(SearchAction.Search(SearchOptions(hideDuplicates = appSettings.enableDedupe)))
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
                onItemClick = { item ->
                    searchViewModel.onAction(
                        SearchAction.ViewResult(
                            context,
                            item,
                            appSettings.enableDirectGalleryOpen
                        )
                    )
                },
                onToggleSelected = {
                    searchViewModel.onAction(
                        SearchAction.ToggleSelectedResult(
                            it
                        )
                    )
                },
                onToggleSelectionMode = {
                    searchViewModel.onAction(SearchAction.ToggleSelectionMode)
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
                        searchViewModel.onAction(SearchAction.SetQueryImageAndSearch(item.uri, SearchOptions(hideDuplicates = appSettings.enableDedupe)))
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
        Text(stringResource(R.string.filter_action), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.search_start_date_label))
                TextButton(onClick = { showStartDatePicker = true }) {
                    Text(state.filter.startDate?.let { formatDate(it) } ?: stringResource(R.string.search_any_time_label))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.search_end_date_label))
                TextButton(onClick = { showEndDatePicker = true }) {
                    Text(state.filter.endDate?.let { formatDate(it) } ?: stringResource(R.string.search_any_time_label))
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
                Text(stringResource(R.string.reset_filters_action))
            }
        }
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
        title = stringResource(R.string.media_type_title),
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


