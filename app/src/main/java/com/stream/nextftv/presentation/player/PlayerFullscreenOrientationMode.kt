package com.stream.nextftv.presentation.player

enum class PlayerFullscreenOrientationMode {
    PORTRAIT,
    LANDSCAPE,
    AUTO;

    companion object {
        fun fromStoredValue(value: String?): PlayerFullscreenOrientationMode {
            return entries.firstOrNull { it.name == value } ?: AUTO
        }
    }
}
