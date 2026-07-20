package com.fpf.smartscan.data.paging

import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.search.SortBy

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
            type = filter.mediaType
        )

        val itemsById = items.associateBy { it.id }

        return pageIds.mapNotNull(itemsById::get)
    }
}