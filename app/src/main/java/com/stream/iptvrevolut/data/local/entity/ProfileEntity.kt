package com.stream.iptvrevolut.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stream.iptvrevolut.domain.model.ServerProfile

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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

fun ProfileEntity.toDomain() = ServerProfile(
    id = id,
    name = name,
    url = url,
    username = username,
    password = password,
    isActive = isActive,
    lastLiveSync = lastLiveSync,
    lastMoviesSync = lastMoviesSync,
    lastSeriesSync = lastSeriesSync,
    expirationDate = expirationDate,
    accountStatus = accountStatus
)

fun ServerProfile.toEntity() = ProfileEntity(
    id = id,
    name = name,
    url = url,
    username = username,
    password = password,
    isActive = isActive,
    lastLiveSync = lastLiveSync,
    lastMoviesSync = lastMoviesSync,
    lastSeriesSync = lastSeriesSync,
    expirationDate = expirationDate,
    accountStatus = accountStatus
)
