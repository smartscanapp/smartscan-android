package com.fpf.smartscan.events

enum class CollectionEventType {
    MERGE,
    DELETE,
    RENAME,
}
data class CollectionEvent (
    val type: CollectionEventType,
    val success: Boolean,
    val message: String? = null,
)