package com.fpf.smartscan.data.tags

import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.tag.NewTag
import com.fpf.smartscan.tag.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepository(private val dao: TagDao) {
     val allTags: Flow<List<Tag>> = dao.getAllFlow().map{tags -> tags.map{it.toDomain()}}

     fun getCollections(): Flow<List<MediaCollection>> = dao.getCollections().map{ collections -> collections.map{it.toDomain()}}

     suspend fun getCollections(tagIds: List<Long>): List<MediaCollection> = dao.getCollections(tagIds).map{ it.toDomain()}

     suspend fun getAllTags(): List<Tag> = dao.getAll().map{it.toDomain()}

     suspend fun getTagsByName(names: List<String>): List<Tag> = dao.getByNames(names).map{it.toDomain()}

     suspend fun getTagsById(ids: List<Long>): List<Tag> = dao.getByIds(ids).map{it.toDomain()}

     suspend fun insertTags(mediaTags: List<NewTag>): List<Long> = dao.insert(mediaTags.map{it.toEntity()})

     suspend fun updateTags(mediaTags: List<Tag>) = dao.update(mediaTags.map{it.toEntity()})

     suspend fun deleteTags(mediaTags: List<Tag>) = dao.delete(mediaTags.map{it.toEntity()})

     suspend fun deleteTagsByName(names: List<String>) = dao.deleteByNames(names)

     suspend fun deleteTagsById(ids: List<Long>) = dao.deleteByIds(ids)

     suspend fun clear() = dao.clear()
}