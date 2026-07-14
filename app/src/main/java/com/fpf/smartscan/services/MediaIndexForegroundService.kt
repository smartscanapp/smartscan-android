package com.fpf.smartscan.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.fpf.smartscan.R
import com.fpf.smartscan.MainActivity
import com.fpf.smartscan.api.llm.LLMProviderConfig
import com.fpf.smartscan.api.llm.OpenaiClient
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.di.IMAGE_EMBED_STORE
import com.fpf.smartscan.di.VIDEO_EMBED_STORE
import com.fpf.smartscan.di.CLUSTER_EMBED_STORE
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.concepts.getAllowedClusters
import com.fpf.smartscan.concepts.getAllowedTags
import com.fpf.smartscan.constants.DEFAULT_SYSTEM_PROMPT
import com.fpf.smartscan.di.CONCEPT_IMAGE_EMBED_STORE
import com.fpf.smartscan.index.CloudImageIndexListener
import com.fpf.smartscan.index.CloudImageIndexer
import com.fpf.smartscan.index.ImageIndexListener
import com.fpf.smartscan.index.IndexJob
import com.fpf.smartscan.index.VideoIndexListener
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.index.indexMedia
import com.fpf.smartscan.index.indexMediaForConcepts
import com.fpf.smartscan.utils.showNotification
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y
import com.fpf.smartscansdk.ml.models.ModelManager
import com.fpf.smartscansdk.ml.models.ModelName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import kotlin.collections.map

class MediaIndexForegroundService : Service(), KoinComponent {
    companion object {
        const val EXTRA_MEDIA_TYPES = "extra_media_types"
        const val EXTRA_INDEX_JOB = "extra_index_job"
        private const val NOTIFICATION_ID = 200
        private const val TAG = "MediaIndexService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private val sharedPrefs by lazy { application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)    }
    private val imageEmbedder by lazy { ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}
    private val textEmbedder by lazy { ModelManager.getTextEmbedder(application, ModelName.ALL_MINILM_L6_V2) }
    private val metadataRepo: MediaMetadataRepository by inject()
    private val clusterMetadataRepository: ClusterMetadataRepository by inject()
    private val clusterCrossRefRepository: ClusterCrossRefRepository by inject()
    private val imageStore: FileEmbeddingStore by inject(IMAGE_EMBED_STORE)
    private val videoStore: FileEmbeddingStore by inject(VIDEO_EMBED_STORE)
    private val clusterStore: FileEmbeddingStore by inject(CLUSTER_EMBED_STORE)
    private val imageConceptEmbedStore: FileEmbeddingStore by inject(CONCEPT_IMAGE_EMBED_STORE)



    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
    }

    private fun startForegroundServiceNotification() {
        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this, getString(R.string.service_media_index_channel_id)
        )
            .setContentTitle(getString(R.string.notif_title_media_index_service))
            .setContentText(getString(R.string.notif_content_media_index_service))
            .setSmallIcon(R.drawable.smartscan_logo)
            .setContentIntent(activityPendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            getString(R.string.service_media_index_channel_id),
            getString(R.string.service_media_index_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaTypes = intent?.getStringArrayListExtra(EXTRA_MEDIA_TYPES)
                ?.map(MediaType::valueOf)
                ?: MediaType.entries
        serviceScope.launch {
            try {
                val indexJob = IndexJob.valueOf(intent?.getStringExtra(EXTRA_INDEX_JOB)?: error("Invalid job type"))
                when(indexJob){
                    IndexJob.MAIN -> runMainIndexPipeline(mediaTypes)
                    IndexJob.CONCEPTS -> runConceptsIndexPipeline(mediaTypes)
                }
            }
            finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    private suspend fun runMainIndexPipeline(mediaTypes: List<MediaType>){
        try {
            val appSettings = loadSettings(sharedPrefs)
            imageEmbedder.initialize()

            val clusterManager = ClusterManager(
                clusterEmbedStore = clusterStore,
                imageEmbedStore = imageStore,
                videoEmbedStore = videoStore,
                clusterCrossRefRepository = clusterCrossRefRepository,
                clusterMetadataRepository = clusterMetadataRepository,
                mediaMetadataRepository = metadataRepo,
            )

            mediaTypes.forEach { mediaType ->
                when (mediaType) {
                    MediaType.IMAGE -> {
                        val imageIndexer = ImageIndexer(
                            imageEmbedder,
                            context = application,
                            listener = ImageIndexListener,
                            store = imageStore,
                            quantize = true
                        )
                        indexMedia(
                            application,
                            MediaType.IMAGE,
                            imageStore,
                            imageIndexer,
                            metadataRepo,
                            appSettings.searchableImageDirectories.map { it.toUri() })
                    }

                    MediaType.VIDEO -> {
                        val videoIndexer = VideoIndexer(
                            imageEmbedder,
                            context = application,
                            listener = VideoIndexListener,
                            store = videoStore,
                            quantize = true,
                            width = IMAGE_SIZE_X,
                            height = IMAGE_SIZE_Y
                        )
                        indexMedia(
                            application,
                            MediaType.VIDEO,
                            videoStore,
                            videoIndexer,
                            metadataRepo,
                            appSettings.searchableVideoDirectories.map { it.toUri() })
                    }
                }
            }

            try {
                clusterManager.cluster()
            } catch (e: Exception) {
                Log.e(TAG, "Clustering failed:", e)
                val title = application.getString(R.string.notif_title_index_error_service, "Media")
                val content = application.getString(R.string.notif_content_cluster_error_service)
                showNotification(application, title, content, NOTIFICATION_ID + 1)
            }
        } catch (e: CancellationException) {
            Log.w(TAG, "Indexing job cancelled:", e)
        } catch (e: Exception) {
            Log.e(TAG, "Indexing failed:", e)
            val title = application.getString(R.string.notif_title_index_error_service, "Media")
            val content = application.getString(R.string.notif_content_index_error_service)
            showNotification(application, title, content, NOTIFICATION_ID + 1)
        } finally {
            imageEmbedder.closeSession()
        }
    }

    private suspend fun runConceptsIndexPipeline(mediaTypes: List<MediaType>){
        try {
            val appSettings = loadSettings(sharedPrefs)
            val allowedTags= getAllowedTags(sharedPrefs)
            val allowedClusters = getAllowedClusters(sharedPrefs)
            val openaiClient = OpenaiClient(
                apiKey = appSettings.openaiApiKey?: error("Missing OpenAI API key"),
                config = LLMProviderConfig(model = "gpt-5.4-mini", systemPrompt = DEFAULT_SYSTEM_PROMPT, maxTokens = 500)
            )
            textEmbedder.initialize()

            mediaTypes.forEach { mediaType ->
                when (mediaType) {
                    MediaType.IMAGE -> {
                        val imageIndexer = CloudImageIndexer(
                            context = application,
                            embedder=textEmbedder,
                            listener = CloudImageIndexListener,
                            store = imageConceptEmbedStore,
                            mediaMetadataRepository = metadataRepo,
                            quantize = true,
                            openaiClient = openaiClient
                        )
                        indexMediaForConcepts(
                            mediaType,
                            imageIndexer,
                            metadataRepo,
                            allowedTags = allowedTags.toList(),
                            allowedClusters = allowedClusters.toList()
                        )
                    }

                    MediaType.VIDEO -> {
                     // TODO
                    }
                }
            }
        } catch (e: CancellationException) {
            Log.w(TAG, "Indexing job cancelled:", e)
        } catch (e: Exception) {
            Log.e(TAG, "Concept Indexing failed:", e)
            val title = application.getString(R.string.notif_title_index_error_service, "Media")
            val content = application.getString(R.string.notif_content_index_error_service)
            showNotification(application, title, content, NOTIFICATION_ID + 1)
        } finally {
            textEmbedder.closeSession()
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
