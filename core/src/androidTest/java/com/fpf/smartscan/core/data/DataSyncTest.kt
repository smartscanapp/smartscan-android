package com.fpf.smartscan.core.data

import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.data.clusters.ClusterCrossRefEntity
import com.fpf.smartscan.core.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.core.data.clusters.ClusterMetadataEntity
import com.fpf.smartscan.core.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.core.data.media.MediaMetadataEntity
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.media.removeStaleMedia
import com.fpf.smartscan.core.utils.randomEmbedding
import com.fpf.smartscansdk.core.cluster.Cluster
import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DataSyncTest {
    private lateinit var stores: TestEmbedStores.Stores
    private lateinit var database: MediaDatabase

    @Before
    fun setup() {
        stores = TestEmbedStores.create()
        database = TestDatabase.create()
    }

    @After
    fun tearDown() {
        stores.directory.deleteRecursively()
        database.close()
    }

    @Test
    fun test_cluster_embed_deletes_when_all_crossrefs_deleted() = runTest {
        val media = createMedia(100)
        val clusters = createClusters(10)

        insertMedia(media)
        insertClusters(clusters)
        insertClusterEmbeddings(clusters)
        insertClusterCrossRefs(media, clusters)
        insertMediaEmbeddings(media)

        val mediaToDelete = media.take(10)
        val clusterManager = createClusterManager()
        val clustersToSync = clusterManager.getClustersMatchingMedia(mediaToDelete.map { it.id }, MediaType.IMAGE).map { it.clusterId }

        removeStaleMedia(
            mediaToDelete.map { it.id },
            MediaType.IMAGE,
            listOf(stores.image),
            MediaMetadataRepository(database.metadataDao())
        )

        assertEquals(media.size - mediaToDelete.size, stores.image.get().size)

        clustersToSync.forEach { clusterManager.sync(it) }

        assertEquals(clusters.size - clustersToSync.size, stores.cluster.get().size)
    }

    private fun createMedia(count: Int): List<MediaMetadataEntity> =
        List(count) { index ->
            MediaMetadataEntity(
                id = index.toLong(),
                dateAdded = System.currentTimeMillis() + index + 1,
                type = MediaType.IMAGE
            )
        }

    private fun createClusters(count: Int): List<Cluster> =
        List(count) { index ->
            Cluster(
                clusterId = index.toLong(),
                embedding = randomEmbedding(true, 512),
                metadata = ClusterMetadata(prototypeSize = 0)
            )
        }

    private suspend fun insertMedia(media: List<MediaMetadataEntity>) {
        database.metadataDao().insert(media)
    }

    private suspend fun insertClusters(clusters: List<Cluster>) {
        database.clusterMetadataDao().insert(
            clusters.map { ClusterMetadataEntity(clusterId = it.clusterId, prototypeSize = it.metadata.prototypeSize) }
        )
    }

    private suspend fun insertClusterEmbeddings(clusters: List<Cluster>) {
        stores.cluster.add(
            clusters.map { StoredEmbedding(it.clusterId, System.currentTimeMillis(), it.embedding) }
        )
    }

    private suspend fun insertClusterCrossRefs(media: List<MediaMetadataEntity>, clusters: List<Cluster>) {
        database.clusterCrossRefDao().upsert(
            media.chunked(media.size / clusters.size)
                .flatMapIndexed { index, chunk ->
                    chunk.map { ClusterCrossRefEntity(mediaId = it.id, mediaType = it.type, clusterId = clusters[index].clusterId) }
                }
        )
    }

    private suspend fun insertMediaEmbeddings(media: List<MediaMetadataEntity>) {
        stores.image.add(
            media.map { StoredEmbedding(it.id, it.dateAdded, randomEmbedding(quantize = true, 512)) }
        )
    }

    private fun createClusterManager() = ClusterManager(
        imageEmbedStore = stores.image,
        videoEmbedStore = stores.video,
        clusterEmbedStore = stores.cluster,
        clusterMetadataRepository = ClusterMetadataRepository(database.clusterMetadataDao()),
        clusterCrossRefRepository = ClusterCrossRefRepository(database.clusterCrossRefDao())
    )
}