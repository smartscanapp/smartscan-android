package com.fpf.smartscan.ui.screens.bin

import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.ui.state.common.SelectionState

data class BinState(
    val filter: MediaFilter = MediaFilter(),
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    val selection: SelectionState<MediaItem> = SelectionState(),
    val trashedIds: List<Long> = emptyList()
)