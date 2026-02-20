package com.stream.iptvrevolut.domain.repository

import com.stream.iptvrevolut.data.local.entity.series.SeriesCategoryEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import com.stream.iptvrevolut.data.remote.SeriesEpisodeDto
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.iptvrevolut.domain.model.ServerProfile
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {
    fun getCategories(profileId: Int): Flow<List<SeriesCategoryEntity>>
    fun getSeries(profileId: Int, categoryId: String?): Flow<List<SeriesStreamEntity>>
    fun getPagedSeries(profileId: Int, categoryId: String?, query: String, sortOrder: String): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<SeriesStreamEntity>>
    fun searchSeries(profileId: Int, query: String): Flow<List<SeriesStreamEntity>>
    
    suspend fun getSeriesEpisodes(profile: ServerProfile, seriesId: Int): Result<Map<String, List<SeriesEpisodeDto>>>
    suspend fun getSeriesDetails(series: SeriesStreamEntity): Result<TmdbMovieDetailsDto>

    suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String)
    fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean>
    suspend fun getSeriesListByTmdbId(profileId: Int, tmdbId: String): List<SeriesStreamEntity>
    suspend fun getSeriesListByName(profileId: Int, normalizedName: String): List<SeriesStreamEntity>
    
    suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String)
}
