package com.stream.iptvrevolut.domain.repository

import com.stream.iptvrevolut.data.local.entity.vod.VodCategoryEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodStreamEntity
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.iptvrevolut.domain.model.ServerProfile
import kotlinx.coroutines.flow.Flow

interface VodRepository {
    suspend fun syncCategories(profile: ServerProfile): Result<Unit>
    suspend fun syncStreams(profile: ServerProfile, categoryId: String? = null): Result<Unit>
    
    fun getCategories(profileId: Int): Flow<List<VodCategoryEntity>>
    fun getStreams(profileId: Int, categoryId: String?): Flow<List<VodStreamEntity>>
    fun getPagedStreams(profileId: Int, categoryId: String?, query: String, sortOrder: String): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<VodStreamEntity>>
    fun searchStreams(profileId: Int, query: String): Flow<List<VodStreamEntity>>
    
    suspend fun getVodInfo(profile: ServerProfile, streamId: Int): Result<com.stream.iptvrevolut.data.remote.VodInfoDto>
    suspend fun getMovieDetails(stream: VodStreamEntity): Result<TmdbMovieDetailsDto>
    suspend fun getMovieCollection(collectionId: Int): Result<com.stream.iptvrevolut.data.remote.tmdb.TmdbCollectionDto>

    suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String)
    fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean>
    suspend fun getStreamsByTmdbId(profileId: Int, tmdbId: String): List<VodStreamEntity>
    suspend fun getStreamsByName(profileId: Int, normalizedName: String): List<VodStreamEntity>
    
    suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String)
}
