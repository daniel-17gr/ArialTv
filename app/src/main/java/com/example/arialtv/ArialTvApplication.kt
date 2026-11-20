package com.example.arialtv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Configures Coil globally for smooth 60fps Android TV scrolling:
 * - RGB_565 posters (cuts bitmap RAM in half)
 * - Hardware bitmaps enabled
 * - Aggressive memory (20% RAM) & 80 MB disk image cache
 */
class ArialTvApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(80L * 1024L * 1024L)
                    .build()
            }
            .allowRgb565(true)
            .allowHardware(true)
            .respectCacheHeaders(false)
            .crossfade(180)
            .build()
    }
}
