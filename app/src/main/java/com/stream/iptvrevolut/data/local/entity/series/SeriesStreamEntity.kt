package com.stream.iptvrevolut.data.local.entity.series

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "series_streams",
    primaryKeys = ["profileId", "categoryId", "seriesId"],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["categoryId"]),
        Index(value = ["normalizedName"]),
        Index(value = ["naturalSortName"])
    ]
)
data class SeriesStreamEntity(
    val profileId: Int,
    val categoryId: String,
    val seriesId: Int,
    val num: Int?,
    val name: String,
    val normalizedName: String,
    val naturalSortName: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val backdropPath: List<String>?,
    val tmdbId: Int? = null
)