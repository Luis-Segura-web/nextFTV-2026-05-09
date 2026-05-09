package com.stream.nextftv.data.mapper

import com.stream.nextftv.data.local.entity.series.SeriesCategoryEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamCategoryRefEntity
import com.stream.nextftv.data.remote.SeriesCategoryDto
import com.stream.nextftv.data.remote.SeriesStreamDto
import com.stream.nextftv.data.utils.StringUtils

fun SeriesCategoryDto.toEntity(profileId: Int, orderIndex: Int = 0) = SeriesCategoryEntity(
    profileId = profileId,
    categoryId = categoryId,
    categoryName = categoryName,
    parentId = parentId ?: 0,
    orderIndex = orderIndex
)

fun SeriesStreamDto.toEntity(profileId: Int): SeriesStreamEntity {
    val safeName = name ?: "Unknown"
    return SeriesStreamEntity(
        profileId = profileId,
        categoryId = categoryId ?: "0",
        seriesId = seriesId ?: 0,
        num = num,
        name = safeName,
        normalizedName = StringUtils.normalize(safeName),
        naturalSortName = StringUtils.naturalSort(safeName),
        streamType = "series",
        streamIcon = cover,
        cover = cover,
        plot = plot,
        cast = cast,
        director = director,
        genre = genre,
        releaseDate = formatReleaseDate(releaseDate),
        lastModified = lastModified,
        rating = rating,
        rating5Based = rating5Based,
        backdropPath = backdropPath,
        backdropUrl = backdropPath?.firstOrNull(),
        youtubeTrailer = youtubeTrailer,
        episodeRunTime = episodeRunTime,
        tmdbId = tmdbId
    )
}

fun SeriesStreamDto.toCategoryRef(profileId: Int): SeriesStreamCategoryRefEntity? {
    val safeSeriesId = seriesId ?: return null
    return SeriesStreamCategoryRefEntity(
        profileId = profileId,
        seriesId = safeSeriesId,
        categoryId = categoryId ?: "0"
    )
}

fun List<SeriesCategoryDto>.toEntities(profileId: Int) = mapIndexed { index, item ->
    item.toEntity(profileId, index)
}

/**
 * Normalizes release date from various formats (timestamp, date string).
 */
private fun formatReleaseDate(rawDate: String?): String? {
    if (rawDate.isNullOrBlank() || rawDate == "null" || rawDate == "0") return null
    if (rawDate.contains("-")) return rawDate
    return try {
        val timestamp = rawDate.toLong()
        val date = if (timestamp > 1_000_000_000_000L) {
            java.util.Date(timestamp)
        } else {
            java.util.Date(timestamp * 1000)
        }
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
    } catch (_: Exception) {
        rawDate
    }
}
