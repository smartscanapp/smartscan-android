package com.fpf.smartscan.core.data.paging

import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.search.SortBy

class ConceptPagingSource(
    filter: MediaFilter = MediaFilter(),
    sortBy: SortBy = SortBy.Date(),
    private val conceptId: Long,
    private val mediaMetadataRepository:MediaMetadataRepository,
) : MediaItemPagingSource(filter=filter, sortBy=sortBy) {

    override suspend fun getMediaItems(filter: MediaFilter, sortBy: SortBy, pageSize: Int, offset: Int): List<MediaMetadata> = when (sortBy) {
            is SortBy.Date -> mediaMetadataRepository.getByConceptSortedByDate(
                        conceptId,
                        mediaType=filter.mediaType,
                        limit=pageSize + 1,
                        offset=offset,
                        ascending=sortBy.ascending,
                        )
            is SortBy.Similarity -> mediaMetadataRepository.getByConceptSortedBySimilarity(
                conceptId,
                mediaType = filter.mediaType,
                limit = pageSize + 1,
                offset=offset,
                ascending=sortBy.ascending,
            )
        }
}