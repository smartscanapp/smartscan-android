package com.fpf.smartscan.ui.state

import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.ui.state.common.Selectable
import com.fpf.smartscan.ui.state.common.SelectionState

data class CollectionsState(
    val collectionType: CollectionType = CollectionType.CLUSTER,
    val showAllCollections: Boolean = false,
    val loading: Boolean = false,
    val collectToView: MediaCollection? = null,
    val totalCollections: Int = 0,
    override val selection: SelectionState<MediaCollection> = SelectionState()
): Selectable<MediaCollection>