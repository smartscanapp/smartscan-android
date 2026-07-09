package com.fpf.smartscan.data.concepts

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concept")
data class ConceptEntity (
    @PrimaryKey
    val id: Long,
    val description: String,
    val updatedAt: Long = System.currentTimeMillis()
)