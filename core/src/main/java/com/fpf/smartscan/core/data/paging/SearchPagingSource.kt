package com.fpf.smartscan.core.data.paging

import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy

class SearchPagingSource(
    private val resultIds: List<Long>,
    filter: SearchFilter = SearchFilter(),
    sortBy: SortBy = SortBy.Date(),
    private val mediaMetadataRepository: MediaMetadataRepository,
) : MediaItemPagingSource(
    filter = filter,
    sortBy = sortBy
) {

    override suspend fun getMediaItems(
        filter: SearchFilter,
        sortBy: SortBy,
        pageSize: Int,
        offset: Int
    ): List<MediaMetadata> {

        val pageIds = resultIds
            .drop(offset)
            .take(pageSize + 1)

        if (pageIds.isEmpty()) {
            return emptyList()
        }


        require(filter.mediaType != null ){"Media type must be provided"}

        val items = mediaMetadataRepository.getByIds(
            mediaIds = pageIds,
            type = filter.mediaType,
            isDuplicate = filter.isDuplicate
        )

        val itemsById = items.associateBy { it.id }

        return pageIds.mapNotNull(itemsById::get)
    }
}