package com.fpf.smartscan.core.data.paging

import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy

class ClusterPagingSource(
    filter: SearchFilter = SearchFilter(),
    sortBy: SortBy = SortBy.Date(),
    private val clusterId: Long,
    private val mediaMetadataRepository: MediaMetadataRepository,
) : MediaItemPagingSource(filter=filter, sortBy=sortBy) {

    override suspend fun getMediaItems(filter: SearchFilter, sortBy: SortBy, pageSize: Int, offset: Int): List<MediaMetadata> =  mediaMetadataRepository.getByCluster(
        clusterId,
        type = filter.mediaType,
        limit = pageSize + 1,
        offset = offset,
        ascending=sortBy.ascending,
        isDuplicate = filter.isDuplicate
    )
}