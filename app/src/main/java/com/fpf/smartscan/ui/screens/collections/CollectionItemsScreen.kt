package com.fpf.smartscan.ui.screens.collections

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DriveFileMoveRtl
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.events.CollectionItemEventType
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaStoreHelper
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.action.CollectionItemAction
import com.fpf.smartscan.ui.components.common.SelectionHeaderRow
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.common.SlideRevealBox
import com.fpf.smartscan.ui.components.TagAdder
import com.fpf.smartscan.ui.components.common.ActionBar
import com.fpf.smartscan.ui.action.ActionConfig
import com.fpf.smartscan.ui.components.media.MediaItemsList
import com.fpf.smartscan.ui.components.collections.CollectionPicker
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.modals.TextInputModal
import com.fpf.smartscan.ui.components.pickers.OptionPicker
import com.fpf.smartscan.ui.components.placeholders.EmptyItemsScreen
import com.fpf.smartscan.ui.shared.MediaViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(FlowPreview::class)
@Composable
fun CollectionItemsScreen(
    appSettings: StateFlow<AppSettings>,
    collection: MediaCollection?,
    onTopBarChange: (TopBarState) -> Unit,
    onViewCollection: (MediaCollection) -> Unit,
    onBack: () -> Unit,
    mediaViewModel: MediaViewModel = koinViewModel(),
    viewModel: CollectionItemsViewModel = koinViewModel(),
    ) {
    if(collection == null) return

    val actionBarHeight = 70

    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    val state by viewModel.state.collectAsState()
    val appSettings by appSettings.collectAsState()

    val tagCollections by viewModel.tagCollections.collectAsState()
    val clusterCollections by viewModel.clusterCollections.collectAsState()
    val tagCollectionItems = viewModel.tagItems.collectAsLazyPagingItems()
    val clusterCollectionItems = viewModel.clusterItems.collectAsLazyPagingItems()
    val items = when(collection.type){
        CollectionType.TAG -> tagCollectionItems
        CollectionType.CLUSTER -> clusterCollectionItems
    }
    val pickerCollections = when(collection.type){
        CollectionType.TAG -> tagCollections
        CollectionType.CLUSTER -> clusterCollections
    }

    // actions
    var showMenu by remember { mutableStateOf(false) }
    var showSortOptions by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var isMoving by remember { mutableStateOf(false) }
    var isCreatingCollectionAndMoving by remember { mutableStateOf(false) }
    var isAddingTag by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    val spaceNotAllowedMessage = stringResource(R.string.alert_space_not_allowed)
    var pendingDeleteItems by remember { mutableStateOf<List<MediaItem>?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteItems?.let{mediaViewModel.delete(it)}
            items.refresh()
            pendingDeleteItems = null
        }else{
            pendingDeleteItems = null
        }
    }

    val menuActions: List<MenuActionConfig> = listOf(
        MenuActionConfig.Button(
            label = stringResource(R.string.sort_action),
            onClick = { showSortOptions = true },
            enabled = !state.loading,
        ),
        MenuActionConfig.Button(
            label = stringResource(R.string.filter_action),
            onClick = { showFilters = true },
            enabled = !state.loading,
        ),
    )

    val mainActions: List<ActionConfig> = listOf(
        ActionConfig(
            label = stringResource(R.string.share_action),
            onClick = { viewModel.onAction(CollectionItemAction.ShareMedia(context)) },
            icon=Icons.Filled.Share
        ),
        ActionConfig(
            label = stringResource(R.string.add_tag_action),
            onClick = { isAddingTag = true },
            icon=Icons.Filled.Tag
        ),
        ActionConfig(
            label = stringResource(R.string.move_action),
            onClick={ isMoving = true },
            enabled = !state.loading,
            icon = Icons.Default.DriveFileMoveRtl
        ),
        ActionConfig(
            label = stringResource(R.string.more_action),
            onClick = { showMoreActions = true },
            icon = Icons.Filled.MoreVert
        ),
    )

    val moreActions: List<MenuActionConfig> = listOf(
         MenuActionConfig.Button(
             label = stringResource(R.string.copy_to_clipboard_action),
             onClick = { viewModel.onAction(CollectionItemAction.CopyMedia(clipboard, context)) },
             enabled = state.selection.selectedItems.size == 1 && state.selection.selectedItems.first().type == MediaType.IMAGE
        ),

         MenuActionConfig.Button(
             label = stringResource(R.string.remove_tag_action),
             enabled = collection.type == CollectionType.TAG,
             onClick = { viewModel.onAction(CollectionItemAction.RemoveTag) },
             hideIfDisabled = true
        ),
        MenuActionConfig.Button(
            label = stringResource(R.string.delete_action),
            onClick = { viewModel.onAction(CollectionItemAction.Delete{ items ->
                pendingDeleteItems = items
                deleteLauncher.launch(
                    MediaStoreHelper.createTrashRequest(context, items.map{it.uri})
                )
            }) },
        ),
    )

    // For dynamic smooth hiding effect of action bars and other components
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()
    val screenTitle = collection.name

    LaunchedEffect(collection) {
        viewModel.onAction(CollectionItemAction.SetCollectionToView(collection))
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
                actions = {
                    Box{
                        IconButton (onClick = { showMenu = true }) {
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

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when(event.type){
                CollectionItemEventType.MOVE -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                    if(event.success){
                        items.refresh()
                    }
                }
                CollectionItemEventType.REMOVE -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                    if(event.success){
                        items.refresh()
                    }
                }

                CollectionItemEventType.COPY -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                }

                CollectionItemEventType.SHARE -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                }

                CollectionItemEventType.TAG -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                    if(event.success){
                        tagCollectionItems.refresh()
                    }
                }
            }
        }
    }


    BackHandler(enabled = state.selection.isSelecting) {
        viewModel.onAction(CollectionItemAction.ResetSelection)
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
                    SelectionHeaderRow(
                        selectedCount = state.selection.selectedCount,
                        checked = (state.selection.selectAll && state.selection.excludedItems.isEmpty()) || (state.selection.selectedItems.size == state.collection?.size),
                        onSelectAllChange = {
                            viewModel.onAction(
                                CollectionItemAction.SetSelectAll(
                                    it
                                )
                            )
                        }
                    )
                }
                MediaItemsList(
                    isVisible = items.itemCount > 0,
                    numGridColumns = appSettings.resultsPerRow,
                    items = items,
                    isSelecting = state.selection.isSelecting,
                    selectAll = state.selection.selectAll,
                    excludedItems = state.selection.excludedItems,
                    selectedItems = state.selection.selectedItems,
                    onItemClick = { item ->
                        viewModel.onAction(
                            CollectionItemAction.SetMediaToView(
                                context,
                                item,
                                appSettings.enableDirectGalleryOpen
                            )
                        )
                    },
                    onToggleSelected = {
                        viewModel.onAction(
                            CollectionItemAction.ToggleSelectedMedia(
                                it
                            )
                        )
                    },
                    onToggleSelectionMode = {
                        viewModel.onAction(CollectionItemAction.ToggleSelectionMode)
                        offset = 0
                    },
                    onOffsetChange = { offset = it },
                    maxCollapsePx = maxCollapsablePx,
                    onError = mediaViewModel::onErrorAsyncImage
                )

                EmptyItemsScreen(
                    isVisible = items.itemCount == 0
                )
            }

        state.mediaToView?.let { item ->
            val mediaItems by remember {
                derivedStateOf {
                    List(items.itemCount) { index -> items[index] }.filterNotNull()
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
                    onClose = {
                        viewModel.onAction(CollectionItemAction.SetMediaToView(context, null))
                    },
                    onUpdateSearchImage = null,
                    onLoadMore = {
                        val lastIndex = (items.itemCount - 1).coerceAtLeast(0)
                        items[lastIndex]
                    },
                    onSaveUpdatedItem = { updatedMedia->
                        mediaViewModel.updateDescription(updatedMedia)
                        items.refresh()
                    },
                    onGetCollections = mediaViewModel::getCollectionsMatchingMedia,
                    onCollectionClick = { id, type ->
                        mediaViewModel.viewCollection(id, type) { collection ->
                            onViewCollection(collection)
                        }
                    }
                )
            }
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

        AnimatedVisibility(
            visible = isMoving,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(500)
            ),
            exit = fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(300)
            )
        ) {
            CollectionPicker(
                collections = pickerCollections,
                onClose = { isMoving = false },
                onSelectCollection = {
                    viewModel.onAction(CollectionItemAction.MoveMedia( it))
                    isMoving = false
                                     },
                onCreateNewCollection =  {
                    isMoving = false
                    isCreatingCollectionAndMoving = true
                }
            )
        }
    }

    TextInputModal(
        isVisible = isCreatingCollectionAndMoving,
        title=stringResource(R.string.add_collection_action),
        placeholder = stringResource(R.string.placeholders_collection_name),
        onClose = {isCreatingCollectionAndMoving = false},
        onConfirm =  {
            viewModel.onAction(CollectionItemAction.CreateNewCollectionAndMove(it))
            isCreatingCollectionAndMoving = false
        },
        leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = "Tag", tint = MaterialTheme.colorScheme.primary) },
        onValueChange = {
            if (!it.text.contains(" ")) {
                true
            } else {
                Toast.makeText(context, spaceNotAllowedMessage, Toast.LENGTH_SHORT).show()
                false
            }
        }
    )

    TagAdder(
        isVisible = isAddingTag,
        onAddTag = {
            viewModel.onAction(CollectionItemAction.Tag(it))
            isAddingTag = false
        },
        onClose = {
            isAddingTag = false
        },
        onCheckAutoCompletion = viewModel::handleAutoCompletionCheck
    )

    OptionPicker(
        isVisible = showSortOptions,
        title = stringResource(R.string.sort_title),
        options =  viewModel.sortByOptions.values.toList(),
        selectedOption  = viewModel.sortByOptions[state.sortBy]?: viewModel.sortByOptions.values.first(),
        onSelect = { selected ->
            val sortBy =  viewModel.sortByOptions.entries.find { it.value == selected }?.key
            sortBy?.let{
                viewModel.onAction(CollectionItemAction.SetSortBy(it))
            }
            showSortOptions = false
        },
        onClose = {showSortOptions = false}
    )

    OptionPicker(
        isVisible = showFilters,
        title = stringResource(R.string.media_type_title),
        options =  listOf("All") + mediaTypeOptions.values.toList(),
        selectedOption  = mediaTypeOptions[state.filter.mediaType]?: "All",
        onSelect = { selected ->
            val mediaType = mediaTypeOptions.entries.find { it.value == selected }?.key
            viewModel.onAction(CollectionItemAction.SetFilter(SearchFilter(mediaType=mediaType)))
            showFilters = false
        },
        onClose = {showFilters = false}
    )
}