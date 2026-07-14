package com.fpf.smartscan.di

import android.app.Application
import com.fpf.smartscan.data.ModelRepository
import org.koin.dsl.module

val modelsModule = module {
    single {
        val app = get<Application>()
        ModelRepository(app)
    }
}
