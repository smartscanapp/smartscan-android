package com.fpf.smartscan.cloud.index

import android.content.Context
import android.util.Log
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscan.core.index.BaseIndexListener
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpflabs.llmconnect.HttpException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CloudImageIndexListener : BaseIndexListener(
    tag = "CloudImageIndexListener"
) {
    override val itemName: String = "Image"

    override suspend fun onError( error: Exception, item: MediaMetadata) {
        if (error is HttpException) {
            when (error.statusCode) {
                401, 403 -> throw AppException.InvalidApiKey()
                429 ->  throw AppException.RateLimit()
            }
        }
        Log.e(tag, "Error during processing", error)
    }
}
