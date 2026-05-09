package com.stream.nextftv.domain.repository

import com.stream.nextftv.data.local.entity.series.SeriesCategoryEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.remote.SeriesEpisodeDto
import com.stream.nextftv.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.nextftv.data.remote.tmdb.TmdbPersonSearchResultDto
import com.stream.nextftv.domain.model.ServerProfile
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {
    fun getCategories(profileId: Int): Flow<List<SeriesCategoryEntity>>
    fun getSeries(profileId: Int, categoryId: String?): Flow<List<SeriesStreamEntity>>
    suspend fun getSeries(profileId: Int, seriesId: Int): SeriesStreamEntity?
    fun getSeriesCount(profileId: Int): Flow<Int>
    fun getPagedSeries(profileId: Int, categoryId: String?, query: String, sortOrder: String): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<SeriesStreamEntity>>
    fun searchSeries(profileId: Int, query: String): Flow<List<SeriesStreamEntity>>
    suspend fun searchSeriesEnhanced(profileId: Int, categoryId: String?, query: String): List<SeriesStreamEntity>
    suspend fun searchActors(profileId: Int, query: String): List<TmdbPersonSearchResultDto>
    
    suspend fun getSeriesEpisodes(profile: ServerProfile, seriesId: Int): Result<Map<String, List<SeriesEpisodeDto>>>
    suspend fun getSeriesDetails(series: SeriesStreamEntity): Result<TmdbMovieDetailsDto>
    suspend fun getSeriesEpisodeTitles(series: SeriesStreamEntity, seasonNumbers: Set<Int>): Result<Map<Pair<Int, Int>, String>>

    suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String)
    fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean>
    suspend fun getSeriesListByTmdbId(profileId: Int, tmdbId: String): List<SeriesStreamEntity>
    suspend fun getSeriesListByTmdbIds(profileId: Int, tmdbIds: List<Int>): List<SeriesStreamEntity>
    suspend fun getSeriesListByName(profileId: Int, normalizedName: String): List<SeriesStreamEntity>
    
    suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String)
}
