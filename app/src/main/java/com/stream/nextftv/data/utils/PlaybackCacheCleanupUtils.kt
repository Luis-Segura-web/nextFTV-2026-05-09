package com.stream.nextftv.data.utils

import android.content.Context
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager
import java.io.File
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager

object PlaybackCacheCleanupUtils {
    fun clearPlaybackCaches(
        context: Context,
        playTag: String,
        remoteUrl: String?
    ) {
        val normalizedUrl = remoteUrl?.takeIf { it.isNotBlank() } ?: return
        val cacheDirectory = PlaybackCacheLocator.cacheDirectory(context, playTag)
        ProxyCacheManager.instance().clearCache(context, cacheDirectory, normalizedUrl)
        ExoPlayerCacheManager().clearCache(context, cacheDirectory, normalizedUrl)
    }

    fun clearAllPlaybackCaches(context: Context) {
        val cacheRoot = context.cacheDir.resolve("gsy_cache")
        if (cacheRoot.exists()) {
            cacheRoot.deleteRecursively()
        }
        cacheRoot.mkdirs()
    }
}
