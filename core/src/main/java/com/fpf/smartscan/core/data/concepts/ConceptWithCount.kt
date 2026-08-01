package com.fpf.smartscan.core.data.concepts

import androidx.room.Embedded

data class ConceptWithCount(
    @Embedded val concept: ConceptEntity,
    val count: Int
)