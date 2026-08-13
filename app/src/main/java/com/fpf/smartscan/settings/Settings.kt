package com.fpf.smartscan.settings

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.fpf.smartscan.constants.PrefsKeys
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun loadSettings(sharedPrefs: SharedPreferences): AppSettings {
    val jsonSettings = sharedPrefs.getString(PrefsKeys.SETTINGS, null)
    return if (jsonSettings != null) {
        try {
            json.decodeFromString<AppSettings>(jsonSettings)
        } catch (e: Exception) {
            Log.e("loadSettings", "Failed to decode settings", e)
            AppSettings()
        }
    } else {
        AppSettings()
    }
}

fun saveSettings(sharedPrefs: SharedPreferences, settings: AppSettings) {
    sharedPrefs.edit {putString(PrefsKeys.SETTINGS, json.encodeToString(settings))  }
}