package com.fpf.smartscan.concepts

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