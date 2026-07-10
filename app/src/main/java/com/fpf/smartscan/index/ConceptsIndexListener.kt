package com.fpf.smartscan.index

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.fpf.smartscan.R
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.utils.getTimeInMinutesAndSeconds
import com.fpf.smartscan.utils.showNotification
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.core.processors.ProcessorListener

abstract class BaseConceptIndexListener(private val notificationId: Int, private val tag: String) : ProcessorListener<MediaMetadata, Pair<MediaMetadata, Embedding>> {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _indexingStatus = MutableStateFlow(IndexingStatus.IDLE)
    val indexingStatus: StateFlow<IndexingStatus> = _indexingStatus

    abstract val itemName: String

    override suspend fun onProgress(context: Context, progress: Float) {
        _progress.value = progress
    }

    override suspend fun onActive(context: Context) {
        _indexingStatus.value = IndexingStatus.ACTIVE
    }

    override suspend fun onComplete(context: Context, metrics: Metrics.Success) {
        try {
            _indexingStatus.value = IndexingStatus.COMPLETE
            _progress.value = 0f
            if (metrics.totalProcessed == 0) return
            val (minutes, seconds) = getTimeInMinutesAndSeconds(metrics.timeElapsed)
            val notificationText = "Total ${itemName.lowercase()}s indexed: ${metrics.totalProcessed}, Time: ${minutes}m ${seconds}s"
            showNotification(
                context,
                context.getString(R.string.notif_title_index_complete),
                notificationText,
                notificationId
            )
        } catch (e: Exception) {
            Log.e(tag, "Error in onComplete: ${e.message}", e)
        }
    }

    override suspend fun onFail(context: Context, failureMetrics: Metrics.Failure) {
        try {
            _indexingStatus.value = IndexingStatus.FAILED
            _progress.value = 0f
            val title = context.getString(R.string.notif_title_index_error_service, itemName)
            val content = context.getString(R.string.notif_content_index_error_service)
            showNotification(context, title, content, notificationId)
        } catch (e: Exception) {
            Log.e(tag, "Error in onFail: ${e.message}", e)
        }
    }

    override fun onError(context: Context, error: Exception, item: MediaMetadata) {
        Log.e(tag, "Error during processing: ${error.message}", error)
    }

    fun reset(){
        _indexingStatus.value = IndexingStatus.IDLE
        _progress.value = 0f
    }
}

object ConceptImageIndexListener : BaseConceptIndexListener(
    notificationId = 1002,
    tag = "CloudImageIndexListener"
) {
    override val itemName: String = "Image"
}
