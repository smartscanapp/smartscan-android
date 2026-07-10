package com.fpf.smartscan.data.tags

import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

class TagCrossRefRepository(private val dao: TagCrossRefDao) {
     suspend fun getAllCrossRefs(): List<TagCrossRef> = dao.getAllCrossRefs()
     suspend fun getTagsForMedia(mediaId: Long): List<Long> = dao.getTagsForMedia(mediaId)
     suspend fun insertTagCrossRefs(crossRefs: List<TagCrossRef>) = dao.insert(crossRefs)
     suspend fun deleteMediaMatchTag(ids: List<Long>, tagId: Long, mediaType: MediaType) = dao.deleteMediaMatchingTag(ids, mediaType, tagId)
     suspend fun clear() = dao.clear()
}