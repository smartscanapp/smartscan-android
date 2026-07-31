package com.fpf.smartscan.di

import com.fpf.smartscan.core.tag.TagManager
import org.koin.dsl.module

val tagModule = module {
    single {
        TagManager(
            tagRepository = get(),
            tagCrossRefRepository = get(),
        )
    }
}
