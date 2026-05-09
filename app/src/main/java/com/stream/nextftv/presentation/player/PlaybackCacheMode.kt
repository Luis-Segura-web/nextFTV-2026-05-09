package com.stream.nextftv.presentation.player

enum class PlaybackCacheMode {
    DISABLED,
    PROXY,
    EXO;

    companion object {
        fun fromStoredValue(value: String?): PlaybackCacheMode {
            return entries.firstOrNull { it.name == value } ?: DISABLED
        }
    }
}
