package com.fpf.smartscan.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.fpf.smartscan.R
import com.fpf.smartscan.MainActivity
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscan.di.IMAGE_EMBED_STORE
import com.fpf.smartscan.di.VIDEO_EMBED_STORE
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.di.CONCEPT_IMAGE_EMBED_STORE
import com.fpf.smartscan.cloud.index.CloudIndexJobManager
import com.fpf.smartscan.constants.EncryptedStorageKeys
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.index.IndexJobType
import com.fpf.smartscan.core.index.LocalIndexJobManager
import com.fpf.smartscan.core.media.MediaJobManager
import com.fpf.smartscan.core.storage.EncryptedStorage
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.utils.getTimeInMinutesAndSeconds
import com.fpf.smartscan.utils.showNotification
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.processors.ProcessorResult
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder
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

class IndexService : Service(), KoinComponent {
    companion object {
        const val EXTRA_MEDIA_TYPES = "extra_media_types"
        const val EXTRA_INDEX_JOB = "extra_index_job"
        private const val NOTIFICATION_ID = 300
        private const val TAG = "IndexService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private val imageEmbedder by lazy { ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}
    private val textEmbedder by lazy { ModelManager.getTextEmbedder(application, ModelName.ALL_MINILM_L6_V2) }
    private val mediaMetadataRepository: MediaMetadataRepository by inject()
    private val clusterManager: ClusterManager by inject()
    private val mediaJobManager: MediaJobManager by inject()
    private val imageEmbedStore: FileEmbeddingStore by inject(IMAGE_EMBED_STORE)
    private val videoEmbedStore: FileEmbeddingStore by inject(VIDEO_EMBED_STORE)
    private val imageConceptsEmbedStore: FileEmbeddingStore by inject(CONCEPT_IMAGE_EMBED_STORE)
    private val sharedPrefs: SharedPreferences by inject()
    private val encryptedStorage: EncryptedStorage by inject()

    private val cloudIndexJobManager by lazy {
        CloudIndexJobManager(
            application = application,
            sharedPrefs=sharedPrefs,
            textEmbedder = textEmbedder,
            imageConceptsEmbedStore = imageConceptsEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaJobManager=mediaJobManager,
        )
    }

    private val localIndexJobManager by lazy {
        LocalIndexJobManager(
            application = application,
            imageEmbedder = imageEmbedder,
            imageEmbedStore = imageEmbedStore,
            videoEmbedStore = videoEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
            clusterManager=clusterManager
        )
    }

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

        val notification = NotificationCompat.Builder(this, getString(R.string.service_media_index_channel_id))
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
        serviceScope.launch {
            try {
                val mediaTypes = intent?.getStringArrayListExtra(EXTRA_MEDIA_TYPES)?.map(MediaType::valueOf) ?: MediaType.entries
                val indexJob = IndexJobType.valueOf(intent?.getStringExtra(EXTRA_INDEX_JOB)?: error("Invalid job type"))
                when(indexJob){
                    IndexJobType.CLOUD -> {
                        val openaiApiKey = encryptedStorage.getString(EncryptedStorageKeys.OPENAI_API_KEY)
                        cloudIndexJobManager.run(mediaTypes, openaiApiKey){ processorResult, mediaType ->
                            handleCloudIndexResult(processorResult, mediaType)
                        }
                    }
                    IndexJobType.LOCAL -> {
                        val appSettings = loadSettings(sharedPrefs)
                        val allowedImageDirs = appSettings.searchableImageDirectories.map{it.toUri()}
                        val allowedVideoDirs = appSettings.searchableVideoDirectories.map{it.toUri()}
                        localIndexJobManager.run(mediaTypes, allowedImageDirs=allowedImageDirs, allowedVideoDirs=allowedVideoDirs){ processorResult, mediaType ->
                            handleLocalIndexResult(processorResult, mediaType)
                        }
                    }
                }
            }
            catch (e: Exception) {
                handleServiceError(e)
            }
            finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleLocalIndexResult(processorResult: ProcessorResult, mediaType: MediaType){
        when(processorResult){
            is ProcessorResult.Success -> {
                val (minutes, seconds) = getTimeInMinutesAndSeconds(processorResult.timeElapsed)
                val indexCompleteTitle = applicationContext.getString(R.string.notif_title_index_complete)
                val notificationText = "Total ${mediaType.name.lowercase()}s indexed: ${processorResult.totalProcessed}, Time: ${minutes}m ${seconds}s"
                showNotification(application, indexCompleteTitle, notificationText, NOTIFICATION_ID + 1)
            }
            is ProcessorResult.Failure -> {
                val title = applicationContext.getString(R.string.notif_title_index_error_service, mediaType.name.lowercase().replaceFirstChar { it.uppercase() })
                val content = applicationContext.getString(R.string.notif_content_index_error_service)
                showNotification(application, title, content, NOTIFICATION_ID + 1)
            }
        }
    }

    private fun handleCloudIndexResult(processorResult: ProcessorResult, mediaType: MediaType){
        when(processorResult){
            is ProcessorResult.Success -> {
                val (minutes, seconds) = getTimeInMinutesAndSeconds(processorResult.timeElapsed)
                val indexCompleteTitle = applicationContext.getString(R.string.notif_title_index_complete)
                val notificationText = "Total ${mediaType.name.lowercase()}s indexed: ${processorResult.totalProcessed}, Time: ${minutes}m ${seconds}s"
                showNotification(application, indexCompleteTitle, notificationText, NOTIFICATION_ID + 1)
            }
            is ProcessorResult.Failure -> {
                val title = applicationContext.getString(R.string.notif_title_index_error_service, mediaType.name.lowercase().replaceFirstChar { it.uppercase() })
                val content = when (processorResult.error) {
                    is AppException.InvalidApiKey -> application.getString(R.string.notif_content_index_error_invalid_api_key)
                    is AppException.RateLimit -> application.getString(R.string.notif_content_index_error_rate_limit)
                    else -> application.getString(R.string.notif_content_index_error_service)
                }
                showNotification(application, title, content, NOTIFICATION_ID + 1)
            }
        }
    }

    private fun handleServiceError(e: Exception){
        Log.e(TAG, "Indexing service error", e)

        when(e) {
            is AppException. ClusterException ->  {
                val title = application.getString(R.string.notif_title_index_error_service, "Media")
                val content = application.getString(R.string.notif_content_cluster_error_service)
                showNotification(application, title, content, NOTIFICATION_ID + 1)
            }
            is CancellationException -> {
                val cancelledTitle = applicationContext.getString(R.string.notif_content_index_scan_cancelled_title)
                showNotification(applicationContext, title=cancelledTitle, id =NOTIFICATION_ID + 1)
            }

            is AppException.MissingApiKey -> {
                val title = application.getString(R.string.notif_title_index_error_service, "Media")
                val content = application.getString(R.string.notif_content_missing_api_key_error_service)
                showNotification(application, title, content, NOTIFICATION_ID + 1)
            }
            else -> {
                val title = application.getString(R.string.notif_title_index_error_service, "Media")
                val content = application.getString(R.string.notif_content_index_error_service)
                showNotification(application, title, content, NOTIFICATION_ID + 1)
            }
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
