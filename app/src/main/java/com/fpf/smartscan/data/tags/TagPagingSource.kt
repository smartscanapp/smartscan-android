package com.fpf.smartscan.data.tags

import android.net.Uri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.toItem

class TagPagingSource(
    private val mediaType: MediaType? = null,
    private val tagId: Long,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val mediaIdToUri: (Long, MediaType) -> Uri
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        // over-fetch by 1 item to detect end of data without using count()
        return try {
            val mediaMetadataList = if(mediaType != null){
                mediaMetadataRepository.getByTag(tagId = tagId, mediaType, limit = pageSize + 1, offset = offset)
            }else{
                mediaMetadataRepository.getByTag(tagId = tagId, limit = pageSize + 1, offset = offset)
            }
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
}