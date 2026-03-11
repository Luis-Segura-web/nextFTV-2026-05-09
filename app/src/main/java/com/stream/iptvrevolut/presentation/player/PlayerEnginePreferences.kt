package com.stream.iptvrevolut.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PlayerEnginePreferences {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_PLAYER_ENGINE = "preferred_player_engine"

    var selectedEngine: PlayerEngine by mutableStateOf(PlayerEngine.MEDIA3)
        private set

    var onChanged: ((PlayerEngine) -> Unit)? = null

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_PLAYER_ENGINE, PlayerEngine.MEDIA3.name)
        selectedEngine = stored
            ?.let { raw -> PlayerEngine.entries.find { it.name == raw } }
            ?: PlayerEngine.MEDIA3
        onChanged?.invoke(selectedEngine)
    }

    fun updateSelectedEngine(engine: PlayerEngine) {
        selectedEngine = engine
        onChanged?.invoke(engine)
    }
}

