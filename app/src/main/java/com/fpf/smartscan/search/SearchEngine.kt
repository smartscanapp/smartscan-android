package com.fpf.smartscan.search

import android.app.Application
import android.util.Log
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.errors.AppException
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.QueryResult
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder

// TODO: add support for concepts

class SearchEngine(
    private val application: Application,
    private val dualEncoderVlm: Pair<TextEmbeddingProvider, ImageEmbeddingProvider>,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val defaultTextQueryThreshold: Float = 0.2f,
    private val defaultImageQueryThreshold: Float = 0.5f,
    private val dedupeThreshold: Float = 0.95f,
    ) {

    companion object {
        private const val TAG = "SearchEngine"
    }

     val dualEngineVlmTextEmbedder: TextEmbeddingProvider
         get() = dualEncoderVlm.first

    val dualEngineVlmImageEmbedder: ImageEmbeddingProvider
        get() = dualEncoderVlm.second

    suspend fun search(searchQuery: SearchQuery): List<Long>{
        require(searchQuery.filter.mediaType != null){"Media type is require"} // TODO: null searches both?
        val store = getStore(searchQuery.filter.mediaType!!)
        if(!store.exists) return emptyList()

        try {
            val queryResults = when(searchQuery) {
                is SearchQuery.ImageQuery -> imageSearch(searchQuery)
                is SearchQuery.TextQuery -> textSearch(searchQuery)
            }
            return postProcess(queryResults, store, searchQuery.options)
        }catch (e: Exception) {
            Log.e(TAG, "Search engine error: $e")
            throw AppException.SearchException(cause = e)
        }
    }

    fun parseTextQuery(query: String): Pair<String?, String>{
        val regex = Regex("""^#([a-zA-Z0-9_]+)""")
        val match = regex.find(query)
        val tag = match?.groupValues?.get(1)
        val actualQueryStart = if(!tag.isNullOrBlank()) tag.length + 1 else 0
        val actualQuery = query.substring(actualQueryStart).trim()
        return Pair(tag, actualQuery)
    }

    private suspend fun postProcess(results: List<Long>, store: FileEmbeddingStore, options: SearchOptions): List<Long> {
        return if(options.hideDuplicates) hideDuplicates(results, store) else results
    }
    private suspend fun textSearch(searchQuery: SearchQuery.TextQuery): List<Long> {
        val query = searchQuery.text
        if (query.isBlank()) {
            return emptyList()
        }
        require(searchQuery.filter.mediaType != null){"Media type is require"} // TODO: null searches both?
        if(!dualEngineVlmTextEmbedder.isInitialized())dualEngineVlmTextEmbedder.initialize()

        val store = getStore(searchQuery.filter.mediaType)
        val embedding = dualEngineVlmTextEmbedder.embed(query)
        val queryEmbed = embedding.toQInt8Embed()
        val threshold = searchQuery.filter.similarity?: defaultTextQueryThreshold
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, threshold, searchQuery.filter.ids.toSet(),  startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, threshold, includeSims = true)
        val itemToSimMap = queryResult.toSimsMap()
        val clusterToSimMap = clusterResult.toSimsMap()
        val itemsToClusterSims = toScoreMap(itemToSimMap, clusterToSimMap, getItemToClusterSimMap(searchQuery.filter.mediaType))
        val signals = getSearchSignals(itemsToClusterSims)
        val reranked = Reranker.rerank(itemToSimMap, signals, searchQuery.options.strictness)
        return reranked
    }

    private suspend fun imageSearch(searchQuery: SearchQuery.ImageQuery): List<Long> {
        if(!dualEngineVlmImageEmbedder.isInitialized()) dualEngineVlmImageEmbedder.initialize()
        require(searchQuery.filter.mediaType != null){"Media type is require"} // TODO: null searches both?

        val bitmap = getBitmapFromUri(application, searchQuery.uri, ClipImageEmbedder.IMAGE_SIZE_X)
        val embedding = dualEngineVlmImageEmbedder.embed(bitmap)
        val queryEmbed= embedding.toQInt8Embed()
        val store = getStore(searchQuery.filter.mediaType)
        val threshold = searchQuery.filter.similarity?: defaultImageQueryThreshold
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, threshold, startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, threshold, includeSims = true)
        val itemToSimMap = queryResult.toSimsMap()
        val clusterToSimMap = clusterResult.toSimsMap()
        val itemsToClusterSims = toScoreMap(itemToSimMap, clusterToSimMap, getItemToClusterSimMap(searchQuery.filter.mediaType))
        val signals = getSearchSignals(itemsToClusterSims)
        val reranked = Reranker.rerank(itemToSimMap, signals, searchQuery.options.strictness)
        return reranked
    }

    private suspend fun hideDuplicates(resultIds: List<Long>, store: FileEmbeddingStore): List<Long> {
        return dedupe(store.get(resultIds), dedupeThreshold)
    }

    private fun getSearchSignals(itemClusterSimMap: Map<Long, Float>, conceptSimMap: Map<Long, Float>? = null): List<RerankSignal>{
        val signals = mutableListOf<RerankSignal>()
        val clusterSignal = RerankSignal(scores = itemClusterSimMap)
        signals.add(clusterSignal)

        conceptSimMap?.let{ signals.add( RerankSignal(scores = conceptSimMap))}
        return signals
    }

    private fun getStore(mediaType: MediaType) = if(mediaType == MediaType.VIDEO) videoEmbedStore else imageEmbedStore

    private suspend fun getItemToClusterSimMap(mediaType: MediaType) = clusterCrossRefRepository.getClusterToMediaIdsMap().filterKeys{it.second == mediaType}.map{it.key.first to it.value}.associate { it.first to it.second }
}

fun toScoreMap(itemSimMap: Map<Long, Float>, clusterSims: Map<Long, Float>, clusterToMediaMap:  Map<Long, MutableSet<Long>>): Map<Long, Float>{
    val itemToCluster = buildMap {
        clusterToMediaMap.forEach { (clusterId, items) ->
            items.forEach { itemId -> put(itemId, clusterId) }
        }
    }

    return itemSimMap.keys.associateWith { itemId -> itemToCluster[itemId]?.let(clusterSims::get) ?: 0f }
}

fun QueryResult.toSimsMap(): Map<Long, Float> = this.sims?.let(this.ids::zip)?.toMap() ?: emptyMap()
