package com.fpf.smartscan.api

import kotlinx.serialization.Serializable


@Serializable
data class ImageSummary(
    val summary: String,
    val topics: List<String>
)