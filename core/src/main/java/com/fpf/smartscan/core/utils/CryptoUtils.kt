package com.fpf.smartscan.core.utils

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import android.util.Base64


class CryptoUtils(context: Context) {
    companion object {
        private const val KEYSET_PREFS = "smartscan_tink_prefs"
        private const val KEYSET_NAME = "smartscan_tink_keyset"
        private const val MASTER_KEY_ALIAS = "smartscan_tink_master_key"
    }

    private val aead: Aead

    init {
        AeadConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
            .build()
            .keysetHandle

        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    fun encrypt(value: String, associatedData: ByteArray = ByteArray(0)): ByteArray =
        aead.encrypt(value.toByteArray(Charsets.UTF_8), associatedData)

    fun decrypt(ciphertext: ByteArray, associatedData: ByteArray = ByteArray(0)): String =
        String(aead.decrypt(ciphertext, associatedData), Charsets.UTF_8)

    fun toBase64(byteArray: ByteArray): String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

    fun toByteArray(base64Str: String): ByteArray = Base64.decode(base64Str, Base64.NO_WRAP)

}