package com.fpf.smartscan.core.concepts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Concept (
    val id: Long,
    val description: String,
    val size: Int,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()

): Parcelable{
}

data class NewConcept (
    val description: String,
    val size: Int = 0,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)