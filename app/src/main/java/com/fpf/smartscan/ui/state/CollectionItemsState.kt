package com.fpf.smartscan.ui.state

import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.state.common.Selectable
import com.fpf.smartscan.ui.state.common.SelectionState

data class CollectionItemsState(
    val collection: MediaCollection? = null,
    val mediaType: MediaType? = null,
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    override val selection: SelectionState<MediaItem> = SelectionState()
): Selectable<MediaItem>