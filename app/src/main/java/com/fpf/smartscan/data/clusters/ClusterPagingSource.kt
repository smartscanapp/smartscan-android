package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.data.MediaItemPagingSource
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.search.SortBy

// TODO: add sort by similarity
class ClusterPagingSource(
    filter: SearchFilter = SearchFilter(),
    sortBy: SortBy = SortBy.Date(),
    private val clusterId: Long,
    private val mediaMetadataRepository: MediaMetadataRepository,
) : MediaItemPagingSource(filter=filter, sortBy=sortBy) {

    override suspend fun getMediaItems(filter: SearchFilter, sortBy: SortBy, pageSize: Int, offset: Int): List<MediaMetadata> = when {
        filter.mediaType != null -> {
            mediaMetadataRepository.getByCluster(
                clusterId,
                type = filter.mediaType,
                limit = pageSize + 1,
                offset = offset,
                ascending=sortBy.ascending,
                )
        }
        else ->
            mediaMetadataRepository.getByCluster(
                clusterId,
                limit=pageSize + 1,
                offset=offset,
                ascending=sortBy.ascending,
                )
    }
}