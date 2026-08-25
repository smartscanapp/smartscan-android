package com.fpf.smartscan.core.search

import android.content.Context
import android.util.Log
import com.fpf.smartscan.core.concepts.ConceptManager
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

class SearchEngine(
    private val dualEncoderVlm: Pair<TextEmbeddingProvider, ImageEmbeddingProvider>,
    private val modelRepository: ModelRepository,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
    private val videoConceptEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val vlmTextSimThreshold: Float = 0.2f,
    private val vlmImageSimThreshold: Float = 0.5f,
    private val conceptSimThreshold: Float = ConceptManager.DEFAULT_SIMILARITY_THRESHOLD,
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
        try {
            require(searchQuery.filter.mediaType != null){"Media type is require"} // TODO: null searches both?
            val store = getStore(searchQuery.filter.mediaType!!)
            if(!store.exists) return emptyList()

            return when(searchQuery) {
                is SearchQuery.ImageQuery -> imageSearch(context, searchQuery)
                is SearchQuery.TextQuery -> textSearch(searchQuery)
            }
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
        val threshold = vlmTextSimThreshold
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, threshold, searchQuery.filter.ids.toSet(),  startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, threshold, includeSims = true)
        val vlmSims = queryResult.toSimsMap()
        val vlmClusterSims = clusterResult.toSimsMap()
        val vlmItemClusterSims = mapItemsToClusterScores(vlmSims, vlmClusterSims, getItemToClusterSimMap(searchQuery.filter.mediaType))

        val miniLmQueryEmbed = miniLmTextEmbedder.embed(query).toQInt8Embed()
        val conceptStore = getConceptStore(searchQuery.filter.mediaType)
        val conceptQueryResult = conceptStore.query(miniLmQueryEmbed, Int.MAX_VALUE, conceptSimThreshold, searchQuery.filter.ids.toSet(),  startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val miniLmSims = conceptQueryResult.toSimsMap()
        val signals = getSearchSignals(vlmSims, vlmItemClusterSims, miniLmSims)
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
        val threshold = vlmImageSimThreshold
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, threshold, startDate = searchQuery.filter.startDate, endDate = searchQuery.filter.endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, threshold, includeSims = true)
        val mainSims = queryResult.toSimsMap()
        val clusterSims = clusterResult.toSimsMap()
        val itemClusterSims = mapItemsToClusterScores(mainSims, clusterSims, getItemToClusterSimMap(searchQuery.filter.mediaType))
        val signals = getSearchSignals(mainSims, itemClusterSims)
        val reranked = Reranker.rerank(signals)
        return reranked
    }

    private fun getSearchSignals(mainSims: Map<Long, Float>, itemClusterSims: Map<Long, Float>, conceptSims: Map<Long, Float>? = null): List<Signal>{
        val signals = mutableListOf<Signal>()
        val mainSignal = Signal(scores = mainSims, type = SignalType.VLM)
        val clusterSignal = Signal(scores = itemClusterSims, type = SignalType.VLM_CLUSTER)
        signals.addAll(listOf(mainSignal, clusterSignal))
        conceptSims?.let{ signals.add(Signal(scores = conceptSims, type = SignalType.SENTENCE_TRANSFORMER))}
        return signals
    }

    private fun getStore(mediaType: MediaType) = if(mediaType == MediaType.VIDEO) videoEmbedStore else imageEmbedStore
    private fun getConceptStore(mediaType: MediaType) = if(mediaType == MediaType.VIDEO) videoConceptEmbedStore else imageConceptEmbedStore
    private suspend fun getItemToClusterSimMap(mediaType: MediaType) = clusterCrossRefRepository.getClusterToMediaIdsMap().filterKeys{it.second == mediaType}.map{it.key.first to it.value}.associate { it.first to it.second }
}

private fun mapItemsToClusterScores(itemSimMap: Map<Long, Float>, clusterSims: Map<Long, Float>, clusterToMediaMap:  Map<Long, MutableSet<Long>>): Map<Long, Float>{
    val itemToCluster = buildMap {
        clusterToMediaMap.forEach { (clusterId, items) ->
            items.forEach { itemId -> put(itemId, clusterId) }
        }
    }
    return itemSimMap.keys.mapNotNull { itemId -> itemToCluster[itemId]?.let { clusterSims[it]?.let { sim -> itemId to sim } } }.toMap()
}

fun QueryResult.toSimsMap(): Map<Long, Float> = this.sims?.let(this.ids::zip)?.toMap() ?: emptyMap()
