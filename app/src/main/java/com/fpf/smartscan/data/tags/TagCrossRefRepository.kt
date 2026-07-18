package com.fpf.smartscan.data.tags

import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.tag.TagCrossRef

class TagCrossRefRepository(private val dao: TagCrossRefDao) {
     suspend fun getAllCrossRefs(): List<TagCrossRef> = dao.getAllCrossRefs().map{it.toDomain()}
     suspend fun insertTagCrossRefs(crossRefs: List<TagCrossRef>) = dao.insert(crossRefs.map{it.toEntity()})
     suspend fun deleteMediaMatchTag(ids: List<Long>, tagId: Long, mediaType: MediaType) = dao.deleteMediaMatchingTag(ids, mediaType, tagId)
     suspend fun clear() = dao.clear()
}