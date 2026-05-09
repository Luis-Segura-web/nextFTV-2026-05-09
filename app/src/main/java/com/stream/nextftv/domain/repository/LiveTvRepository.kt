package com.stream.nextftv.domain.repository

import androidx.paging.PagingData

import com.stream.nextftv.data.local.entity.live.LiveCategoryEntity

import com.stream.nextftv.data.local.entity.live.LiveStreamEntity

import kotlinx.coroutines.flow.Flow



interface LiveTvRepository {


    fun getCategories(profileId: Int): Flow<List<LiveCategoryEntity>>

    fun getStreams(profileId: Int, categoryId: String?): Flow<List<LiveStreamEntity>>

    fun getStreamCount(profileId: Int): Flow<Int>

    fun getPagedStreams(profileId: Int, categoryId: String?, query: String, sortOrder: String): Flow<PagingData<LiveStreamEntity>>

    suspend fun getFilteredStreams(profileId: Int, categoryId: String?, query: String, sortOrder: String): List<LiveStreamEntity>

    fun searchStreams(profileId: Int, query: String): Flow<List<LiveStreamEntity>>


    
    suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String)
    fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean>
    fun getFavorites(profileId: Int, contentType: String): Flow<List<LiveStreamEntity>>
    
    suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String)
}
