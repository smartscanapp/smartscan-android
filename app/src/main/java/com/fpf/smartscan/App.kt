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
import com.fpf.smartscan.di.dbModule
import com.fpf.smartscan.di.embedStoreModule
import com.fpf.smartscan.di.modelsModule
import com.fpf.smartscan.di.viewModelModule
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
                modules(embedStoreModule, dbModule, viewModelModule, modelsModule)
            }
        }
    }
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(embedStoreModule, dbModule, viewModelModule, modelsModule)
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

        createNotificationChannel(
            channelId = getString(R.string.worker_channel_id),
            channelName = getString(R.string.worker_channel_name),
            description = getString(R.string.worker_channel_description)
        )
    }
    private fun createNotificationChannel(channelId: String, channelName: String, description: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH
        ).apply { this.description = description }
        notificationManager.createNotificationChannel(channel)
    }

}