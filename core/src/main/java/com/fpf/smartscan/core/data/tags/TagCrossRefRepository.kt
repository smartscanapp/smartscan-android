package com.fpf.smartscan.core.data.tags

import com.fpf.smartscan.core.data.mappers.toDomain
import com.fpf.smartscan.core.data.mappers.toEntity
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.tag.TagCrossRef

class TagCrossRefRepository(private val dao: TagCrossRefDao) {
     suspend fun getAllCrossRefs(): List<TagCrossRef> = dao.getAllCrossRefs().map{it.toDomain()}
     suspend fun moveTagCrossRefs(primaryTagId: Long, tagIds: List<Long>) = dao.moveCrossRefs(primaryTagId, tagIds)
     suspend fun insertTagCrossRefs(crossRefs: List<TagCrossRef>) = dao.insert(crossRefs.map{it.toEntity()})
     suspend fun deleteMediaMatchTag(ids: List<Long>, tagId: Long, mediaType: MediaType) = dao.deleteMediaMatchingTag(ids, mediaType, tagId)
     suspend fun clear() = dao.clear()
}