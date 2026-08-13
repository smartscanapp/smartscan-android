package com.fpf.smartscan.di

import com.fpf.smartscan.core.utils.CryptoUtils
import org.koin.dsl.module


val cryptoModule = module {
    single {
        CryptoUtils(
            context = get(),
        )
    }
}