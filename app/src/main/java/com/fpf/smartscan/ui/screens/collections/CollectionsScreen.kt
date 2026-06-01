package com.fpf.smartscan.ui.screens.collections

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fpf.smartscan.ui.components.modals.SelectorModal
import com.fpf.smartscan.ui.components.common.SlideRevealBox
import com.fpf.smartscan.ui.components.modals.TextInputModal
import com.fpf.smartscan.ui.components.collections.MediaCollectionsList
import com.fpf.smartscan.ui.screens.collections.CollectionsViewModel.Companion.TOP_N
import kotlinx.coroutines.FlowPreview
import com.fpf.smartscan.R
import androidx.compose.ui.res.stringResource
import com.fpf.smartscan.events.CollectionEventType
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaCollection.Companion.UNLABELLED_COLLECTION
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.ui.action.CollectionAction
import com.fpf.smartscan.ui.components.common.SelectionHeaderRow
import com.fpf.smartscan.ui.components.common.ActionBar
import com.fpf.smartscan.ui.action.ActionConfig
import org.koin.compose.viewmodel.koinViewModel


@OptIn(FlowPreview::class)
@Composable
fun CollectionsScreen(
    onTopBarChange: (TopBarState) -> Unit,
    onViewCollection: (MediaCollection) -> Unit,
    viewModel: CollectionsViewModel = koinViewModel(),
    ) {

    val actionBarHeight = 70

    val state by viewModel.state.collectAsState()
    val clusterCollections by viewModel.clusterCollections.collectAsState()
    val tagCollections by viewModel.tagCollections.collectAsState()

    val collections = when(state.collectionType) {
        CollectionType.CLUSTER -> clusterCollections
        CollectionType.TAG -> tagCollections
    }
    val isCollectionVisible = (tagCollections.isNotEmpty() && state.collectionType == CollectionType.TAG) || (clusterCollections.isNotEmpty() && state.collectionType == CollectionType.CLUSTER)

    val context = LocalContext.current

    // actions
    var isRenamingCollection by remember { mutableStateOf(false) }
    var isMergingCollections by remember { mutableStateOf(false) }
    var isCreatingNewTagAndTaggingClusters by remember { mutableStateOf(false) }
    var isDeletingCollection by remember { mutableStateOf(false) }
    val isActionBarVisible = state.selection.isSelecting && state.selection.selectedCount > 0

    val actionBarActions: List<ActionConfig> = listOf(
        ActionConfig(label = stringResource(R.string.merge_action), { isMergingCollections = true }, enabled = !state.loading, icon = Icons.Filled.Merge),
        ActionConfig( label = stringResource(R.string.rename_action), { isRenamingCollection = true }, enabled = state.selection.selectedItems.size == 1, icon = Icons.Filled.DriveFileRenameOutline),
        ActionConfig(label = stringResource(R.string.delete_action), { isDeletingCollection = true }, enabled = state.collectionType == CollectionType.TAG, icon = Icons.Filled.Delete)
    )

    // Menu
    var showMenu by remember { mutableStateOf(false) }
//    val menuActions: Map<String, MenuActionConfig> = mapOf(
//    )
    val spaceNotAllowedMessage = stringResource(R.string.alert_space_not_allowed)


    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()

    LaunchedEffect(state.collectToView) {
        state.collectToView?.let{
            onViewCollection(it)
            viewModel.onAction(CollectionAction.SetCollectionToView(null))
        }
    }

    // Handle action result via events
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when(event.type){
                CollectionEventType.MERGE -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                }
                CollectionEventType.COPY -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                }

                CollectionEventType.RENAME -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                }

                CollectionEventType.DELETE -> {
                    event.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show()}
                }
            }
        }
    }

    val screenTitle = stringResource(R.string.title_collections)

    LaunchedEffect(state.collectionType) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
//                actions = {
//                    Box{
//                        IconButton(onClick = { showMenu = true }) {
//                            Icon(
//                                imageVector = Icons.Filled.MoreVert,
//                                contentDescription = "menu"
//                            )
//                        }
//                        DropDownMenuWrapper(
//                            expanded = showMenu,
//                            actions = menuActions,
//                            onClose = {showMenu = false}
//                        )
//                    }
//                }
            )
        )
    }

    BackHandler(enabled = state.selection.isSelecting) {
        viewModel.onAction(CollectionAction.ResetSelection)
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
                    onSelectAllChange = {viewModel.onAction(CollectionAction.SetSelectAll(it))}
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (state.collectionType == CollectionType.CLUSTER)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainer
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            viewModel.onAction(CollectionAction.SetCollectionType(CollectionType.CLUSTER))
                        }
                        .padding(12.dp)
                ) {
                    Text(
                        "Auto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.collectionType == CollectionType.CLUSTER)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (state.collectionType == CollectionType.TAG)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainer
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            viewModel.onAction(CollectionAction.SetCollectionType(CollectionType.TAG))
                        }
                        .padding(12.dp)
                ) {
                    Text(
                        "Tags",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.collectionType == CollectionType.TAG)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if(state.totalCollections >= TOP_N) {
                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {viewModel.onAction(CollectionAction.ToggleViewAllCollections)}
                ) {
                    Text(
                        text = if (state.showAllCollections) "Show less" else "Show all",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            MediaCollectionsList(
                isVisible = isCollectionVisible,
                numGridColumns = 3,
                items = collections,
                isSelecting = state.selection.isSelecting,
                selectAll = state.selection.selectAll,
                selectedItems = state.selection.selectedItems,
                excludedItems = state.selection.excludedItems,
                onItemClick = { viewModel.onAction(CollectionAction.SetCollectionToView(it)) },
                onToggleSelected = { viewModel.onAction(CollectionAction.ToggleSelectedCollection(it)) },
                onToggleSelectionMode = {
                    viewModel.onAction(CollectionAction.ToggleSelectionMode)
                    offset = 0
                },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsablePx,
            )

            EmptyCollectionScreen(!isCollectionVisible)
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
                actions = actionBarActions,
                modifier = Modifier.height(actionBarHeight.dp),
            )
        }
    }

    TextInputModal(
        isVisible = isRenamingCollection,
        title=stringResource(R.string.rename_action),
        placeholder = stringResource(R.string.placeholders_rename),
        onClose = { isRenamingCollection = false },
        onConfirm = {
                newName -> viewModel.onAction(CollectionAction.RenameCollection(newName))
            isRenamingCollection = false
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

    TextInputModal(
        isVisible = isCreatingNewTagAndTaggingClusters,
        title=stringResource(R.string.add_tag_action),
        placeholder = stringResource(R.string.placeholders_add_tag),
        onClose = { isCreatingNewTagAndTaggingClusters = false },
        onConfirm =  {
            viewModel.onAction(CollectionAction.CreateNewTagAndTagClusters(it))
            isCreatingNewTagAndTaggingClusters = false
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

    if (isMergingCollections) {
        val labelledCollections: List<String> = state.selection.selectedItems.sortedByDescending { it.size}.map { it.name }.filterNot { it == UNLABELLED_COLLECTION }
        var useSelectorInput by remember { mutableStateOf(labelledCollections.isNotEmpty()) }

        if (useSelectorInput) {
            SelectorModal(
                isVisible = labelledCollections.isNotEmpty(),
                initialOption = labelledCollections.first(),
                title = stringResource(R.string.merge_action),
                label = stringResource(R.string.collections_primary_collection_label),
                options = labelledCollections,
                onConfirm = { selected ->
                    viewModel.onAction(CollectionAction.MergeCollections(selected))
                    isMergingCollections = false
                },
                onClose = { isMergingCollections = false }
            )
        }else{
            TextInputModal(
                isVisible = true,
                title = stringResource(R.string.merge_action),
                placeholder = stringResource(R.string.placeholders_rename),
                onClose = { isMergingCollections = false },
                onConfirm =  { newName ->
                    viewModel.onAction(CollectionAction.MergeCollections(newName, isNewMergedLabel = true))
                    isMergingCollections = false
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
        }
    }

    if ( isDeletingCollection) {
        val count = state.selection.selectedCount
        val alertTitle = stringResource(R.string.collections_delete_collections_alert_title)
        val alertDescription = stringResource(
            R.string.collections_delete_collections_alert_description,
            count,
            pluralStringResource(R.plurals.collection_count, count)
        )
        AlertDialog(
            onDismissRequest = { },
            title = { Text(alertTitle) },
            text = { Text(alertDescription) },
            dismissButton = {
                TextButton(onClick = { isDeletingCollection = false })
                { Text(stringResource(R.string.cancel_action)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(CollectionAction.DeleteCollections)
                    isDeletingCollection = false
                })
                { Text(stringResource(R.string.confirm_action)) }
            }
        )
    }
}