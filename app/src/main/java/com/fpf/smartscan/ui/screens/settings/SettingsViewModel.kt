package com.fpf.smartscan.ui.screens.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.ModelRepository
import com.fpf.smartscan.events.BackupEvent
import com.fpf.smartscan.events.BackupEventType
import com.fpf.smartscan.events.ModelEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.fpf.smartscan.settings.AppSettings
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.settings.saveSettings
import com.fpf.smartscan.ui.theme.ColorSchemeType
import com.fpf.smartscan.ui.theme.ThemeManager
import com.fpf.smartscan.ui.theme.ThemeMode
import com.fpf.smartscan.utils.BackupUtils
import com.fpf.smartscansdk.ml.models.ModelInfo
import com.fpf.smartscansdk.ml.models.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application, private val modelRepository: ModelRepository) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences(PrefsNames.APP_PREFS, Context.MODE_PRIVATE)
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings
    private val _modelEvent = MutableSharedFlow<ModelEvent>()
    val modelEvent = _modelEvent.asSharedFlow()

    private val _backupEvent = MutableSharedFlow<BackupEvent>()
    val backupEvent = _backupEvent.asSharedFlow()

    private val _isBackupLoading = MutableStateFlow(false)
    val isBackupLoading: StateFlow<Boolean> = _isBackupLoading

    private val _isRestoreLoading = MutableStateFlow(false)
    val isRestoreLoading: StateFlow<Boolean> = _isRestoreLoading

    val installedModels = modelRepository.installedModels


    companion object {
        private const val TAG = "SettingsViewModel"
    }

    init {
        _appSettings.value = loadSettings(sharedPrefs)
    }

    fun updateTextQueryStrictness(strictness: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(textQueryStrictness = strictness)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun updateImageQueryStrictness(strictness: Float) {
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(imageQueryStrictness = strictness)
        saveSettings(sharedPrefs, _appSettings.value)
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
        _isBackupLoading.value = true
        viewModelScope.launch(Dispatchers.IO){
            try {
                BackupUtils.backup(getApplication(), uri)
                _backupEvent.emit(BackupEvent(BackupEventType.BACKUP, success = true, "Backup successful"))
            }catch (e: Exception){
                Log.e(TAG, "Error backing up: ${e.message}")
                val appEventMessage = if(e.message == "Missing index file(s)")  "Missing index file(s)" else "Backup failed"
                _backupEvent.emit(BackupEvent(BackupEventType.BACKUP, success = false, appEventMessage))
            }finally {
                _isBackupLoading.emit(false)
            }
        }
    }

    fun restore(uri: Uri){
        _isRestoreLoading.value = true
        MediaDatabase.close()
        viewModelScope.launch(Dispatchers.IO){
            try {
                BackupUtils.restore(getApplication(), uri)
                _backupEvent.emit(BackupEvent(BackupEventType.RESTORE, success = true, "Restore successful"))
            }catch (e: Exception){
                Log.e(TAG, "Error restoring: ${e.message}")
                _backupEvent.emit(BackupEvent(BackupEventType.RESTORE, success = false, "Invalid backup file"))
            }finally {
                _isRestoreLoading.emit(false)
            }
        }
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

    fun updateOpenaiApiKey(apiKey: String){
        val currentSettings = _appSettings.value
        _appSettings.value = currentSettings.copy(openaiApiKey = apiKey)
        saveSettings(sharedPrefs, _appSettings.value)
    }

    fun downloadModel(modelInfo: ModelInfo) = modelRepository.downloadModel(modelInfo)

    fun deleteModel(modelInfo: ModelInfo) = modelRepository.deleteModel(modelInfo)
}
