package com.fpf.smartscan.ui.shared.state

data class SelectionState<T> (
    val selectedItems: Set<T> = emptySet(),
    val excludedItems: Set<T> = emptySet(),
    val selectAll: Boolean = false,
    val isSelecting: Boolean = false,
    val selectedCount: Int = 0,
)