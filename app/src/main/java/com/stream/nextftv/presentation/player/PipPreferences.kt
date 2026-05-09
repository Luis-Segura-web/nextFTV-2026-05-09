package com.stream.nextftv.presentation.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PipPreferences {
    private const val PREFS_NAME = "settings_prefs"
    private const val KEY_PIP_ENABLED = "pip_enabled"

    var isEnabled: Boolean by mutableStateOf(true)
        private set

    var onChanged: ((Boolean) -> Unit)? = null

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean(KEY_PIP_ENABLED, true)
        onChanged?.invoke(isEnabled)
    }

    fun updateEnabled(enabled: Boolean) {
        isEnabled = enabled
        onChanged?.invoke(enabled)
    }
}
