package com.stream.nextftv.data.local.dao

import androidx.room.*
import com.stream.nextftv.data.local.entity.SyncStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(syncStatus: SyncStatusEntity)

    @Query("SELECT * FROM sync_status WHERE profileId = :profileId AND module = :module")
    suspend fun getStatus(profileId: Int, module: String): SyncStatusEntity?

    @Query("SELECT * FROM sync_status WHERE profileId = :profileId AND module = :module")
    fun observeStatus(profileId: Int, module: String): Flow<SyncStatusEntity?>

    @Query("SELECT * FROM sync_status WHERE profileId = :profileId")
    fun observeAllStatuses(profileId: Int): Flow<List<SyncStatusEntity>>

    @Query("SELECT * FROM sync_status WHERE profileId = :profileId")
    suspend fun getAllStatuses(profileId: Int): List<SyncStatusEntity>

    @Query("DELETE FROM sync_status WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: Int)

    @Query("UPDATE sync_status SET status = :status WHERE profileId = :profileId AND module = :module")
    suspend fun updateStatus(profileId: Int, module: String, status: String)
}

