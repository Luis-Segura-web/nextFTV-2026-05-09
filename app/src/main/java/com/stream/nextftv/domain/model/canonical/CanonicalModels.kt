package com.stream.nextftv.domain.model.canonical

data class CatalogItem(
    val id: String,
    val type: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val tmdbId: Int? = null,
    val categoryId: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating10: Double? = null,
    val year: Int? = null,
    val addedAt: Long? = null, // Epoch ms
    val epgChannelId: String? = null,
    val containerExtension: String? = null,
    val directSource: String? = null
)

enum class MediaType {
    LIVE, MOVIE, SERIES, EPISODE
}

data class DetailItem(
    val id: String,
    val type: MediaType,
    val title: String?,
    val overview: String? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val directors: List<String> = emptyList(),
    val runtimeSec: Int? = null,
    val releaseDate: String? = null,
    val seasons: List<Season> = emptyList(),
    val episodes: List<Episode> = emptyList() // Flattened list option
)

data class Season(
    val seasonNumber: Int?,
    val name: String?,
    val overview: String?,
    val airDate: String?,
    val posterUrl: String?,
    val episodeCount: Int?
)

data class Episode(
    val id: String,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val title: String?,
    val overview: String?,
    val runtimeSec: Int?,
    val airDate: String?,
    val stillUrl: String?,
    val containerExtension: String?
)
