package com.stream.nextftv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stream.nextftv.domain.model.ServerProfile

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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

fun ProfileEntity.toDomain() = ServerProfile(
    id = id,
    name = name,
    url = url,
    username = username,
    password = password,
    sourceType = sourceType,
    isActive = isActive,
    lastLiveSync = lastLiveSync,
    lastMoviesSync = lastMoviesSync,
    lastSeriesSync = lastSeriesSync,
    expirationDate = expirationDate,
    accountStatus = accountStatus,
    serverTimezone = serverTimezone,
    activeConnections = activeConnections,
    maxConnections = maxConnections,
    allowedOutputFormats = allowedOutputFormats,
    epgUrl = epgUrl
)

fun ServerProfile.toEntity() = ProfileEntity(
    id = id,
    name = name,
    url = url,
    username = username,
    password = password,
    sourceType = sourceType,
    isActive = isActive,
    lastLiveSync = lastLiveSync,
    lastMoviesSync = lastMoviesSync,
    lastSeriesSync = lastSeriesSync,
    expirationDate = expirationDate,
    accountStatus = accountStatus,
    serverTimezone = serverTimezone,
    activeConnections = activeConnections,
    maxConnections = maxConnections,
    allowedOutputFormats = allowedOutputFormats,
    epgUrl = epgUrl
)
