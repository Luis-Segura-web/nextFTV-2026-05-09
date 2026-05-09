package com.stream.nextftv.data.local.entity.vod

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vod_streams",
    primaryKeys = ["profileId", "streamId"],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["normalizedName"]),
        Index(value = ["naturalSortName"])
    ]
)
data class VodStreamEntity(
    val profileId: Int,
    val categoryId: String,
    val streamId: Int,
    val num: Int,
    val name: String,
    val normalizedName: String,
    val naturalSortName: String,
    val streamType: String,
    val streamIcon: String?,
    val rating: String?,
    val rating5Based: Double? = null,
    val added: String?,
    val customSid: String? = null,
    val containerExtension: String?,
    val directSource: String?,
    val backdropPath: List<String>? = null,
    val tmdbId: Int? = null,
    val backdropUrl: String? = null
)
