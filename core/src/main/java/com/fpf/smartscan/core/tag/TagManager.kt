package com.fpf.smartscan.core.tag


import com.fpf.smartscan.core.data.tags.TagCrossRefRepository
import com.fpf.smartscan.core.data.tags.TagRepository
import com.fpf.smartscan.core.media.MediaItem

class TagManager(
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
) {

    val allTagsFlow = tagRepository.allTags
    val allCollectionsFlow = tagRepository.getCollections()
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


    suspend fun renameTag(tagName: String, newName: String){
        val tag = tagRepository.getTagsByName(listOf(tagName)).firstOrNull()
        tag?.let { tagRepository.updateTags(listOf((it).copy(name = newName))) }
    }

    suspend fun removeItems(tagName: String, items: Set<MediaItem>) {
        val tag = tagRepository.getTagsByName(listOf(tagName)).firstOrNull() ?: return
        items.groupBy { it.type }.forEach { (type, items) ->
            tagCrossRefRepository.deleteMediaMatchTag(items.map{it.id}, tag.id, type)
        }
    }

    suspend fun deleteTagsByName(names: List<String>) = tagRepository.deleteTagsByName(names)
    suspend fun deleteTags(ids: List<Long>) = tagRepository.deleteTagsById(ids)

    suspend fun mergeTags(primaryTagId: Long, otherTags: List<Long>){
        tagCrossRefRepository.moveTagCrossRefs(primaryTagId, otherTags)
        tagRepository.deleteTagsById(otherTags)
    }

    suspend fun moveItems(items: Set<MediaItem>, currentTagName: String, destinationTagName: String){
        val destinationTag = tagRepository.getTagsByName(listOf(destinationTagName)).firstOrNull()?: return
        moveItems(items, currentTagName, destinationTag.id)
    }

    suspend fun createNewTagAndMoveItems(items: Set<MediaItem>, currentTagName: String, newTagName: String){
        val newTagId = tagRepository.insertTags(listOf(NewTag(name = newTagName))).firstOrNull()?: return
        moveItems(items, currentTagName, newTagId)
    }

    private suspend fun moveItems(items: Set<MediaItem>, currentTagName: String, destinationTagId: Long){
        val updatedCrossRef = items.map{ TagCrossRef(mediaId = it.id, tagId = destinationTagId, mediaType = it.type) }
        tagCrossRefRepository.insertTagCrossRefs(updatedCrossRef)

        val currentTag = tagRepository.getTagsByName(listOf(currentTagName)).firstOrNull()?: return
        items.groupBy { it.type }.forEach { (type, items) ->
            tagCrossRefRepository.deleteMediaMatchTag(  items.map{it.id}, currentTag.id, type)
        }
    }
}