package com.fpf.smartscan.core.data.paging

import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.search.SortBy

class BinPagingSource(
    private val trashedIds: List<Long>,
    filter: MediaFilter = MediaFilter(),
    sortBy: SortBy = SortBy.Date(),
    private val mediaMetadataRepository: MediaMetadataRepository,
) : MediaItemPagingSource(
    filter = filter,
    sortBy = sortBy
) {

    override suspend fun getMediaItems(
        filter: MediaFilter,
        sortBy: SortBy,
        pageSize: Int,
        offset: Int
    ): List<MediaMetadata> {

        val pageIds = trashedIds
            .drop(offset)
            .take(pageSize + 1)

        if (pageIds.isEmpty()) {
            return emptyList()
        }

        val items = mediaMetadataRepository.getByIds(
            mediaIds = pageIds,
            type = filter.mediaType,
            isTrashed = true
        )

        val itemsById = items.associateBy { it.id }

        return pageIds.mapNotNull(itemsById::get)
    }
}