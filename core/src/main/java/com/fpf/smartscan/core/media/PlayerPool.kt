package com.fpf.smartscan.core.media

import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.exoplayer.ExoPlayer

class PlayerPool(private val players: List<ExoPlayer>) {
    private val assigned = mutableStateMapOf<Long, ExoPlayer>()
    val assignedIds: Set<Long> = assigned.keys

    fun get(videoId: Long): ExoPlayer? = assigned[videoId]

    fun assign(videoId: Long): ExoPlayer? {
        return assigned[videoId]
            ?: players.firstOrNull { it !in assigned.values }?.also {
                assigned[videoId] = it
            }
    }

    fun release(videoId: Long) {
        assigned.remove(videoId)?.apply {
            pause()
            clearMediaItems()
        }
    }

    fun releaseAll() {
        players.forEach { it.release() }
        assigned.clear()
    }

    fun isEmpty(): Boolean = players.isEmpty()
}