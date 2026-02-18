package com.stream.iptvrevolut.data.local.entity.vod

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vod_streams",
    primaryKeys = ["profileId", "categoryId", "streamId"],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["categoryId"]),
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
    val added: String?,
    val containerExtension: String?,
    val directSource: String?,
    val tmdbId: Int? = null,
    val backdropUrl: String? = null
)