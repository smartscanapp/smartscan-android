package com.fpf.smartscan.workers

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.net.toUri
import androidx.work.*
import com.fpf.smartscan.R
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.di.IMAGE_EMBED_STORE
import com.fpf.smartscan.di.VIDEO_EMBED_STORE
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.fpf.smartscan.di.CONCEPT_IMAGE_EMBED_STORE
import com.fpf.smartscan.cloud.index.CloudIndexJobManager
import com.fpf.smartscan.constants.EncryptedStorageKeys
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.concepts.getAllowedClusters
import com.fpf.smartscan.core.concepts.getAllowedTags
import com.fpf.smartscan.core.index.LocalIndexJobManager
import com.fpf.smartscan.core.media.MediaJobManager
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.models.ModelRepository
import com.fpf.smartscan.core.storage.EncryptedStorage
import com.fpf.smartscan.services.IndexService
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
    private val imageEmbedder by lazy { ClipImageEmbedder(ModelAssetSource.Resource(applicationContext.resources, R.raw.clip_image_encoder_quant))}
    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }
    private val mediaMetadataRepository: MediaMetadataRepository by inject()
    private val clusterManager: ClusterManager by inject()
    private val imageEmbedStore: FileEmbeddingStore by inject(IMAGE_EMBED_STORE)
    private val videoEmbedStore: FileEmbeddingStore by inject(VIDEO_EMBED_STORE)
    private val imageConceptsEmbedStore: FileEmbeddingStore by inject(CONCEPT_IMAGE_EMBED_STORE)
    private val mediaJobManager: MediaJobManager by inject()
    private val sharedPrefs: SharedPreferences by inject()

    private val encryptedStorage: EncryptedStorage by inject()


    // Disable listener for background jobs
    // Note: May later use worker specific listener
    private val cloudIndexJobManager by lazy {
        CloudIndexJobManager(
            application = applicationContext as Application,
            textEmbedder = textEmbedder,
            imageConceptsEmbedStore = imageConceptsEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaJobManager=mediaJobManager,
            useListener = false
        )
    }
    private val localIndexJobManager by lazy {
        LocalIndexJobManager(
            application = applicationContext as Application,
            imageEmbedder = imageEmbedder,
            imageEmbedStore = imageEmbedStore,
            videoEmbedStore = videoEmbedStore,
            mediaMetadataRepository = mediaMetadataRepository,
            clusterManager=clusterManager,
            useListener = false
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val serviceRunning = isServiceRunning(applicationContext, IndexService::class.java)
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
            val results = localIndexJobManager.run(mediaTypes, allowedImageDirs=allowedImageDirs, allowedVideoDirs=allowedVideoDirs)
            val imageResult = results[MediaType.IMAGE]
            imageResult?.let{
                if(it.totalProcessed > 0) mediaJobManager.findAndMarkDuplicates(MediaType.IMAGE)
            }

            val allowedTags= getAllowedTags(sharedPrefs, PrefsKeys.ALLOWED_TAG_COLLECTIONS)
            val allowedClusters = getAllowedClusters(sharedPrefs, PrefsKeys.ALLOWED_AUTO_COLLECTIONS)
            val modelExist = ModelManager.modelExists(applicationContext, ModelName.ALL_MINILM_L6_V2)
            val openaiApiKey = encryptedStorage.getString(EncryptedStorageKeys.OPENAI_API_KEY)
            if(modelExist && !openaiApiKey.isNullOrBlank() && imageConceptsEmbedStore.exists){
                val results = cloudIndexJobManager.run(listOf(MediaType.IMAGE), openaiApiKey, allowedTags = allowedTags.toList(), allowedClusters = allowedClusters.toList()) // only image is supported ATM
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background indexing errors: ${e.message}", e)
            return@withContext Result.failure()
        }
    }
}
