package com.fpf.smartscan.core.data.media

import androidx.room.Embedded

data class HideableMediaEntity(
    @Embedded val media: MediaMetadataEntity,
    val isHidden: Boolean
)