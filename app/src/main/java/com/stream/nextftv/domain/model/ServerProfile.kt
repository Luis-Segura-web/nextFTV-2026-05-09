package com.stream.nextftv.domain.model

data class ServerProfile(
    val id: Int = 0,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val sourceType: String = "xtream",
    val isActive: Boolean = false,
    val lastLiveSync: Long = 0L,
    val lastMoviesSync: Long = 0L,
    val lastSeriesSync: Long = 0L,
    val expirationDate: String? = null,
    val accountStatus: String? = null,
    val serverTimezone: String? = null,
    val activeConnections: Int? = null,
    val maxConnections: Int? = null,
    val allowedOutputFormats: List<String>? = null,
    val epgUrl: String? = null
)
