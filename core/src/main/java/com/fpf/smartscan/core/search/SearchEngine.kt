package com.fpf.smartscan.core.search

import android.content.Context
import android.util.Log
import com.fpf.smartscan.core.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.models.ModelRepository
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.QueryResult
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder

// TODO: add support for concepts

class SearchEngine(
    private val dualEncoderVlm: Pair<TextEmbeddingProvider, ImageEmbeddingProvider>,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
    private val videoConceptEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val modelRepository: ModelRepository,
    private val defaultTextQueryThreshold: Float = 0.2f,
    private val defaultImageQueryThreshold: Float = 0.5f,
    private val dedupeThreshold: Float = 0.95f,
    ) {

    companion object {
        private const val TAG = "SearchEngine"
    }

     private val dualEngineVlmTextEmbedder: TextEmbeddingProvider
         get() = dualEncoderVlm.first

    private val dualEngineVlmImageEmbedder: ImageEmbeddingProvider
        get() = dualEncoderVlm.second

    private val miniLmTextEmbedder by lazy {modelRepository.getMiniLmTextEmbedder()}

    suspend fun search(context: Context, searchQuery: SearchQuery): List<Long>{
        require(searchQuery.filter.mediaType != null){"Media type is require"} // TODO: null searches both?
        val store = getStore(searchQuery.filter.mediaType!!)
        if(!store.exists) return emptyList()

        try {
            val queryResults = when(searchQuery) {
                is SearchQuery.ImageQuery -> imageSearch(context, searchQuery)
                is SearchQuery.TextQuery -> textSearch(searchQuery)
            }
            return postProcess(queryResults, store, searchQuery.options)
        }catch (e: Exception) {
            Log.e(TAG, "Search engine error", e)
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
        if(!miniLmTextEmbedder.isInitialized())miniLmTextEmbedder.initialize()

        val store = getStore(searchQuery.filter.mediaType)
        val embedding = dualEngineVlmTextEmbedder.embed(query)
        val queryEmbed = embedding.toQInt8Embed()
        val threshold = searchQuery.filter.similarity?: defaultTextQueryThreshold
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, threshold, searchQuery.filter.ids.toSet(),  startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, threshold, includeSims = true)
        val mainSims = queryResult.toSimsMap()
        val clusterSims = clusterResult.toSimsMap()
        val itemClusterSims = toScoreMap(mainSims, clusterSims, getItemToClusterSimMap(searchQuery.filter.mediaType))

        val conceptQueryEmbed = miniLmTextEmbedder.embed(query).toQInt8Embed()
        val conceptStore = getConceptStore(searchQuery.filter.mediaType)
        val conceptQueryResult = conceptStore.query(conceptQueryEmbed, Int.MAX_VALUE, threshold, searchQuery.filter.ids.toSet(),  startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val conceptSims = conceptQueryResult.toSimsMap()
        val signals = getSearchSignals(mainSims, itemClusterSims, conceptSims)
        val reranked = Reranker.rerank(signals)
        return reranked
    }

    private suspend fun imageSearch(context: Context, searchQuery: SearchQuery.ImageQuery): List<Long> {
        if(!dualEngineVlmImageEmbedder.isInitialized()) dualEngineVlmImageEmbedder.initialize()
        require(searchQuery.filter.mediaType != null){"Media type is require"} // TODO: null searches both?

        val bitmap = getBitmapFromUri(context, searchQuery.uri, ClipImageEmbedder.IMAGE_SIZE_X)
        val embedding = dualEngineVlmImageEmbedder.embed(bitmap)
        val queryEmbed= embedding.toQInt8Embed()
        val store = getStore(searchQuery.filter.mediaType)
        val threshold = searchQuery.filter.similarity?: defaultImageQueryThreshold
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, threshold, startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, threshold, includeSims = true)
        val mainSims = queryResult.toSimsMap()
        val clusterSims = clusterResult.toSimsMap()
        val itemClusterSims = toScoreMap(mainSims, clusterSims, getItemToClusterSimMap(searchQuery.filter.mediaType))
        val signals = getSearchSignals(mainSims, itemClusterSims)
        val reranked = Reranker.rerank(signals)
        return reranked
    }

    private suspend fun hideDuplicates(resultIds: List<Long>, store: FileEmbeddingStore): List<Long> {
        return dedupe(store.get(resultIds), dedupeThreshold)
    }

    private fun getSearchSignals(mainSims: Map<Long, Float>, itemClusterSims: Map<Long, Float>, conceptSims: Map<Long, Float>? = null): List<RerankSignal>{
        val signals = mutableListOf<RerankSignal>()
        val mainSignal = RerankSignal(scores = mainSims, key = 0)
        val clusterSignal = RerankSignal(scores = itemClusterSims, key = 1)
        signals.addAll(listOf(mainSignal, clusterSignal))
        conceptSims?.let{ signals.add( RerankSignal(scores = conceptSims, key =2))}
        return signals
    }

    private fun getStore(mediaType: MediaType) = if(mediaType == MediaType.VIDEO) videoEmbedStore else imageEmbedStore
    private fun getConceptStore(mediaType: MediaType) = if(mediaType == MediaType.VIDEO) videoConceptEmbedStore else imageConceptEmbedStore
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
