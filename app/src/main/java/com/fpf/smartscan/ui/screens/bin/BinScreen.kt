package com.fpf.smartscan.ui.screens.bin

import android.app.Activity
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.R
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.ui.components.common.SelectionHeaderRow
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.common.SlideRevealBox
import com.fpf.smartscan.ui.components.common.ActionBar
import com.fpf.smartscan.ui.action.ActionConfig
import com.fpf.smartscan.ui.components.media.MediaItemsList
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.placeholders.EmptyItemsScreen
import com.fpf.smartscan.ui.shared.MediaViewModel
import com.fpf.smartscan.utils.createDeleteRequest
import com.fpf.smartscan.utils.createTrashRequest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(FlowPreview::class)
@Composable
fun BinScreen(
    appSettings: StateFlow<AppSettings>,
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit,
    mediaViewModel: MediaViewModel = koinViewModel(),
    viewModel: BinViewModel = koinViewModel(),
) {

    val context = LocalContext.current
    val actionBarHeight = 70
    val state by viewModel.state.collectAsState()
    val appSettings by appSettings.collectAsState()
    val items = viewModel.trashedItems.collectAsLazyPagingItems()

    // actions
    var showMenu by remember { mutableStateOf(false) }
    var itemPendingAction by remember { mutableStateOf<List<MediaItem>?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            itemPendingAction?.let{mediaViewModel.delete(it)}
            items.refresh()
            itemPendingAction = null
        }else{
            itemPendingAction = null
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            itemPendingAction?.let{mediaViewModel.restore(it)}
            items.refresh()
            itemPendingAction = null
        }else{
            itemPendingAction = null
        }
    }

    val menuActions: List<MenuActionConfig> = listOf(
        MenuActionConfig.Button(
            label = stringResource(R.string.empty_bin_action),
            onClick = {
                viewModel.onAction(BinAction.SetSelectAll(true))
                viewModel.onAction(
                BinAction.Delete{ items ->
                    itemPendingAction = items
                    deleteLauncher.launch(createDeleteRequest(context, items.map{it.uri}))
                }
            )},
        ),
    )

    val actionBarActions: List<ActionConfig> = listOf(
        ActionConfig(
            label = stringResource(R.string.delete_action),
            onClick = { viewModel.onAction(
                BinAction.Delete{ items ->
                    itemPendingAction = items
                    deleteLauncher.launch(createDeleteRequest(context, items.map{it.uri}))
                }
            ) },
            icon=Icons.Filled.Delete
        ),
        ActionConfig(
            label = stringResource(R.string.restore_action),
            onClick = { viewModel.onAction(BinAction.Restore{ items ->
                itemPendingAction = items
                restoreLauncher.launch(
                    createTrashRequest(context, items.map{it.uri}, false)
                )
            })},
            icon=Icons.Filled.Restore
        ),
    )

    // For dynamic smooth hiding effect of action bars and other components
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()
    val screenTitle = stringResource(R.string.title_recycle_bin)

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

    BackHandler(enabled = state.selection.isSelecting) {
        viewModel.onAction(BinAction.ResetSelection)
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
                    checked = (state.selection.selectAll && state.selection.excludedItems.isEmpty()) || (state.selection.selectedItems.size == state.trashedIds.size),
                    onSelectAllChange = { viewModel.onAction(BinAction.SetSelectAll(it)) }
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
                    if(state.selection.isSelecting){
                        viewModel.onAction(BinAction.ToggleSelectedMedia(item))
                    }else{
                        viewModel.onAction(BinAction.SetMediaToView(item))
                    }
                },
                onLongItemClick = {
                    viewModel.onAction(BinAction.ToggleSelectionMode)
                    viewModel.onAction(BinAction.ToggleSelectedMedia(it))
                    offset = 0
                },
                onOffsetChange = { offset = it },
                maxCollapsePx = maxCollapsablePx,
                onError = mediaViewModel::onErrorAsyncImage
            )

            EmptyItemsScreen(
                isVisible = items.itemCount == 0,
                description = stringResource(R.string.bin_description)
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
                    onClose = { viewModel.onAction(BinAction.SetMediaToView(null)) },
                    onLoadMore = {
                        val lastIndex = (items.itemCount - 1).coerceAtLeast(0)
                        items[lastIndex]
                    },
                    onSaveUpdatedItem = null,
                    onGetCollections =null,
                    onCollectionClick = null,
                    actionsEnabled = false
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
                    actions = actionBarActions,
                    modifier = Modifier.height(actionBarHeight.dp),
                )
            }
        }
    }
}