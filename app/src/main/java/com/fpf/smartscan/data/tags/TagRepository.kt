package com.fpf.smartscan.data.tags

import kotlinx.coroutines.flow.Flow

class TagRepository(private val dao: TagDao) {
     val allTags: Flow<List<Tag>> = dao.getAllFlow()

     fun getCollections(): Flow<List<TagCollectionData>> = dao.getCollections()

     suspend fun getAllTags(): List<Tag> = dao.getAll()

     suspend fun getTagsByName(names: List<String>): List<Tag> = dao.getByNames(names)

     suspend fun getTagsById(ids: List<Long>): List<Tag> = dao.getByIds(ids)

     suspend fun insertTags(mediaTags: List<Tag>): List<Long> = dao.insert(mediaTags)

     suspend fun updateTags(mediaTags: List<Tag>) = dao.update(mediaTags)

     suspend fun deleteTags(mediaTags: List<Tag>) = dao.delete(mediaTags)

     suspend fun deleteTagsByName(names: List<String>) = dao.deleteByNames(names)

     suspend fun deleteTagsById(ids: List<Long>) = dao.deleteByIds(ids)

     suspend fun clear() = dao.clear()
}