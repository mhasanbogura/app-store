package com.example

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppStoreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val imageClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { imageClient }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.05)
                        .build()
                }
                .crossfade(true)
                .build()
        )
    }
}
