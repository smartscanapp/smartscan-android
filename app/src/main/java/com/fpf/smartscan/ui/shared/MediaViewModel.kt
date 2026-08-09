package com.fpf.smartscan.ui.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.core.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.data.tags.TagRepository
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaJobManager
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.media.onMediaLoadingError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaViewModel(
    private val mediaJobManager: MediaJobManager,
    private val tagRepository: TagRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository
) : ViewModel() {
    companion object {
        private const val TAG = "MediaViewModel"
    }

    fun delete(items: List<MediaItem>){
        val (images, videos) = items.partition { it.type == MediaType.IMAGE }
        viewModelScope.launch(Dispatchers.IO) {
            if(images.isNotEmpty()) mediaJobManager.delete(images.map{it.id}, MediaType.IMAGE)
            if(videos.isNotEmpty()) mediaJobManager.delete(videos.map{it.id}, MediaType.VIDEO)
        }
    }

    fun delete(id: Long, mediaType: MediaType){
        viewModelScope.launch(Dispatchers.IO) {
            mediaJobManager.delete(listOf(id), mediaType)
        }
    }
    fun trash(items: List<MediaItem>){
        viewModelScope.launch(Dispatchers.IO) {
            mediaJobManager.trash(items)
        }
    }

    fun restore(items: List<MediaItem>){
        viewModelScope.launch(Dispatchers.IO) {
            mediaJobManager.restore(items)
        }
    }
    fun updateDescription(updatedMedia: MediaItem) = mediaJobManager.updateDescription(updatedMedia)

    fun findAndMarkDuplicates(mediaType: MediaType) = mediaJobManager.findAndMarkDuplicates(mediaType)

    fun viewCollection(collectionId: Long, type: CollectionType, onViewCollection: (MediaCollection) -> Unit){
        viewModelScope.launch(Dispatchers.IO) {
            val collection = when (type) {
                CollectionType.CLUSTER -> clusterMetadataRepository.getCollections(listOf(collectionId)).firstOrNull()
                CollectionType.TAG -> tagRepository.getCollections(listOf(collectionId)).firstOrNull()
            }
            if(collection != null){
                withContext(Dispatchers.Main){
                    onViewCollection(collection)
                }
            }else{
                Log.e(TAG, "Collection not found: $collection")
            }
        }
    }


    fun onErrorAsyncImage(error: AsyncImagePainter.State.Error){
        viewModelScope.launch (Dispatchers.IO){
            onMediaLoadingError(error){
                id, mediaType -> delete(id, mediaType)
            }
        }
    }

    // Only collection id and name are required hence why getCollections no used here as that method does unncessary db work
    suspend fun getCollectionsMatchingMedia(media: MediaItem, collectionType: CollectionType ): MutableMap<Long, String> {
        return when(collectionType){
            CollectionType.TAG -> getTagsMatchingMedia(media)
            CollectionType.CLUSTER -> getClustersMatchingMedia(media)
        }
    }

    private suspend fun getTagsMatchingMedia(media: MediaItem): MutableMap<Long, String> {
        val tags = tagRepository.getTagsForMedia(media.id, media.type)
        return tags.associate { it.id to it.name }.toMutableMap()
    }

    private suspend fun getClustersMatchingMedia(media: MediaItem): MutableMap<Long, String> {
        val clusters = clusterMetadataRepository.getClustersForMedia(media.id, media.type)
        return  clusters
            .filter { !it.label.isNullOrBlank() }
            .associate { it.clusterId to it.label!! }.toMutableMap()
    }
}