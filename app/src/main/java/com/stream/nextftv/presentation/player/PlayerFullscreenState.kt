package com.stream.nextftv.presentation.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PlayerFullscreenState {
    private var isFullscreenState by mutableStateOf(false)

    var isFullscreen: Boolean
        get() = isFullscreenState
        set(value) {
            isFullscreenState = value
            onChanged?.invoke()
        }

    var onChanged: (() -> Unit)? = null
}
