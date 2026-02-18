package com.stream.iptvrevolut.domain.repository

import androidx.paging.PagingData

import com.stream.iptvrevolut.data.local.entity.live.LiveCategoryEntity

import com.stream.iptvrevolut.data.local.entity.live.LiveStreamEntity

import com.stream.iptvrevolut.domain.model.ServerProfile

import kotlinx.coroutines.flow.Flow



interface LiveTvRepository {

    suspend fun syncCategories(profile: ServerProfile): Result<Unit>

    suspend fun syncStreams(profile: ServerProfile, categoryId: String? = null): Result<Unit>

    fun getCategories(profileId: Int): Flow<List<LiveCategoryEntity>>

    fun getStreams(profileId: Int, categoryId: String?): Flow<List<LiveStreamEntity>>

    fun getPagedStreams(profileId: Int, categoryId: String?, query: String, sortOrder: String): Flow<PagingData<LiveStreamEntity>>

    suspend fun getFilteredStreams(profileId: Int, categoryId: String?, query: String, sortOrder: String): List<LiveStreamEntity>

    fun searchStreams(profileId: Int, query: String): Flow<List<LiveStreamEntity>>


    
    suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String)
    fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean>
    fun getFavorites(profileId: Int, contentType: String): Flow<List<LiveStreamEntity>>
    
    suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String)
}
