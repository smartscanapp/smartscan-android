package com.fpf.smartscan.di

import com.fpf.smartscan.tag.TagManager
import org.koin.dsl.module


val tagModule = module {
    single {
        TagManager(
            tagRepository = get(),
            tagCrossRefRepository = get(),
        )
    }
}
