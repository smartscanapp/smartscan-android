package com.fpf.smartscan.ui.state

import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.state.common.Selectable
import com.fpf.smartscan.ui.state.common.SelectionState

data class ConceptItemsState(
    val concept: Concept? = null,
    val mediaType: MediaType? = null,
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    override val selection: SelectionState<MediaItem> = SelectionState()
): Selectable<MediaItem>