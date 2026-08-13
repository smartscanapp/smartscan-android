package com.fpf.smartscan.core.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.fpf.smartscan.core.utils.CryptoUtils


class EncryptedStorage(
    private val sharedPrefs: SharedPreferences,
    private val cryptoUtils: CryptoUtils
) {
    fun putString(key: String, value: String) {
        val encrypted = cryptoUtils.encryptToBase64(value)
        sharedPrefs.edit { putString(key, encrypted) }
    }

    fun getString(key: String): String? {
        val encoded = sharedPrefs.getString(key, null)
        return encoded?.let{cryptoUtils.decryptFromBase64(encoded)}
    }

    fun remove(key: String) {
        sharedPrefs.edit { remove(key) }
    }
}