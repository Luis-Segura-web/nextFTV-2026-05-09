package com.stream.iptvrevolut.data.sync

/**
 * Represents the three content modules in the Xtream Code API.
 */
enum class ContentModule(val key: String) {
    LIVE("live"),
    VOD("movies"),
    SERIES("series");

    companion object {
        fun fromKey(key: String): ContentModule = when (key) {
            "live" -> LIVE
            "movies" -> VOD
            "series" -> SERIES
            else -> throw IllegalArgumentException("Unknown module key: $key")
        }
    }
}

