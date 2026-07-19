package com.fpf.smartscan.data.concepts

import com.fpf.smartscan.data.MediaItemPagingSource
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.search.SortBy

class ConceptPagingSource(
    filter: SearchFilter = SearchFilter(),
    sortBy: SortBy = SortBy.Date(),
    private val conceptId: Long,
    private val mediaMetadataRepository: MediaMetadataRepository,
) : MediaItemPagingSource(filter=filter, sortBy=sortBy) {

    override suspend fun getMediaItems(filter: SearchFilter, sortBy: SortBy, pageSize: Int, offset: Int): List<MediaMetadata> = when (sortBy) {
            is SortBy.Date ->
                if (filter.mediaType != null) {
                    mediaMetadataRepository.getByConceptSortedByDate(
                        conceptId,
                        mediaType=filter.mediaType,
                        limit=pageSize + 1,
                        offset=offset,
                        ascending=sortBy.ascending,
                        )
                } else {
                    mediaMetadataRepository.getByConceptSortedByDate(
                        conceptId,
                        limit=pageSize + 1,
                        offset=offset,
                        ascending=sortBy.ascending,
                        )
                }

            is SortBy.Similarity ->
                when {
                    filter.mediaType != null && filter.similarity != null ->
                        mediaMetadataRepository.getByConceptSortedBySimilarity(
                            conceptId,
                            mediaType = filter.mediaType,
                            minSimilarity = filter.similarity,
                            limit = pageSize + 1,
                            offset=offset,
                            ascending=sortBy.ascending,
                            )

                    filter.mediaType != null ->
                        mediaMetadataRepository.getByConceptSortedBySimilarity(
                            conceptId,
                            mediaType = filter.mediaType,
                            limit = pageSize + 1,
                            offset=offset,
                            ascending=sortBy.ascending,
                            )

                    filter.similarity != null ->
                        mediaMetadataRepository.getByConceptSortedBySimilarity(
                            conceptId,
                            minSimilarity = filter.similarity,
                            limit = pageSize + 1,
                            offset=offset,
                            ascending=sortBy.ascending,
                            )

                    else ->
                        mediaMetadataRepository.getByConceptSortedBySimilarity(
                            conceptId,
                            limit=pageSize + 1,
                            offset=offset,
                            ascending=sortBy.ascending,
                            )
                }
        }
}