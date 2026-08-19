package com.fpf.smartscan.ui.utils

import com.fpf.smartscan.ui.shared.state.SelectionState

object SelectionUtils {
    fun <T>clearSelection(state: SelectionState<T>): SelectionState<T> = state.copy(selectedItems = emptySet(), excludedItems = emptySet(), selectAll = false, selectedCount = 0)

    fun <T>toggleSelectedItem(state: SelectionState<T>, item: T, total: Int): SelectionState<T>{
        return if(state.selectAll){
            if (item in state.excludedItems) {
                val updatedExcludedResults = state.excludedItems - item
                val safeCount = ( state.selectedCount + 1).coerceAtLeast(0 )
                state.copy(excludedItems = updatedExcludedResults, selectedCount =safeCount)
            } else {
                val safeCount = ( state.selectedCount - 1).coerceAtMost(total )
                val updatedExcludedResults = state.excludedItems + item
                state.copy(excludedItems = updatedExcludedResults, selectedCount = safeCount)
            }
        }
        else{
            if (item in state.selectedItems) {
                val safeCount = ( state.selectedCount - 1).coerceAtLeast(0 )
                val updatedSelectedResults = state.selectedItems - item
                state.copy(selectedItems = updatedSelectedResults, selectedCount = safeCount)
            } else {
                val safeCount = ( state.selectedCount + 1).coerceAtMost(total )
                val updatedSelectedResults = state.selectedItems + item
                state.copy(selectedItems = updatedSelectedResults, selectedCount = safeCount)
            }
        }
    }

    fun <T>setSelectAll(state: SelectionState<T>, selectAll: Boolean, total: Int): SelectionState<T>{
        val newSelectAll = if (state.selectAll && state.excludedItems.isNotEmpty()) {
                true
            } else {
                selectAll
            }
        var updatedState = state.copy(selectAll = newSelectAll, selectedItems = emptySet(), excludedItems = emptySet())
        updatedState = updatedState.copy(selectedCount=getSelectedCount(updatedState, total))
        return updatedState
    }

    private fun <T>getSelectedCount(state: SelectionState<T>, total: Int): Int{
        return if(state.selectAll){
            if(state.excludedItems.isEmpty()) total else total - state.excludedItems.size
        }else{
            state.selectedItems.size
        }
    }

    suspend fun <T> getSelectedItems(state: SelectionState<T>, getAllItems: suspend () -> MutableSet<T>): Set<T> {
        return if (state.selectAll) {
            val allItems = getAllItems()
            allItems.removeAll(state.excludedItems)
            allItems
        } else {
            state.selectedItems
        }
    }

    fun <T> toggleSelectionMode(state: SelectionState<T>): SelectionState<T> = state.copy(isSelecting = !state.isSelecting)

    fun <T>resetSelection(state: SelectionState<T>): SelectionState<T> = state.copy(selectedItems = emptySet(), excludedItems = emptySet(), selectAll = false, selectedCount = 0, isSelecting = false)

}
