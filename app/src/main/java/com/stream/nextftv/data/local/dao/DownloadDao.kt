package com.stream.nextftv.data.local.dao

import androidx.room.*
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getDownloadsByProfile(profileId: Int): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("UPDATE downloads SET progress = :progress, downloadedSize = :downloaded, downloadSpeed = :speed WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, downloaded: Long, speed: Long)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE downloads SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: String)

    @Query("UPDATE downloads SET retryCount = 0 WHERE id = :id")
    suspend fun resetRetryCount(id: String)

    @Query("SELECT * FROM downloads WHERE status = 'queued' OR status = 'retrying' ORDER BY timestamp ASC LIMIT 1")
    suspend fun getNextQueuedDownload(): DownloadEntity?
}
