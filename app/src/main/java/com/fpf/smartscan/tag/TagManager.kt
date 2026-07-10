package com.fpf.smartscan.tag

import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.TagEntity
import com.fpf.smartscan.data.tags.TagCrossRefEntity
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType

class TagManager(
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository
) {
    suspend fun tagItems( tagName: String, items: Set<MediaItem>){
        val existing = tagRepository.getTagsByName(listOf(tagName)).firstOrNull()
        var id = existing?.id
        if(id == null){
            id = tagRepository.insertTags(listOf(TagEntity(name = tagName.trim()))).first()
        }
        val tagEntries = items.map { TagCrossRefEntity(mediaId = it.id, tagId = id, mediaType = it.type) }
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

    suspend fun getMediaMatchingTag(tagName: String?, mediaType: MediaType, startDateFilter: Long? = null, endDateFilter: Long? = null): List<Long>{
        tagName?: return emptyList()
        val tag = tagRepository.getTagsByName(listOf(tagName)).firstOrNull()
        return if(endDateFilter != null || startDateFilter != null){
            tag?.let { tag-> mediaMetadataRepository.getByTag(tag.id, mediaType,startDateFilter, endDateFilter).map{it.id}  }?: emptyList()
        }else{
            tag?.let { tag-> mediaMetadataRepository.getByTag(tag.id, mediaType).map{it.id}  }?: emptyList()
        }
    }

    suspend fun updateLastUsage(tagName: String){
        val tag = tagRepository.getTagsByName(listOf(tagName)).firstOrNull()?: return
        tagRepository.updateTags(listOf(TagEntity(tag.id, tag.name, System.currentTimeMillis())))
    }

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

    suspend fun mergeTags(primaryTagName: String, otherTags: List<String>){
        val primaryTag = tagRepository.getTagsByName(listOf(primaryTagName)).firstOrNull()
        val tagsToMerge = tagRepository.getTagsByName(otherTags)
        val mediaToUpdate = tagsToMerge.flatMap { mediaMetadataRepository.getByTag(it.id) }
        if(primaryTag != null && mediaToUpdate.isNotEmpty()){
            val updated = mediaToUpdate.map{ TagCrossRefEntity(mediaId = it.id, tagId = primaryTag.id, mediaType = it.type) }
            tagCrossRefRepository.insertTagCrossRefs(updated)
            tagRepository.deleteTags(tagsToMerge)
        }
    }

    suspend fun moveItems(items: Set<MediaItem>, currentTagName: String, destinationTagName: String){
        val destinationTag = tagRepository.getTagsByName(listOf(destinationTagName)).firstOrNull()?: return
        moveItems(items, currentTagName, destinationTag.id)
    }

    suspend fun createNewTagAndMoveItems(items: Set<MediaItem>, currentTagName: String, newTagName: String){
        val newTagId = tagRepository.insertTags(listOf(TagEntity(name = newTagName))).firstOrNull()?: return
        moveItems(items, currentTagName, newTagId)
    }

    private suspend fun moveItems(items: Set<MediaItem>, currentTagName: String, destinationTagId: Long){
        val updatedCrossRef = items.map{ TagCrossRefEntity(mediaId = it.id, tagId = destinationTagId, mediaType = it.type) }
        tagCrossRefRepository.insertTagCrossRefs(updatedCrossRef)

        val currentTag = tagRepository.getTagsByName(listOf(currentTagName)).firstOrNull()?: return
        items.groupBy { it.type }.forEach { (type, items) ->
            tagCrossRefRepository.deleteMediaMatchTag(  items.map{it.id}, currentTag.id, type)
        }
    }
}