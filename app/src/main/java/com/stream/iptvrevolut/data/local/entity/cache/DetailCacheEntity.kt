package com.stream.iptvrevolut.data.local.entity.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "details_cache")
data class DetailCacheEntity(
    @PrimaryKey val id: String, // composite: type_streamId (e.g., "vod_12345")
    val type: String, // "vod" or "series"
    val dataJson: String, // JSON completo de Xtream + TMDB fusionado
    val lastUpdated: Long
)
