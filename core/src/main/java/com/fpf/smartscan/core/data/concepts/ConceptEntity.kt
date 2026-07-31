package com.fpf.smartscan.core.data.concepts

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concept")
data class ConceptEntity (
    @PrimaryKey
    val id: Long,
    val description: String,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)