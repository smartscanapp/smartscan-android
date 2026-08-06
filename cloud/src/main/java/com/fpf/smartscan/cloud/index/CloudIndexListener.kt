package com.fpf.smartscan.cloud.index

import android.content.Context
import android.util.Log
import com.fpf.smartscan.core.index.IndexingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscansdk.core.processors.ProcessorListener
import com.fpf.smartscansdk.core.processors.ProcessorResult


abstract class BaseCloudIndexListener(private val tag: String) : ProcessorListener<MediaMetadata> {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _indexingStatus = MutableStateFlow(IndexingStatus.IDLE)
    val indexingStatus: StateFlow<IndexingStatus> = _indexingStatus

    private val _result = MutableStateFlow<ProcessorResult?>(null)
    val result: StateFlow<ProcessorResult?> = _result

    abstract val itemName: String

    override suspend fun onProgress(context: Context, progress: Float) {
        _progress.value = progress
    }

    override suspend fun onActive(context: Context) {
        _indexingStatus.value = IndexingStatus.ACTIVE
    }

    override suspend fun onComplete(context: Context, result: ProcessorResult.Success) {
        _indexingStatus.value = IndexingStatus.COMPLETE
        _progress.value = 0f
        _result.value = result
    }

    override suspend fun onFail(context: Context, result: ProcessorResult.Failure) {
        _indexingStatus.value = IndexingStatus.FAILED
        _progress.value = 0f
        _result.value = result
    }

    override suspend fun onError(context: Context, error: Exception, item: MediaMetadata) {
        Log.e(tag, "Error during processing: ${error.message}", error)
    }

    fun reset(){
        _indexingStatus.value = IndexingStatus.IDLE
        _progress.value = 0f
        _result.value = null
    }
}

object CloudImageIndexListener : BaseCloudIndexListener(
    tag = "CloudImageIndexListener"
) {
    override val itemName: String = "Image"
}
