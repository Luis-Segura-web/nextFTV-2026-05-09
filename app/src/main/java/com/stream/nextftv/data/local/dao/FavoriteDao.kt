package com.stream.nextftv.data.local.dao

import androidx.room.*
import com.stream.nextftv.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE profileId = :profileId AND streamId = :streamId AND contentType = :contentType)")
    fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean>

    @Query("SELECT streamId FROM favorites WHERE profileId = :profileId AND contentType = :contentType ORDER BY timestamp DESC")
    fun getFavoriteIds(profileId: Int, contentType: String): Flow<List<Int>>

    @Query("""
        DELETE FROM favorites
        WHERE profileId = :profileId
        AND contentType = 'live'
        AND streamId NOT IN (
            SELECT streamId FROM live_streams WHERE profileId = :profileId
        )
    """)
    suspend fun clearOrphanLiveFavorites(profileId: Int)

    @Query("""
        DELETE FROM favorites
        WHERE profileId = :profileId
        AND contentType = 'vod'
        AND streamId NOT IN (
            SELECT streamId FROM vod_streams WHERE profileId = :profileId
        )
    """)
    suspend fun clearOrphanVodFavorites(profileId: Int)

    @Query("""
        DELETE FROM favorites
        WHERE profileId = :profileId
        AND contentType = 'series'
        AND streamId NOT IN (
            SELECT seriesId FROM series_streams WHERE profileId = :profileId
        )
    """)
    suspend fun clearOrphanSeriesFavorites(profileId: Int)
}
