package com.fpf.smartscan.search

import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.dot

suspend fun dedupe(store: FileEmbeddingStore, searchResults: List<Long>, duplicateThreshold: Float): List<Long>{
    val validEmbeds = mutableListOf<Embedding>()
    val validIds = mutableListOf<Long>()

    val resultEmbeds = store.get(searchResults)

    for (res in resultEmbeds){
        var isDuplicate = false
        for(emb in validEmbeds){
            val sim = when(emb){
                is Embedding.F32 -> (res.embedding as Embedding.F32).vector dot emb.vector
                is Embedding.QInt8 ->(res.embedding as Embedding.QInt8).vector dot emb.vector
            }
            if (sim >= duplicateThreshold){
                isDuplicate = true
                break
            }
        }
        if (!isDuplicate){
            validIds.add(res.id)
            validEmbeds.add(res.embedding)
        }
    }
    return validIds
}

fun parseQuery(query: String): Pair<String?, String>{
    val regex = Regex("""^#([a-zA-Z0-9_]+)""")
    val match = regex.find(query)
    val tag = match?.groupValues?.get(1)
    val actualQueryStart = if(!tag.isNullOrBlank()) tag.length + 1 else 0
    val actualQuery = query.substring(actualQueryStart).trim()
    return Pair(tag, actualQuery)
}

fun getPaginatedResult(currentItemsCount: Int, batchSize: Int, cachedIds:  MutableList<Long>): List<Long>{
    val end = (currentItemsCount + batchSize).coerceAtMost(cachedIds.size)
    if (currentItemsCount >= end) return emptyList()
    return cachedIds.subList(currentItemsCount, end)
}