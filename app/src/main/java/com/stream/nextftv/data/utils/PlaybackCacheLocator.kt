package com.stream.nextftv.data.utils

import android.content.Context
import com.stream.nextftv.presentation.player.PlaybackContentType
import java.io.File

object PlaybackCacheLocator {
    private const val ROOT_DIRECTORY = "gsy_cache"
    private const val TAG_LIVE = "live_tv"
    private const val TAG_MOVIE = "movie_detail"
    private const val TAG_SERIES = "series_detail"

    fun playTagFor(contentType: PlaybackContentType): String = when (contentType) {
        PlaybackContentType.LIVE -> TAG_LIVE
        PlaybackContentType.MOVIE -> TAG_MOVIE
        PlaybackContentType.SERIES -> TAG_SERIES
    }

    fun playTagForDownloadType(type: String): String? = when (type) {
        "movie" -> TAG_MOVIE
        "episode" -> TAG_SERIES
        else -> null
    }

    fun cacheDirectory(context: Context, playTag: String): File {
        return File(context.cacheDir, "$ROOT_DIRECTORY/$playTag")
    }
}
