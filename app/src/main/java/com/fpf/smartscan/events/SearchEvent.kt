package com.fpf.smartscan.events


enum class SearchEventType {
    TAG, TEXT_QUERY, IMAGE_QUERY
}

data class SearchEvent (
    val type: SearchEventType,
    val success: Boolean,
    val message: String? = null,
)