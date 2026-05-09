package com.stream.nextftv.domain.repository

import com.stream.nextftv.data.local.entity.vod.VodCategoryEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.nextftv.data.remote.tmdb.TmdbPersonSearchResultDto
import com.stream.nextftv.domain.model.ServerProfile
import kotlinx.coroutines.flow.Flow

interface VodRepository {

    fun getCategories(profileId: Int): Flow<List<VodCategoryEntity>>
    fun getStreams(profileId: Int, categoryId: String?): Flow<List<VodStreamEntity>>
    suspend fun getStream(profileId: Int, streamId: Int): VodStreamEntity?
    fun getStreamCount(profileId: Int): Flow<Int>
    fun getPagedStreams(profileId: Int, categoryId: String?, query: String, sortOrder: String): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<VodStreamEntity>>
    fun searchStreams(profileId: Int, query: String): Flow<List<VodStreamEntity>>
    suspend fun searchStreamsEnhanced(profileId: Int, categoryId: String?, query: String): List<VodStreamEntity>
    suspend fun searchActors(profileId: Int, query: String): List<TmdbPersonSearchResultDto>
    
    suspend fun getVodInfo(profile: ServerProfile, streamId: Int): Result<com.stream.nextftv.data.remote.VodInfoDto>
    suspend fun getMovieDetails(stream: VodStreamEntity): Result<TmdbMovieDetailsDto>
    suspend fun getMovieCollection(collectionId: Int): Result<com.stream.nextftv.data.remote.tmdb.TmdbCollectionDto>

    suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String)
    fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean>
    suspend fun getStreamsByTmdbId(profileId: Int, tmdbId: String): List<VodStreamEntity>
    suspend fun getStreamsByTmdbIds(profileId: Int, tmdbIds: List<Int>): List<VodStreamEntity>
    suspend fun getStreamsByName(profileId: Int, normalizedName: String): List<VodStreamEntity>
    
    suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String)
}
