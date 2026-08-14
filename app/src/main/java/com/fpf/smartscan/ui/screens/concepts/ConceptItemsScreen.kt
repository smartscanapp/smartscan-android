package com.fpf.smartscan.ui.screens.concepts


import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.R
import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.PlayerPool
import com.fpf.smartscan.events.ConceptItemEventType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.concepts.ConceptFilterControls
import com.fpf.smartscan.ui.components.concepts.ConceptItemsList
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.modals.BottomSheet
import com.fpf.smartscan.ui.components.pickers.OptionPicker
import com.fpf.smartscan.ui.components.placeholders.EmptyItemsScreen
import com.fpf.smartscan.ui.shared.MediaViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConceptItemsScreen(
    concept: Concept?,
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit,
    onViewCollection: (MediaCollection) -> Unit,
    mediaViewModel: MediaViewModel = koinViewModel(),
    viewModel: ConceptItemsViewModel = koinViewModel(),
) {
    if(concept == null) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val conceptItems = viewModel.conceptItems.collectAsLazyPagingItems()
    val playerPool = remember(context) {
        PlayerPool(
            List(2) {
                ExoPlayer.Builder(context).build()
            }
        )
    }

    // For dynamic smooth hiding effect of action bars and other components
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()
    val screenTitle = ""

    var showSortOptions by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf(false) }

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


    DisposableEffect(playerPool) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                playerPool.assignedIds.forEach { id ->
                    playerPool.get(id)?.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            playerPool.releaseAll()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event.type) {
                ConceptItemEventType.HIDE -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    if (event.success) {
                        conceptItems.refresh()
                    }
                }
            }
        }
    }

    BackHandler(enabled = state.selection.isSelecting) {
        viewModel.onAction(ConceptItemsAction.ResetSelection)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            ConceptItemsList(
                isVisible = conceptItems.itemCount > 0,
                items = conceptItems,
                playerPool = playerPool,
                onItemClick = { viewModel.onAction(ConceptItemsAction.SetMediaToView(it)) },
                onShowItemMenu = {
                    showItemMenu = true
                    viewModel.onAction(ConceptItemsAction.ToggleSelectedMedia(it))
                },
                onOffsetChange = { offset = it },
                maxCollapsePx = maxCollapsablePx,
                onError = mediaViewModel::onErrorAsyncImage
            )

            EmptyItemsScreen(
                isVisible = conceptItems.itemCount == 0
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
                onClose = { viewModel.onAction(ConceptItemsAction.SetMediaToView(null)) },
                onUpdateSearchImage = null,
                onLoadMore = {
                    val lastIndex = (conceptItems.itemCount - 1).coerceAtLeast(0)
                    conceptItems[lastIndex]
                },
                onSaveUpdatedItem = { updatedMedia ->
                    mediaViewModel.updateDescription(updatedMedia)
                    conceptItems.refresh()
                },
                onGetCollections = mediaViewModel::getCollectionsMatchingMedia,
                onCollectionClick = { id, type ->
                    mediaViewModel.viewCollection(id, type) { collection->
                        onViewCollection(collection)
                    }
                }
            )
        }
    }


    OptionPicker(
        isVisible = showSortOptions,
        title = stringResource(R.string.sort_title),
        options =  viewModel.sortByOptions.values.toList(),
        selectedOption  = viewModel.sortByOptions[state.sortBy]?: viewModel.sortByOptions.values.first(),
        onSelect = { selected ->
            val sortBy =  viewModel.sortByOptions.entries.find { it.value == selected }?.key
            sortBy?.let{
                viewModel.onAction(ConceptItemsAction.SetSortBy(it))
            }
            showSortOptions = false
        },
        onClose = {showSortOptions = false}
    )

    BottomSheet (
        show = showFilters,
        onDismiss = { showFilters = false }
    ) {
        ConceptFilterControls(
            filter = state.filter,
            label = stringResource(R.string.filter_action),
            onSetShowHidden = { viewModel.onAction(ConceptItemsAction.SetShowHiddenFilter(it))},
            onSetMediaType = { viewModel.onAction(ConceptItemsAction.SetMediaTypeFilter(it))},
            onResetFilters = { viewModel.onAction(ConceptItemsAction.ResetFilters)},
        )
    }

    BottomSheet(
        show = showItemMenu && state.selection.selectedItems.size == 1,
        onDismiss = {
            showItemMenu = false
            viewModel.onAction(ConceptItemsAction.ResetSelection)
        }
    ) {
        val isHidden = state.selection.selectedItems.firstOrNull()?.isHidden ?: false

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ){
            Row (
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = {
                            showItemMenu = false
                            viewModel.onAction(ConceptItemsAction.ToggleHide)
                        }
                    )
                    .padding(8.dp)
            ){
                Icon(
                    imageVector = if(isHidden) Icons.Filled.Visibility else  Icons.Filled.VisibilityOff,
                    contentDescription = "hide icon",
                )

                val text = if(isHidden){
                    stringResource(R.string.show_action)
                }else{
                    stringResource(R.string.hide_action)
                }
                Text(text)
            }
        }
    }
}