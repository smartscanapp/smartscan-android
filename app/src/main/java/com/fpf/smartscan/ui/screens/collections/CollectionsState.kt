package com.fpf.smartscan.ui.screens.collections

import com.fpf.smartscan.core.collections.CollectionType
import com.fpf.smartscan.core.collections.MediaCollection
import com.fpf.smartscan.ui.shared.state.SelectionState

data class CollectionsState(
    val collectionType: CollectionType = CollectionType.CLUSTER,
    val showAllCollections: Boolean = false,
    val loading: Boolean = false,
    val collectToView: MediaCollection? = null,
    val totalCollections: Int = 0,
    val selection: SelectionState<MediaCollection> = SelectionState()
)