package com.fpf.smartscan.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.events.BackupEvent
import com.fpf.smartscan.events.BackupEventType
import com.fpf.smartscan.events.ModelEvent
import com.fpf.smartscan.events.ModelEventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.utils.copyFromUri
import com.fpf.smartscan.utils.copyToUri
import com.fpf.smartscan.utils.hashFile
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.settings.saveSettings
import com.fpf.smartscan.utils.unzipFiles
import com.fpf.smartscan.utils.zipFiles
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscan.ui.theme.ThemeMode
import com.fpf.smartscansdk.core.SmartScanException
import com.fpf.smartscansdk.ml.models.ModelInfo
import com.fpf.smartscansdk.ml.models.ModelManager
import com.fpf.smartscansdk.ml.models.ModelName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences(PrefsNames.APP_PREFS, Context.MODE_PRIVATE)
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings

    private val _importedModels = MutableStateFlow(ModelManager.listModels(application))
    val importedModels: StateFlow<List<ModelName>> = _importedModels
    private val _modelEvent = MutableSharedFlow<ModelEvent>()
    val modelEvent = _modelEvent.asSharedFlow()

    private val _backupEvent = MutableSharedFlow<BackupEvent>()
    val backupEvent = _backupEvent.asSharedFlow()

    private val _isBackupLoading = MutableStateFlow(false)
    val isBackupLoading: StateFlow<Boolean> = _isBackupLoading

    private val _isRestoreLoading = MutableStateFlow(false)
    val isRestoreLoading: StateFlow<Boolean> = _isRestoreLoading



    companion object {
        private const val TAG = "SettingsViewModel"
        const val BACKUP_FILENAME = "smartscan_backup.zip"
        private const val HASH_FILENAME = "hash.txt"
    }

    init {
        _appSettings.value = loadSettings(sharedPrefs)
    }

    fun updateSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(similarityThreshold = threshold)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateImageSimilarityThreshold(threshold: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(imageSimilarityThreshold = threshold)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun onImportModel( uri: Uri, modelInfo: ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ModelManager.importModel(getApplication(), modelInfo, uri)
                _importedModels.value = ModelManager.listModels(getApplication())
                _modelEvent.emit(ModelEvent(ModelEventType.IMPORT, success = true, "Model imported successfully"))
            }catch (e: SmartScanException.InvalidModelFile){
                Log.e(TAG, "${e.message}")
                _modelEvent.emit(ModelEvent(ModelEventType.IMPORT, success = false, e.message?: "Model import failed"))
            }
            catch (e: Exception) {
                Log.e(TAG, "${e.message}")
                _modelEvent.emit(ModelEvent(ModelEventType.IMPORT, success = false,  "Model import failed"))
            }
        }
    }

    fun onDeleteModel(modelInfo: ModelInfo){
        if(ModelManager.deleteModel(getApplication(), modelInfo)) {
            _importedModels.value = ModelManager.listModels(getApplication())
        }
    }

    fun addSearchableImageDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableImageDirectories + dir
        _appSettings.value = currentSettings.copy(searchableImageDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun deleteSearchableImageDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableImageDirectories - dir
        _appSettings.value = currentSettings.copy(searchableImageDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }
    fun addSearchableVideoDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableVideoDirectories + dir
        _appSettings.value = currentSettings.copy(searchableVideoDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun deleteSearchableVideoDirectory(dir: String) {
        val currentSettings = _appSettings.value
        val newDirs = currentSettings.searchableVideoDirectories - dir
        _appSettings.value = currentSettings.copy(searchableVideoDirectories = newDirs)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateTheme(theme: ThemeMode){
        ThemeManager.updateThemeMode(theme)
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(theme = theme)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateColorScheme(colorScheme: ColorSchemeType){
        ThemeManager.updateColorScheme(colorScheme)
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(color = colorScheme)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun backup(uri: Uri){
        val indexZipFile = File(getApplication<Application>().cacheDir, BACKUP_FILENAME)
        val imageEmbeddingStoreFile = File(getApplication<Application>().filesDir, EmbeddingStoresFiles.IMAGE)
        val videoEmbeddingStoreFile = File(getApplication<Application>().filesDir,  EmbeddingStoresFiles.VIDEO)
        val imageClusterEmbeddingStoreFile = File(getApplication<Application>().filesDir, EmbeddingStoresFiles.IMAGE_CLUSTER)
        val videoClusterEmbeddingStoreFile = File(getApplication<Application>().filesDir,  EmbeddingStoresFiles.VIDEO_CLUSTER)
        val hashFile = File(getApplication<Application>().cacheDir, HASH_FILENAME)
        val dbPath = getApplication<Application>().getDatabasePath(MediaDatabase.DB_NAME)

        val embedStoreFiles = listOf(imageEmbeddingStoreFile, videoEmbeddingStoreFile, imageClusterEmbeddingStoreFile, videoClusterEmbeddingStoreFile)
        val filesToZip = listOf( hashFile, dbPath) + embedStoreFiles
        _isBackupLoading.value = true

        viewModelScope.launch(Dispatchers.IO){
            try {
                if(embedStoreFiles.any{!it.exists()}) error("Missing index file(s)")
                val hashes: List<String> = filesToZip.filter { it.exists() && it != hashFile }.map{hashFile(it)}
                hashFile.writeText(hashes.joinToString("\n") )

                zipFiles(indexZipFile, filesToZip)
                copyToUri(getApplication(), uri, indexZipFile)
                _backupEvent.emit(BackupEvent(BackupEventType.BACKUP, success = true, "Backup successful"))
            }catch (e: Exception){
                Log.e(TAG, "Error backing up: ${e.message}")
                val appEventMessage = if(e.message == "Missing index file(s)")  "Missing index file(s)" else "Backup failed"
                _backupEvent.emit(BackupEvent(BackupEventType.BACKUP, success = false, appEventMessage))
            }finally {
                indexZipFile.delete()
                hashFile.delete()
                _isBackupLoading.emit(false)
            }
        }
    }

    fun restore(uri: Uri){
        val indexZipFile = File(getApplication<Application>().cacheDir, BACKUP_FILENAME)
        _isRestoreLoading.value = true

        MediaDatabase.close()

        viewModelScope.launch(Dispatchers.IO){
            try {

                copyFromUri(getApplication(), uri, indexZipFile)
                val extractedFiles = unzipFiles(indexZipFile, getApplication<Application>().filesDir)
//                val expectedFileNames = setOf(EmbeddingStoresFiles.IMAGE,EmbeddingStoresFiles.VIDEO, EmbeddingStoresFiles.IMAGE_CLUSTER, EmbeddingStoresFiles.VIDEO_CLUSTER, MediaDatabase.DB_NAME )

                if(!isValidBackupFile(extractedFiles)){
                    extractedFiles.forEach { it.delete() }
                    error("Invalid backup file")
                }
                _backupEvent.emit(BackupEvent(BackupEventType.RESTORE, success = true, "Restore successful"))
            }catch (e: Exception){
                Log.e(TAG, "Error restoring: ${e.message}")
                _backupEvent.emit(BackupEvent(BackupEventType.RESTORE, success = false, "Invalid backup file"))
            }finally {
                indexZipFile.delete()
                _isRestoreLoading.emit(false)
            }
        }
    }

    private suspend fun isValidBackupFile(extractedFiles: List<File>): Boolean{
        val hashFile = extractedFiles.find { it.name == HASH_FILENAME }?: return false
        val hashesFromFile: List<String> = hashFile.readLines()
        if(hashesFromFile.isEmpty()) return false

        val otherFiles = extractedFiles.filterNot{it.name == HASH_FILENAME}
        val otherFileHashes = otherFiles.map{hashFile(it)}
        return hashesFromFile.toSet() == otherFileHashes.toSet()
    }

    fun updateEnableDirectionGalleryOpen(enable: Boolean){
        _appSettings.update{currentSettings -> currentSettings.copy(enableDirectGalleryOpen = enable)}
        saveSettings(sharedPrefs, _appSettings.value)
    }
    fun updateResultsPerRow(n: Int){
        _appSettings.update{currentSettings -> currentSettings.copy(resultsPerRow = n)}
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateEnableDedupe(enable: Boolean){
        _appSettings.update{currentSettings -> currentSettings.copy(enableDedupe = enable)}
        saveSettings(sharedPrefs, _appSettings.value)
    }
}
