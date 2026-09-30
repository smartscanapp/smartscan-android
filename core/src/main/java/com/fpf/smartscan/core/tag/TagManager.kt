package com.fpf.smartscan.core.tag


import com.fpf.smartscan.core.collections.CollectionManager
import com.fpf.smartscan.core.collections.MediaCollection
import com.fpf.smartscan.core.data.tags.TagCrossRefRepository
import com.fpf.smartscan.core.data.tags.TagRepository
import com.fpf.smartscan.core.media.MediaItem
import kotlinx.coroutines.flow.first

class TagManager(
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
): CollectionManager<MediaItem, MediaCollection>  {

    val allTagsFlow = tagRepository.allTags
    val allCollectionsFlow = tagRepository.getCollections()

    override suspend fun rename(collectionId: Long, name: String){
        val tag = tagRepository.getTagsById(listOf(collectionId)).firstOrNull()
        tag?.let { tagRepository.updateTags(listOf((it).copy(name = name))) }
    }

    override suspend fun remove(collectionId: Long, items: Set<MediaItem>) {
        val tag = tagRepository.getTagsById(listOf(collectionId)).firstOrNull() ?: return
        items.groupBy { it.type }.forEach { (type, items) ->
            tagCrossRefRepository.deleteMediaMatchTag(items.map{it.id}, tag.id, type)
        }
    }

    override suspend fun delete(collectionIds: List<Long>) = tagRepository.deleteTagsById(collectionIds)

    override suspend fun merge(primaryCollectionId: Long, secondaryCollectionIds: List<Long>){
        tagCrossRefRepository.moveTagCrossRefs(primaryCollectionId, secondaryCollectionIds)
        tagRepository.deleteTagsById(secondaryCollectionIds)
    }

    override suspend fun move(items: Set<MediaItem>,  newCollectionId: Long, currentCollectionId: Long){
        moveItems(items, currentCollectionId, newCollectionId)
    }

    override suspend fun getCollections(): List<MediaCollection> = allCollectionsFlow.first()

    suspend fun tagItems( tagName: String, items: Set<MediaItem>){
        val existing = tagRepository.getTagsByName(listOf(tagName)).firstOrNull()
        var id = existing?.id
        if(id == null){
            id = tagRepository.insertTags(listOf(NewTag(name = tagName.trim()))).first()
        }
        val tagEntries = items.map { TagCrossRef(mediaId = it.id, tagId = id, mediaType = it.type) }
        tagCrossRefRepository.insertTagCrossRefs(tagEntries)
    }

    fun checkAutoCompletion(query: CharSequence, substringEnd: Int, tags: List<String>, startWithHashtag: Boolean =  true): List<String>{
        val text = query.toString()
        val safeEnd = substringEnd.coerceIn(0, text.length)
        val prefix = text.substring(0, safeEnd)
        // Regex: find #tag at the end of prefix
        var pattern =  """^#([a-zA-Z0-9]*)$"""
        pattern = if(!startWithHashtag )  pattern.replace("#", "") else pattern
        val match = Regex(pattern).find(prefix)
        return if (match != null) {
            val partialTag =  match.groupValues[1]
            tags .filter { it.startsWith(partialTag, ignoreCase = true) }
        } else {
            emptyList()
        }
    }


    suspend fun updateLastUsage(tagName: String){
        val tag = tagRepository.getTagsByName(listOf(tagName)).firstOrNull()?: return
        tagRepository.updateTags(listOf(Tag(tag.id, tag.name, System.currentTimeMillis())))
    }

    suspend fun getTagByName(name: String): Tag? = tagRepository.getTagsByName(listOf(name)).firstOrNull()


    suspend fun createNewTagAndMoveItems(items: Set<MediaItem>, currentTagId: Long, name: String){
        val newTagId = tagRepository.insertTags(listOf(NewTag(name = name))).firstOrNull()?: return
        moveItems(items, currentTagId, newTagId)
    }

    private suspend fun moveItems(items: Set<MediaItem>, currentTagId: Long, newTagId: Long){
        val updatedCrossRef = items.map{ TagCrossRef(mediaId = it.id, tagId = newTagId, mediaType = it.type) }
        tagCrossRefRepository.insertTagCrossRefs(updatedCrossRef)

        items.groupBy { it.type }.forEach { (type, items) ->
            tagCrossRefRepository.deleteMediaMatchTag(  items.map{it.id}, currentTagId, type)
        }
    }
}