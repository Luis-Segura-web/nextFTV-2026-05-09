package com.stream.nextftv.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PlaybackEnginePreferences {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_LIVE_TV_ENGINE = "live_tv_playback_engine"
    private const val KEY_MOVIES_ENGINE = "movies_playback_engine"
    private const val KEY_SERIES_ENGINE = "series_playback_engine"
    private val DEFAULT_LIVE_TV_ENGINE = PlaybackEngine.EXO
    private val DEFAULT_MOVIES_ENGINE = PlaybackEngine.EXO
    private val DEFAULT_SERIES_ENGINE = PlaybackEngine.EXO

    var liveTvEngine: PlaybackEngine by mutableStateOf(DEFAULT_LIVE_TV_ENGINE)
        private set

    var moviesEngine: PlaybackEngine by mutableStateOf(DEFAULT_MOVIES_ENGINE)
        private set

    var seriesEngine: PlaybackEngine by mutableStateOf(DEFAULT_SERIES_ENGINE)
        private set

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        liveTvEngine = PlaybackEngine.fromStoredValue(
            prefs.getString(KEY_LIVE_TV_ENGINE, DEFAULT_LIVE_TV_ENGINE.name)
        )
        moviesEngine = PlaybackEngine.fromStoredValue(
            prefs.getString(KEY_MOVIES_ENGINE, DEFAULT_MOVIES_ENGINE.name)
        )
        seriesEngine = PlaybackEngine.fromStoredValue(
            prefs.getString(KEY_SERIES_ENGINE, DEFAULT_SERIES_ENGINE.name)
        )
    }

    fun update(context: Context, contentType: PlaybackContentType, engine: PlaybackEngine) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = when (contentType) {
            PlaybackContentType.LIVE -> KEY_LIVE_TV_ENGINE
            PlaybackContentType.MOVIE -> KEY_MOVIES_ENGINE
            PlaybackContentType.SERIES -> KEY_SERIES_ENGINE
        }
        prefs.edit().putString(key, engine.name).apply()
        when (contentType) {
            PlaybackContentType.LIVE -> liveTvEngine = engine
            PlaybackContentType.MOVIE -> moviesEngine = engine
            PlaybackContentType.SERIES -> seriesEngine = engine
        }
    }

    fun engineFor(contentType: PlaybackContentType): PlaybackEngine {
        return when (contentType) {
            PlaybackContentType.LIVE -> liveTvEngine
            PlaybackContentType.MOVIE -> moviesEngine
            PlaybackContentType.SERIES -> seriesEngine
        }
    }
}
