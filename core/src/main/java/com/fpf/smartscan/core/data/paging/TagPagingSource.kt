package com.fpf.smartscan.core.data.paging

import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy

class TagPagingSource(
    filter: SearchFilter = SearchFilter(),
    sortBy: SortBy = SortBy.Date(),
    private val tagId: Long,
    private val mediaMetadataRepository: MediaMetadataRepository,
) : MediaItemPagingSource(filter=filter, sortBy=sortBy) {

    override suspend fun getMediaItems(filter: SearchFilter, sortBy: SortBy, pageSize: Int, offset: Int): List<MediaMetadata> = when {
        filter.mediaType != null -> {
            mediaMetadataRepository.getByTag(
                tagId,
                mediaType = filter.mediaType,
                limit = pageSize + 1,
                offset = offset,
                ascending = sortBy.ascending

            )
        }
        else ->
            mediaMetadataRepository.getByTag(
                tagId,
                limit=pageSize + 1,
                offset=offset,
                ascending = sortBy.ascending
            )
    }
}