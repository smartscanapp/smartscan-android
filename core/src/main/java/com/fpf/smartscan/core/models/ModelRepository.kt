package com.fpf.smartscan.core.models

import android.app.Application
import android.util.Log
import com.fpf.smartscansdk.core.SmartScanException
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.ml.models.ModelInfo
import com.fpf.smartscansdk.ml.models.ModelManager
import com.fpf.smartscansdk.ml.models.ModelName
import com.fpf.smartscansdk.ml.models.ModelRegistry
import com.fpf.smartscansdk.ml.models.ModelType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ModelRepository(
    private val application: Application
) {
    companion object {
        const val TAG = "ModelRepository"
    }

    // Own global scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _modelDownloadProgress = MutableStateFlow(0)

    val modelDownloadProgress: StateFlow<Int> = _modelDownloadProgress
    private val _modelDownloadStatus = MutableStateFlow(ModelDownloadStatus.IDLE)

    val modelDownloadStatus: StateFlow<ModelDownloadStatus> = _modelDownloadStatus

    private val _installedModels = MutableStateFlow<List<ModelName>>(getInstalledModels())

    val installedModels: StateFlow<List<ModelName>> = _installedModels

    private var miniLmTextEmbedder: TextEmbeddingProvider? = null

    private var modelLastUsages: MutableMap<ModelName, Long> = mutableMapOf()
    fun downloadModel( modelInfo: ModelInfo){
        scope.launch {
            _modelDownloadStatus.update { ModelDownloadStatus.ACTIVE }
            try {
                ModelManager.downloadModelInternal(application, modelInfo) { progress ->
                    _modelDownloadProgress.update { progress }
                }
                _modelDownloadStatus.update { ModelDownloadStatus.COMPLETE }
                _installedModels.update { it + modelInfo.name }
            } catch (e: SmartScanException.ModelDownloadFailed) {
                Log.e(TAG, "Model download failed: $e")
                _modelDownloadStatus.update { ModelDownloadStatus.FAILED }
            }
        }
    }

    fun reset(){
        _modelDownloadStatus.update { ModelDownloadStatus.IDLE }
        _modelDownloadProgress.update {0}
    }

    fun deleteModel(modelInfo: ModelInfo){
        ModelManager.deleteModel(application, modelInfo)
        _installedModels.update { it - modelInfo.name }
    }

    fun getInstalledModels(type: ModelType?=null): List<ModelName> = ModelManager.listModels(application, type)

    fun getAvailableModelRegistry(): Map<ModelName, ModelInfo> = ModelRegistry.filter { item -> item.key in listOf(ModelName.ALL_MINILM_L6_V2)}

    // 'Singleton' used because this model is required in various parts of the app
    fun getMiniLmTextEmbedder(): TextEmbeddingProvider {
        miniLmTextEmbedder?.let { return it }
        val model = ModelManager.getTextEmbedder(application, ModelName.ALL_MINILM_L6_V2)
        miniLmTextEmbedder = model
        return model
    }

    fun updateModelLastUsage(model: ModelName, lastUsed: Long){
        modelLastUsages[model] = lastUsed
    }

    fun shouldShutdownModel(model: ModelName, maxDuration: Long): Boolean {
        val lastUsage = modelLastUsages[model]
        return lastUsage != null && System.currentTimeMillis() - lastUsage >= maxDuration
    }

}

enum class ModelDownloadStatus {
    IDLE,
    ACTIVE,
    COMPLETE,
    FAILED,
    CANCELLED
}