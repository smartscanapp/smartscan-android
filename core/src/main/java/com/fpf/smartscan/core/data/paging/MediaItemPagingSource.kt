package com.fpf.smartscan.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fpf.smartscan.core.data.mappers.toItem
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy

abstract class MediaItemPagingSource(
    private val filter: SearchFilter = SearchFilter(),
    private val sortBy: SortBy = SortBy.Date(),
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        // over-fetch by 1 item to detect end of data without using count()
        return try {
            val mediaMetadataList = getMediaItems(filter, sortBy=sortBy, pageSize=pageSize, offset=offset)
            val hasMore = mediaMetadataList.size > pageSize
            val pageItems = if (hasMore) mediaMetadataList.dropLast(1) else mediaMetadataList
            val mediaItems = pageItems.map {it.toItem()}

            LoadResult.Page(
                data = mediaItems,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (hasMore) page + 1 else null
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
    }

    protected abstract suspend fun getMediaItems(filter: SearchFilter, sortBy: SortBy, pageSize: Int, offset: Int): List<MediaMetadata>
}