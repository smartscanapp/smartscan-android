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
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.core.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.core.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscan.di.IMAGE_EMBED_STORE
import com.fpf.smartscan.di.VIDEO_EMBED_STORE
import com.fpf.smartscan.di.CLUSTER_EMBED_STORE
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.di.CONCEPT_IMAGE_EMBED_STORE
import com.fpf.smartscan.core.index.CloudIndexJobManager
import com.fpf.smartscan.core.index.IndexJobType
import com.fpf.smartscan.core.index.LocalIndexJobManager
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.utils.showNotification
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
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

class MediaIndexForegroundService : Service(), KoinComponent {
    companion object {
        const val EXTRA_MEDIA_TYPES = "extra_media_types"
        const val EXTRA_INDEX_JOB = "extra_index_job"
        private const val NOTIFICATION_ID = 300
        private const val TAG = "MediaIndexService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private val imageEmbedder by lazy { ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}
    private val textEmbedder by lazy { ModelManager.getTextEmbedder(application, ModelName.ALL_MINILM_L6_V2) }
    private val mediaMetadataRepository: MediaMetadataRepository by inject()
    private val clusterMetadataRepository: ClusterMetadataRepository by inject()
    private val clusterCrossRefRepository: ClusterCrossRefRepository by inject()
    private val imageEmbedStore: FileEmbeddingStore by inject(IMAGE_EMBED_STORE)
    private val videoEmbedStore: FileEmbeddingStore by inject(VIDEO_EMBED_STORE)
    private val clusterEmbedStore: FileEmbeddingStore by inject(CLUSTER_EMBED_STORE)
    private val imageConceptsEmbedStore: FileEmbeddingStore by inject(CONCEPT_IMAGE_EMBED_STORE)

    private val sharedPrefs by lazy { application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)}

    private val cloudIndexJobManager by lazy {
        CloudIndexJobManager(
            application = application,
            sharedPrefs=sharedPrefs,
            textEmbedder = textEmbedder,
            imageConceptsEmbedStore = imageConceptsEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
        )
    }

    private val localIndexJobManager by lazy {
        LocalIndexJobManager(
            application = application,
            imageEmbedder = imageEmbedder,
            imageEmbedStore = imageEmbedStore,
            videoEmbedStore = videoEmbedStore,
            clusterEmbedStore = clusterEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
            clusterMetadataRepository = clusterMetadataRepository,
            clusterCrossRefRepository = clusterCrossRefRepository
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
                        try {
                            val appSettings = loadSettings(sharedPrefs)
                            cloudIndexJobManager.run(mediaTypes, appSettings.openaiApiKey)
                        } catch (e: AppException.MissingApiKey) {
                            Log.e(TAG, e.message, e)
                            val title = application.getString(R.string.notif_title_index_error_service, "Media")
                            val content = application.getString(R.string.notif_content_missing_api_key_error_service)
                            showNotification(application, title, content, NOTIFICATION_ID + 1)
                        }

                        catch (e: CancellationException) {
                            Log.w(TAG, "Indexing job cancelled:", e)
                        }
                        catch (e: Exception) {
                            Log.e(TAG, "Cloud Indexing failed:", e)
                            val title = application.getString(R.string.notif_title_index_error_service, "Media")
                            val content = application.getString(R.string.notif_content_index_error_service)
                            showNotification(application, title, content, NOTIFICATION_ID + 1)
                        }
                    }
                    IndexJobType.LOCAL -> {
                        try {
                            val appSettings = loadSettings(sharedPrefs)
                            val allowedImageDirs = appSettings.searchableImageDirectories.map{it.toUri()}
                            val allowedVideoDirs = appSettings.searchableVideoDirectories.map{it.toUri()}
                            localIndexJobManager.run(mediaTypes, allowedImageDirs=allowedImageDirs, allowedVideoDirs=allowedVideoDirs)
                        }catch (e: AppException.ClusterException)  {
                            Log.e(TAG, e.message, e)
                            val title = application.getString(R.string.notif_title_index_error_service, "Media")
                            val content = application.getString(R.string.notif_content_cluster_error_service)
                            showNotification(application, title, content, NOTIFICATION_ID + 1)
                        }
                        catch (e: CancellationException) {
                            Log.w(TAG, "Indexing job cancelled:", e)
                        }
                        catch (e: Exception) {
                            Log.e(TAG, "Indexing failed:", e)
                            val title = application.getString(R.string.notif_title_index_error_service, "Media")
                            val content = application.getString(R.string.notif_content_index_error_service)
                            showNotification(application, title, content, NOTIFICATION_ID + 1)
                        }
                    }
                }
            }
            finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
