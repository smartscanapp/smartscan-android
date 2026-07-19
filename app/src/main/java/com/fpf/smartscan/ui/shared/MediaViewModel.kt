package com.fpf.smartscan.ui.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaJobManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaViewModel(
    private val mediaJobManager: MediaJobManager,
    private val tagRepository: TagRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,

    ) : ViewModel() {
    companion object {
        private const val TAG = "MediaViewModel"
    }

    fun updateDescription(updatedMedia: MediaItem){
        mediaJobManager.updateDescription(updatedMedia)
    }

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