package com.fpf.smartscan.api

import kotlinx.serialization.Serializable


@Serializable
data class ImageSummary(
    val highlights: List<String>,
    val isTextBasedImage: Boolean
)