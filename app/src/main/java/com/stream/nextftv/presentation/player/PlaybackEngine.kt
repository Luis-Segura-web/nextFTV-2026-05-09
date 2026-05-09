package com.stream.nextftv.presentation.player

enum class PlaybackEngine {
    IJK,
    EXO;

    companion object {
        fun fromStoredValue(value: String?): PlaybackEngine {
            return entries.firstOrNull { it.name == value } ?: IJK
        }
    }
}
