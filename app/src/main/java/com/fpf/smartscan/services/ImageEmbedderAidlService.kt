package com.fpf.smartscan.services


import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import com.fpf.smartscan.IImageEmbedderService
import com.fpf.smartscan.R
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.flattenEmbeddings
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.models.ModelManager
import com.fpf.smartscansdk.ml.models.ModelType
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder
import kotlinx.coroutines.runBlocking
import com.fpf.smartscansdk.core.embeddings.embedBatch
import com.fpf.smartscansdk.core.processors.Concurrency
import com.fpf.smartscansdk.core.processors.ConcurrencyController


class ImageEmbedderAidlService: Service() {

    companion object {
        private const val TAG = "ImageEmbedderAidlService"
        private const val BATCH_SIZE = 10
    }
    private lateinit var imageEmbedder: ImageEmbeddingProvider

    private val concurrencyController by lazy { ConcurrencyController(application)}

    override fun onCreate() {
        super.onCreate()
        imageEmbedder = ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        imageEmbedder.closeSession()
    }

    private val binder = object : IImageEmbedderService.Stub() {

        override fun getEmbeddingDim(): Int {
            return imageEmbedder.embeddingDim
        }

        override fun closeSession() {
            imageEmbedder.closeSession()
        }

        override fun embed(data: ByteArray): FloatArray? {
            return runBlocking {
                try {
                    if (!imageEmbedder.isInitialized()) imageEmbedder.initialize()
                    val embedding = imageEmbedder.embed(byteArrayToBitmap(data))
                    embedding
                } catch (e: Exception) {
                    Log.d(TAG, "EMBEDDING_ERROR: ${e.message}")
                    null
                }
            }
        }

        override fun embedBatch(data: ByteArray): FloatArray? {
            return runBlocking {
                try {
                    if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()
                    val decoded = decodeByteArrayPayload(data, delimiter)
                    val bitmaps = decoded.map { byteArrayToBitmap(it) }
                    val concurrency = Concurrency.Dynamic{concurrencyController.calculateConcurrency()}
                    val embeddings = embedBatch( imageEmbedder, bitmaps, concurrency=concurrency, batchSize = BATCH_SIZE)
                    flattenEmbeddings(embeddings, imageEmbedder.embeddingDim)
                }catch(e: Exception){
                    Log.d(TAG, "EMBEDDING_ERROR: ${e.message}")
                    null
                }
            }
        }

        override fun listModels(): List<String> {
            return ModelManager.listModels(application, ModelType.IMAGE_ENCODER).map { it.name }
        }

        override fun selectModel(model: String): Boolean {
           // Current no other image models
            return true
        }

        override fun getDelimiter(): ByteArray {
            return byteArrayOf(0xFF.toByte(), 0x00, 0xFF.toByte(), 0x00)
        }
    }

    private fun byteArrayToBitmap(bytes: ByteArray): Bitmap{
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun decodeByteArrayPayload(bytes: ByteArray, delimiter: ByteArray): List<ByteArray> {
        val recoveredPayload = mutableListOf<ByteArray>()
        var pos = 0
        var i = 0

        while (i <= bytes.size - delimiter.size) {
            var match = true
            for (j in delimiter.indices) {
                if (bytes[i + j] != delimiter[j]) {
                    match = false
                    break
                }
            }

            if (match) {
                recoveredPayload.add(bytes.sliceArray(pos until i))
                i +=  delimiter.size
                pos = i
            } else {
                i++
            }
        }

        if (pos < bytes.size) {
            recoveredPayload.add(bytes.sliceArray(pos until bytes.size))
        }

        return recoveredPayload
    }
}