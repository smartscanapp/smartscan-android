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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.fpf.smartscan.R
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.constants.mediaTypeOptions
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.ui.action.ConceptItemsAction
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.concepts.ConceptItemsList
import com.fpf.smartscan.ui.components.media.MediaViewer
import com.fpf.smartscan.ui.components.pickers.OptionPicker
import com.fpf.smartscan.ui.components.placeholders.EmptyItemsScreen
import com.fpf.smartscan.ui.shared.MediaViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConceptItemsScreen(
    concept: Concept?,
    onTopBarChange: (TopBarState) -> Unit,
    onBack: () -> Unit,
    mediaViewModel: MediaViewModel = koinViewModel(),
    viewModel: ConceptItemsViewModel = koinViewModel(),
) {
    if(concept == null) return

    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val conceptItems = viewModel.conceptItems.collectAsLazyPagingItems()

    // For dynamic smooth hiding effect of action bars and other components
    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()
    val screenTitle = ""

    var showSortOptions by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val menuActions: List<MenuActionConfig> = listOf(
        MenuActionConfig.Button(
            label = "Sort",
            onClick = { showSortOptions = true },
            enabled = !state.loading,
        ),
        MenuActionConfig.Button(
            label = "Filter",
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


    BackHandler(enabled = state.selection.isSelecting) {
        viewModel.onAction(ConceptItemsAction.ResetSelection)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
//                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            ConceptItemsList(
                isVisible = conceptItems.itemCount > 0,
                items = conceptItems,
                onItemClick = { viewModel.onAction(ConceptItemsAction.SetMediaToView( it)) },
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
                    onClose = { viewModel.onAction(ConceptItemsAction.SetMediaToView( null))},
                    onUpdateSearchImage = null,
                    onLoadMore = { val lastIndex = (conceptItems.itemCount - 1).coerceAtLeast(0)
                        conceptItems[lastIndex]},
                    onSaveUpdatedItem = {
                        mediaViewModel.saveUpdatedItem(it)
                        conceptItems.refresh()
                    }
                )
            }
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

    OptionPicker(
        isVisible = showFilters,
        title = stringResource(R.string.media_type_title),
        options =  listOf("All") + mediaTypeOptions.values.toList(),
        selectedOption  = mediaTypeOptions[state.filter.mediaType]?: "All",
        onSelect = { selected ->
            val mediaType = mediaTypeOptions.entries.find { it.value == selected }?.key
            viewModel.onAction(ConceptItemsAction.SetFilter(SearchFilter(mediaType=mediaType)))
            showFilters = false
        },
        onClose = {showFilters = false}
    )

}