package com.stream.nextftv.data.local.entity.series

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "series_streams",
    primaryKeys = ["profileId", "seriesId"],
    indices = [
        Index(value = ["profileId"]),
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
    val streamType: String,
    val streamIcon: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5Based: Double? = null,
    val backdropPath: List<String>?,
    val backdropUrl: String? = null,
    val youtubeTrailer: String? = null,
    val episodeRunTime: String? = null,
    val tmdbId: Int? = null
)
