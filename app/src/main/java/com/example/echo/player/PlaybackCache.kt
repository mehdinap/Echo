package com.example.echo.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File

/**
 * Streamed bytes are written to disk as they play, so a replay or a seek backwards costs
 * nothing. 512 MB, least-recently-used eviction. Downloads live elsewhere (app files dir)
 * and are read straight off the filesystem — this cache only backs streaming.
 */
@UnstableApi
object PlaybackCache {
    private const val MAX_BYTES = 512L * 1024 * 1024

    @Volatile private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.cacheDir, "media"),
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context),
        ).also { cache = it }
    }

    fun factory(context: Context, okHttpClient: OkHttpClient): DataSource.Factory {
        val upstream = DefaultDataSource.Factory(
            context,
            OkHttpDataSource.Factory(okHttpClient),
        )
        return CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstream)
            // A cache miss must never stall playback; write-through happens in the background.
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
