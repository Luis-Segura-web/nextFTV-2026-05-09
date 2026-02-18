package com.stream.iptvrevolut.data.local.dao

import androidx.room.*
import com.stream.iptvrevolut.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfile(id: Int)

    @Query("UPDATE profiles SET lastLiveSync = :timestamp WHERE id = :id")
    suspend fun updateLiveSyncTime(id: Int, timestamp: Long)

    @Query("UPDATE profiles SET lastMoviesSync = :timestamp WHERE id = :id")
    suspend fun updateMoviesSyncTime(id: Int, timestamp: Long)

    @Query("UPDATE profiles SET lastSeriesSync = :timestamp WHERE id = :id")
    suspend fun updateSeriesSyncTime(id: Int, timestamp: Long)

    @Query("UPDATE profiles SET expirationDate = :expDate, accountStatus = :status WHERE id = :id")
    suspend fun updateAccountInfo(id: Int, expDate: String, status: String)
}
