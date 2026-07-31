package com.fpf.smartscan.core.index

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.core.processors.ProcessorListener

abstract class BaseIndexListener : ProcessorListener<Long, Pair<Long, Embedding>> {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _indexingStatus = MutableStateFlow(IndexingStatus.IDLE)
    val indexingStatus: StateFlow<IndexingStatus> = _indexingStatus

    private val _result = MutableStateFlow<Metrics?>(null)
    val result: StateFlow<Metrics?> = _result

    abstract val itemName: String

    override suspend fun onProgress(context: Context, progress: Float) {
        _progress.value = progress
    }

    override suspend fun onActive(context: Context) {
        _indexingStatus.value = IndexingStatus.ACTIVE
    }

    override suspend fun onComplete(context: Context, metrics: Metrics.Success) {
        _indexingStatus.value = IndexingStatus.COMPLETE
        _progress.value = 0f
        _result.value = metrics
    }

    override suspend fun onFail(context: Context, failureMetrics: Metrics.Failure) {
        _indexingStatus.value = IndexingStatus.FAILED
        _progress.value = 0f
        _result.value = failureMetrics
    }

    fun reset(){
        _indexingStatus.value = IndexingStatus.IDLE
        _progress.value = 0f
        _result.value = null
    }
}

object ImageIndexListener : BaseIndexListener() {
    override val itemName: String = "Image"
}

object VideoIndexListener : BaseIndexListener() {
    override val itemName: String = "Video"
}