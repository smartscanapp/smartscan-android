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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fpf.smartscan.ui.components.common.SlideRevealBox
import com.fpf.smartscan.ui.components.modals.TextInputModal
import kotlinx.coroutines.FlowPreview
import com.fpf.smartscan.R
import androidx.compose.ui.res.stringResource
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.navigation.TopBarState
import com.fpf.smartscan.ui.components.common.SelectionHeaderRow
import com.fpf.smartscan.ui.components.common.ActionBar
import com.fpf.smartscan.ui.action.ActionConfig
import com.fpf.smartscan.ui.action.ConceptAction
import com.fpf.smartscan.ui.action.MenuActionConfig
import com.fpf.smartscan.ui.components.buttons.CustomFloatingActionButton
import com.fpf.smartscan.ui.components.collections.MultiCollectionPicker
import com.fpf.smartscan.ui.components.common.DropDownMenuWrapper
import com.fpf.smartscan.ui.components.common.LoadingIndicator
import com.fpf.smartscan.ui.components.concepts.ConceptsList
import org.koin.compose.viewmodel.koinViewModel


@OptIn(FlowPreview::class)
@Composable
fun ConceptsScreen(
    onTopBarChange: (TopBarState) -> Unit,
    onViewConcept: (Concept) -> Unit,
    isMainScanRequired: Boolean,
    onIndex: ()-> Unit,
    hasStoragePermission: Boolean,
    viewModel: ConceptsViewModel = koinViewModel(),
) {

    val actionBarHeight = 70
    val state by viewModel.state.collectAsState()
    val clusterCollections by viewModel.clusterCollections.collectAsState()
    val tagCollections by viewModel.tagCollections.collectAsState()
    val collections = when(state.selectedCollectionType) {
        CollectionType.CLUSTER -> clusterCollections
        CollectionType.TAG -> tagCollections
        else -> emptyList()
    }
    val concepts by viewModel.concepts.collectAsState()
    val isConceptsVisible = concepts.isNotEmpty()

    // actions
    var showMenu by remember { mutableStateOf(false) }
    var isCreatingConcept by remember { mutableStateOf(false) }
    var isEditingConcept by remember { mutableStateOf(false) }
    var isDeletingConcept by remember { mutableStateOf(false) }
    val isActionBarVisible = state.selection.isSelecting && state.selection.selectedCount > 0

    //TODO: update db to store isPinned on concepts

    val actionBarActions: List<ActionConfig> = listOf(
        ActionConfig( label = stringResource(R.string.edit_concept_action), onClick={ isEditingConcept = true }, enabled = state.selection.selectedItems.size == 1, icon = Icons.Filled.DriveFileRenameOutline),
        ActionConfig(label = stringResource(R.string.pin_unpin_concept_action), onClick = {viewModel.onAction(ConceptAction.PinUnpinConcept)}, enabled = !state.loading, icon = Icons.Filled.PushPin),
        ActionConfig(label = stringResource(R.string.delete_action), { isDeletingConcept = true }, icon = Icons.Filled.Delete)
    )
    val menuActions: List<MenuActionConfig> = listOf(
        MenuActionConfig.Button(
            label = "Set allowed tag collections",
            onClick = { viewModel.onAction(ConceptAction.SetCollectionType(CollectionType.TAG)) },
            enabled = !state.loading,
        ),
        MenuActionConfig.Button(
            label = "Set allowed auto collections",
            onClick = { viewModel.onAction(ConceptAction.SetCollectionType(CollectionType.CLUSTER)) },
            enabled = !state.loading,
        ),
        MenuActionConfig.Button(
            label = "Generate summaries",
            onClick = { onIndex()},
            enabled = viewModel.hasSelectCollection, // TODO: add api key check here also
        ),
    )

    var offset by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val maxCollapsablePx = with(density) { 70.dp.toPx() }.toInt()

    LaunchedEffect(state.conceptToView) {
        state.conceptToView?.let{
            onViewConcept(it)
            viewModel.onAction(ConceptAction.SetConceptToView(null))
        }
    }


    val screenTitle = stringResource(R.string.title_concepts)

    LaunchedEffect(Unit) {
        onTopBarChange(
            TopBarState(
                title = screenTitle,
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
        viewModel.checkRecentUpdatesAndUpdateConcepts()
    }

    BackHandler(enabled = state.selection.isSelecting) {
        viewModel.onAction(ConceptAction.ResetSelection)
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
            if(!hasStoragePermission){
                Text(
                    text = stringResource(R.string.concepts_storage_permissions),
                    color = Color.Red,
                    modifier = Modifier.padding(vertical=8.dp).align(Alignment.CenterHorizontally),
                )
            }

            if(state.loading){
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    LoadingIndicator(true)
                    Text(
                        text = "Updating matching media...",
                    )
                }
            }

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
                    checked = (state.selection.selectAll && state.selection.excludedItems.isEmpty()) || (state.selection.selectedItems.size == state.totalConcepts),
                    onSelectAllChange = {viewModel.onAction(ConceptAction.SetSelectAll(it))}
                )
            }

            ConceptsList(
                isVisible = isConceptsVisible,
                numGridColumns = 2,
                items = concepts,
                isSelecting = state.selection.isSelecting,
                selectAll = state.selection.selectAll,
                selectedItems = state.selection.selectedItems,
                excludedItems = state.selection.excludedItems,
                onItemClick = {
                    if(state.selection.isSelecting){
                        viewModel.onAction(ConceptAction.ToggleSelectedConcept(it))
                    }
                    else{
                        viewModel.onAction(ConceptAction.SetConceptToView(it)) }
                    },
                onItemLongClick = {
                    viewModel.onAction(ConceptAction.ToggleSelectedConcept(it))
                    viewModel.onAction(ConceptAction.ToggleSelectionMode)
                    offset = 0
                },
                onOffsetChange = {  offset = it },
                maxCollapsePx = maxCollapsablePx,
            )
            EmptyConceptsScreen(
                isVisible = !isConceptsVisible,
                isMainScanRequired=isMainScanRequired,
            )
        }


        CustomFloatingActionButton (
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding( 32.dp),
            onClick = {
                isCreatingConcept = true
            }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add concept button")
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

    AnimatedVisibility(
        visible = state.selectedCollectionType != null,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(500)
        ),
        exit = fadeOut(animationSpec = tween(300)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(300)
        )
    ) {
        MultiCollectionPicker(
            collections = collections,
            onClose = { viewModel.onAction(ConceptAction.SetCollectionType(null)) },
            selectedItems = state.collectionsSelection.selectedItems,
            onToggleSelected = {
                viewModel.onAction(ConceptAction.ToggleSelectedCollection(it))
            },
            onSaveSelectedCollections = {
                viewModel.onAction(ConceptAction.SetAllowedCollections)
            },
        )
    }

    // Reset both for simplicity
    TextInputModal(
        isVisible = isCreatingConcept || isEditingConcept,
        initialValue = if(isCreatingConcept) "" else state.selection.selectedItems.firstOrNull()?.description?: "",
        title=if(isCreatingConcept)stringResource(R.string.create_concept_modal_title) else stringResource(R.string.edit_concept_modal_title),
        placeholder = stringResource(R.string.placeholders_add_concept),
        onClose = {
            isCreatingConcept = false
            isEditingConcept = false
                  },
        onConfirm = {
            if(isCreatingConcept){
                 viewModel.onAction(ConceptAction.AddConcept(it))
            }else{
                viewModel.onAction(ConceptAction.EditConcept(it))
            }
            isCreatingConcept = false
            isEditingConcept = false
        },
        leadingIcon = { Icon(Icons.Filled.Lightbulb, contentDescription = "Lightbulb", tint = MaterialTheme.colorScheme.primary) },
    )

    if ( isDeletingConcept) {
        val count = state.selection.selectedCount
        val alertTitle = stringResource(R.string.concepts_delete_alert_title)
        val alertDescription = stringResource(
            R.string.concepts_delete_alert_description,
            count,
            pluralStringResource(R.plurals.concept_count, count)
        )
        AlertDialog(
            onDismissRequest = { },
            title = { Text(alertTitle) },
            text = { Text(alertDescription) },
            dismissButton = {
                TextButton(onClick = { isDeletingConcept = false })
                { Text(stringResource(R.string.cancel_action)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(ConceptAction.DeleteConcepts)
                    isDeletingConcept = false
                })
                { Text(stringResource(R.string.confirm_action)) }
            }
        )
    }
}
