package com.fpf.smartscan

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.fpf.smartscan.di.clusterModule
import com.fpf.smartscan.di.conceptModule
import com.fpf.smartscan.di.cryptoModule
import com.fpf.smartscan.di.dbModule
import com.fpf.smartscan.di.embedStoreModule
import com.fpf.smartscan.di.mediaModule
import com.fpf.smartscan.di.modelsModule
import com.fpf.smartscan.di.storageModule
import com.fpf.smartscan.di.tagModule
import com.fpf.smartscan.di.viewModelModule
import com.fpf.smartscan.notifications.NotificationChannels
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class App : Application() {

    companion object {
        private const val TAG = "App"

        fun resetKoin(app: Application){
            stopKoin()

            startKoin {
                androidContext(app)
                modules(
                    embedStoreModule,
                    dbModule,
                    viewModelModule,
                    modelsModule,
                    conceptModule,
                    clusterModule,
                    tagModule,
                    mediaModule,
                    cryptoModule,
                    storageModule
                )
            }
        }
    }
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                embedStoreModule,
                dbModule,
                viewModelModule,
                modelsModule,
                conceptModule,
                clusterModule,
                tagModule,
                mediaModule,
                cryptoModule,
                storageModule
            )
        }

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this).components {
                add(VideoFrameDecoder.Factory())
            }
                .crossfade(true)
                .memoryCache { MemoryCache.Builder().maxSizePercent(this, 0.25).build() }
                .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizePercent(0.05).build() }
                .build()
        }

        createNotificationChannels()
    }
    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val indexChannel = NotificationChannel(
            NotificationChannels.INDEX,
            getString(R.string.index_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.index_channel_description)
        }

        val indexServiceChannel = NotificationChannel(
            NotificationChannels.INDEX_SERVICE,
            getString(R.string.service_index_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )

        val conceptRemindersChannel = NotificationChannel(
            NotificationChannels.CONCEPT_REMINDERS,
            getString(R.string.concept_reminders_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.concept_reminders_channel_description)
        }

        notificationManager.createNotificationChannels(
            listOf(indexChannel, indexServiceChannel, conceptRemindersChannel)
        )

        // Delete old channels.
        notificationManager.deleteNotificationChannel(NotificationChannels.OLD_INDEX)
        notificationManager.deleteNotificationChannel(NotificationChannels.OLD_BACKGROUND)
    }

}