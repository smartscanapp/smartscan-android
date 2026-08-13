package com.fpf.smartscan.di

import android.content.Context
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.core.storage.EncryptedStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val storageModule = module {
    single { androidContext().getSharedPreferences(PrefsNames.APP_PREFS, Context.MODE_PRIVATE) }
    single { EncryptedStorage(sharedPrefs = get(), cryptoUtils = get()) }
}