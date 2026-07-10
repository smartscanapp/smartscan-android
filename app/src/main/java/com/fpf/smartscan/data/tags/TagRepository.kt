package com.fpf.smartscan.data.tags

import kotlinx.coroutines.flow.Flow

class TagRepository(private val dao: TagDao) {
     val allTags: Flow<List<TagEntity>> = dao.getAllFlow()

     fun getCollections(): Flow<List<TagCollectionData>> = dao.getCollections()

     suspend fun getAllTags(): List<TagEntity> = dao.getAll()

     suspend fun getTagsByName(names: List<String>): List<TagEntity> = dao.getByNames(names)

     suspend fun getTagsById(ids: List<Long>): List<TagEntity> = dao.getByIds(ids)

     suspend fun insertTags(mediaTags: List<TagEntity>): List<Long> = dao.insert(mediaTags)

     suspend fun updateTags(mediaTags: List<TagEntity>) = dao.update(mediaTags)

     suspend fun deleteTags(mediaTags: List<TagEntity>) = dao.delete(mediaTags)

     suspend fun deleteTagsByName(names: List<String>) = dao.deleteByNames(names)

     suspend fun deleteTagsById(ids: List<Long>) = dao.deleteByIds(ids)

     suspend fun clear() = dao.clear()
}