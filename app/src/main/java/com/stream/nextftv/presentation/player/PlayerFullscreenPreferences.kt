package com.stream.nextftv.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PlayerFullscreenPreferences {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_FULLSCREEN_ORIENTATION_MODE = "player_fullscreen_orientation_mode"

    var fullscreenOrientationMode: PlayerFullscreenOrientationMode by mutableStateOf(PlayerFullscreenOrientationMode.AUTO)
        private set

    var onChanged: ((PlayerFullscreenOrientationMode) -> Unit)? = null

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        fullscreenOrientationMode = PlayerFullscreenOrientationMode.fromStoredValue(
            prefs.getString(KEY_FULLSCREEN_ORIENTATION_MODE, PlayerFullscreenOrientationMode.AUTO.name)
        )
        onChanged?.invoke(fullscreenOrientationMode)
    }

    fun updateFullscreenOrientationMode(context: Context, mode: PlayerFullscreenOrientationMode) {
        fullscreenOrientationMode = mode
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FULLSCREEN_ORIENTATION_MODE, mode.name)
            .apply()
        onChanged?.invoke(mode)
    }
}
