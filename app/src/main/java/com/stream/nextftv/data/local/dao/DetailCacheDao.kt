package com.stream.nextftv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stream.nextftv.data.local.entity.cache.DetailCacheEntity

@Dao
interface DetailCacheDao {
    @Query("SELECT * FROM details_cache WHERE id = :id")
    suspend fun getDetail(id: String): DetailCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: DetailCacheEntity)

    @Query("DELETE FROM details_cache WHERE lastUpdated < :threshold")
    suspend fun clearOldCache(threshold: Long)

    @Query("DELETE FROM details_cache")
    suspend fun clearAllCache()

    @Query("DELETE FROM details_cache WHERE id LIKE 'vod_%'")
    suspend fun clearVodCache()

    @Query("DELETE FROM details_cache WHERE id LIKE 'episodes_%'")
    suspend fun clearSeriesEpisodesCache()
}
