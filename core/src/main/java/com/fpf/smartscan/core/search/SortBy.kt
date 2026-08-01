package com.fpf.smartscan.core.search

sealed interface SortBy {
    val ascending: Boolean

    data class Date(override val ascending: Boolean = false): SortBy
    data class Similarity(override val ascending: Boolean = false): SortBy
}