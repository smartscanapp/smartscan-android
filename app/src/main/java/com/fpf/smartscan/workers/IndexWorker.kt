package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.core.net.toUri
import androidx.work.*
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.core.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.core.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.di.CLUSTER_EMBED_STORE
import com.fpf.smartscan.di.IMAGE_EMBED_STORE
import com.fpf.smartscan.di.VIDEO_EMBED_STORE
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.fpf.smartscan.di.CONCEPT_IMAGE_EMBED_STORE
import com.fpf.smartscan.core.index.CloudIndexJobManager
import com.fpf.smartscan.core.index.LocalIndexJobManager
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.models.ModelRepository
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.utils.isServiceRunning
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.models.ModelManager
import com.fpf.smartscansdk.ml.models.ModelName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class IndexWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams), KoinComponent {

    companion object {
        const val TAG = "IndexWorker"

        fun scheduleWorker(context: Context, frequency: Pair<Long, TimeUnit>, delay: Pair<Long, TimeUnit>? = null) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequestBuilder = PeriodicWorkRequestBuilder<IndexWorker>(frequency.first, frequency.second)
                .setConstraints(constraints)

            if (delay != null) {
                workRequestBuilder.setInitialDelay(delay.first, delay.second)
            }

            val workRequest = workRequestBuilder.build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    private val modelRepository: ModelRepository by inject()
    private val imageEmbedder by lazy { ClipImageEmbedder(applicationContext, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))}

    // Reuses singleton minilm model
    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }
    private val mediaMetadataRepository: MediaMetadataRepository by inject()
    private val clusterMetadataRepository: ClusterMetadataRepository by inject()
    private val clusterCrossRefRepository: ClusterCrossRefRepository by inject()
    private val imageEmbedStore: FileEmbeddingStore by inject(IMAGE_EMBED_STORE)
    private val videoEmbedStore: FileEmbeddingStore by inject(VIDEO_EMBED_STORE)
    private val clusterEmbedStore: FileEmbeddingStore by inject(CLUSTER_EMBED_STORE)
    private val imageConceptsEmbedStore: FileEmbeddingStore by inject(CONCEPT_IMAGE_EMBED_STORE)

    private val sharedPrefs by lazy { applicationContext.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)}

    // Disable listener for background jobs
    // Note: May later use worker specific listener
    private val cloudIndexJobManager by lazy {
        CloudIndexJobManager(
            application = applicationContext as Application,
            sharedPrefs=sharedPrefs,
            textEmbedder = textEmbedder,
            imageConceptsEmbedStore = imageConceptsEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
            useListener = false
        )
    }

    private val localIndexJobManager by lazy {
        LocalIndexJobManager(
            application = applicationContext as Application,
            imageEmbedder = imageEmbedder,
            imageEmbedStore = imageEmbedStore,
            videoEmbedStore = videoEmbedStore,
            clusterEmbedStore = clusterEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
            clusterMetadataRepository = clusterMetadataRepository,
            clusterCrossRefRepository = clusterCrossRefRepository,
            useListener = false
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val serviceRunning = isServiceRunning(applicationContext, MediaIndexForegroundService::class.java)
            if(serviceRunning) {
                return@withContext Result.success()
            }

            val mediaTypes = mutableListOf<MediaType>()
            // Prevents doing full indexes by checking if embedding stores already exist. That responsibility should be left to the foreground service
            // No listener used (may change to avoid silent errors)
            if(imageEmbedStore.exists){
                mediaTypes.add((MediaType.IMAGE))
            }
            if(videoEmbedStore.exists){
                mediaTypes.add((MediaType.VIDEO))
            }
            val appSettings = loadSettings(sharedPrefs)

            val allowedImageDirs = appSettings.searchableImageDirectories.map{it.toUri()}
            val allowedVideoDirs = appSettings.searchableVideoDirectories.map{it.toUri()}
            localIndexJobManager.run(mediaTypes, allowedImageDirs=allowedImageDirs, allowedVideoDirs=allowedVideoDirs)

            //TODO: replace openai api with SmartScan API key
            val modelExist = ModelManager.modelExists(applicationContext, ModelName.ALL_MINILM_L6_V2)
            if(modelExist && !appSettings.openaiApiKey.isNullOrBlank() && imageConceptsEmbedStore.exists){
                cloudIndexJobManager.run(listOf(MediaType.IMAGE), appSettings.openaiApiKey) // only image is supported ATM
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background indexing errors: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}
