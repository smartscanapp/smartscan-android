package com.fpf.smartscan.search

sealed interface SortBy {
    val ascending: Boolean

    data class Date(override val ascending: Boolean = true): SortBy
    data class Similarity(override val ascending: Boolean = true): SortBy
}