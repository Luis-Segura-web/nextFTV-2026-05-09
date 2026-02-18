package com.stream.iptvrevolut.domain.model

data class ServerProfile(
    val id: Int = 0,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val isActive: Boolean = false,
    val lastLiveSync: Long = 0L,
    val lastMoviesSync: Long = 0L,
    val lastSeriesSync: Long = 0L,
    val expirationDate: String? = null,
    val accountStatus: String? = null
)
