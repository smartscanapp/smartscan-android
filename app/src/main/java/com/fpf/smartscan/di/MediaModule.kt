package com.fpf.smartscan.di

import com.fpf.smartscan.media.MediaJobManager
import org.koin.dsl.module


val mediaModule = module {
    single {
        MediaJobManager(
            conceptManager = get(),
            mediaMetadataRepository = get(),
            modelRepository = get()
        )
    }
}
