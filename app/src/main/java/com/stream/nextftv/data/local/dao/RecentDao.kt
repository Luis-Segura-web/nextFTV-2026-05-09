package com.stream.nextftv.data.local.dao

import androidx.room.*
import com.stream.nextftv.data.local.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentInternal(recent: RecentEntity)

    @Query("DELETE FROM recents WHERE profileId = :profileId AND contentType = :contentType AND streamId NOT IN (SELECT streamId FROM recents WHERE profileId = :profileId AND contentType = :contentType ORDER BY timestamp DESC LIMIT 15)")
    suspend fun trimRecents(profileId: Int, contentType: String)

    @Transaction
    suspend fun insertRecentWithLimit(recent: RecentEntity) {
        insertRecentInternal(recent)
        trimRecents(recent.profileId, recent.contentType)
    }

    @Query("SELECT streamId FROM recents WHERE profileId = :profileId AND contentType = :contentType ORDER BY timestamp DESC LIMIT 15")
    fun getRecentIds(profileId: Int, contentType: String): Flow<List<Int>>

    @Query("DELETE FROM recents WHERE profileId = :profileId")
    suspend fun clearRecents(profileId: Int)

    @Query("""
        DELETE FROM recents
        WHERE profileId = :profileId
        AND contentType = 'live'
        AND streamId NOT IN (
            SELECT streamId FROM live_streams WHERE profileId = :profileId
        )
    """)
    suspend fun clearOrphanLiveRecents(profileId: Int)

    @Query("""
        DELETE FROM recents
        WHERE profileId = :profileId
        AND contentType = 'vod'
        AND streamId NOT IN (
            SELECT streamId FROM vod_streams WHERE profileId = :profileId
        )
    """)
    suspend fun clearOrphanVodRecents(profileId: Int)

    @Query("""
        DELETE FROM recents
        WHERE profileId = :profileId
        AND contentType = 'series'
        AND streamId NOT IN (
            SELECT seriesId FROM series_streams WHERE profileId = :profileId
        )
    """)
    suspend fun clearOrphanSeriesRecents(profileId: Int)
}
