package com.fpf.smartscan.media

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import coil3.compose.AsyncImagePainter
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import android.util.Log
import com.fpf.smartscan.data.media.MediaMetadataRepository
import kotlinx.coroutines.flow.SharedFlow
import java.io.FileNotFoundException
import java.lang.SecurityException

const val TAG = "MediaError"
fun onMediaLoadingError( error: AsyncImagePainter.State.Error, onDelete: (Long, MediaType) -> Unit) {
    when (val throwable = error.result.throwable) {
        is SecurityException,
        is FileNotFoundException -> {
            val uri = error.result.request.data
            val id = ContentUris.parseId(uri as Uri)
            if(uri.toString().startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())){
                onDelete(id, MediaType.IMAGE)
            }else if(uri.toString().startsWith(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString())){
                onDelete(id, MediaType.VIDEO)
            }
        }

        else -> {
            Log.e(TAG, "Unhandled media error", throwable)
        }
    }
}

suspend fun removeStaleMedia(idsToPurge: List<Long>, type: MediaType, embedStores: List<FileEmbeddingStore>, mediaMetadataRepository:MediaMetadataRepository){
    embedStores.forEach {
        it.remove(idsToPurge)
    }
    mediaMetadataRepository.deleteByMediaIds(idsToPurge, type)
    mediaMetadataRepository.deleteByMediaIds(idsToPurge, type)
}

