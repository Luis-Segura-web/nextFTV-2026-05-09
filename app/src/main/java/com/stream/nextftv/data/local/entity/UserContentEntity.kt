package com.stream.nextftv.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["profileId", "streamId", "contentType"],
    indices = [Index(value = ["profileId"])]
)
data class FavoriteEntity(
    val profileId: Int,
    val streamId: Int,
    val contentType: String, // "live", "vod", "series"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recents",
    primaryKeys = ["profileId", "streamId", "contentType"],
    indices = [Index(value = ["profileId"])]
)
data class RecentEntity(
    val profileId: Int,
    val streamId: Int,
    val contentType: String,
    val timestamp: Long = System.currentTimeMillis()
)