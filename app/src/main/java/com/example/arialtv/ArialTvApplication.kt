package com.example.arialtv

import android.app.Application
import android.content.ComponentCallbacks2
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.arialtv.util.CrashLogger

/**
 * Low-RAM Android TV Coil configuration:
 * - Lean 10% memory cache & 40 MB disk cache to avoid GC pauses / OOM on 1–2 GB TV boxes.
 * - RGB_565 + software bitmaps (avoids HWUI/RenderThread hardware-bitmap crashes on Android TV 14) + crossfade disabled.
 * - Immediate memory cache eviction on Android TV trim/low-memory signals.
 */
class ArialTvApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(40L * 1024L * 1024L)
                    .build()
            }
            .allowRgb565(true)
            .allowHardware(false)
            .respectCacheHeaders(false)
            .crossfade(false)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        ) {
            runCatching { Coil.imageLoader(this).memoryCache?.clear() }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        runCatching { Coil.imageLoader(this).memoryCache?.clear() }
    }
}
