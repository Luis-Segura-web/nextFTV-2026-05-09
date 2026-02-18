package com.stream.iptvrevolut.data.local.dao

import androidx.room.*
import com.stream.iptvrevolut.data.local.entity.FavoriteEntity
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
}
