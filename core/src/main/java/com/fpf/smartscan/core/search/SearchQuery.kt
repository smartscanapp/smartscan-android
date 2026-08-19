package com.fpf.smartscan.core.search

import android.net.Uri


sealed interface SearchQuery {
    val filter: SearchFilter

    val isImageQuery: Boolean
        get() = this is ImageQuery

    val isTextQuery: Boolean
        get() = this is TextQuery

    data class ImageQuery(
        val uri: Uri,
        override val filter: SearchFilter = SearchFilter(),
    ) : SearchQuery

    data class TextQuery(
        val text: String,
        override val filter: SearchFilter = SearchFilter(),
    ) : SearchQuery
}