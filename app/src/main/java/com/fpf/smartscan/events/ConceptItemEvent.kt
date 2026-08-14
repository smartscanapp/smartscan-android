package com.fpf.smartscan.events


enum class ConceptItemEventType {
    HIDE,
}

data class ConceptItemEvent (
    val type: ConceptItemEventType,
    val success: Boolean,
    val message: String? = null,
)