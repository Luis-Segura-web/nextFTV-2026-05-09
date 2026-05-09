package com.stream.nextftv.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PlaybackCachePreferences {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_LIVE_TV_CACHE = "live_tv_playback_cache"
    private const val KEY_MOVIES_CACHE = "movies_playback_cache"
    private const val KEY_SERIES_CACHE = "series_playback_cache"
    private val DEFAULT_LIVE_TV_CACHE = PlaybackCacheMode.DISABLED
    private val DEFAULT_MOVIES_CACHE = PlaybackCacheMode.PROXY
    private val DEFAULT_SERIES_CACHE = PlaybackCacheMode.PROXY

    var liveTvCacheMode: PlaybackCacheMode by mutableStateOf(DEFAULT_LIVE_TV_CACHE)
        private set

    var moviesCacheMode: PlaybackCacheMode by mutableStateOf(DEFAULT_MOVIES_CACHE)
        private set

    var seriesCacheMode: PlaybackCacheMode by mutableStateOf(DEFAULT_SERIES_CACHE)
        private set

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        liveTvCacheMode = DEFAULT_LIVE_TV_CACHE
        if (prefs.getString(KEY_LIVE_TV_CACHE, DEFAULT_LIVE_TV_CACHE.name) != DEFAULT_LIVE_TV_CACHE.name) {
            prefs.edit().putString(KEY_LIVE_TV_CACHE, DEFAULT_LIVE_TV_CACHE.name).apply()
        }
        moviesCacheMode = PlaybackCacheMode.fromStoredValue(
            prefs.getString(KEY_MOVIES_CACHE, DEFAULT_MOVIES_CACHE.name)
        )
        seriesCacheMode = PlaybackCacheMode.fromStoredValue(
            prefs.getString(KEY_SERIES_CACHE, DEFAULT_SERIES_CACHE.name)
        )
    }

    fun update(contentType: PlaybackContentType, mode: PlaybackCacheMode) {
        when (contentType) {
            PlaybackContentType.LIVE -> liveTvCacheMode = DEFAULT_LIVE_TV_CACHE
            PlaybackContentType.MOVIE -> moviesCacheMode = mode
            PlaybackContentType.SERIES -> seriesCacheMode = mode
        }
    }

    fun cacheModeFor(contentType: PlaybackContentType): PlaybackCacheMode {
        return when (contentType) {
            PlaybackContentType.LIVE -> liveTvCacheMode
            PlaybackContentType.MOVIE -> moviesCacheMode
            PlaybackContentType.SERIES -> seriesCacheMode
        }
    }
}
