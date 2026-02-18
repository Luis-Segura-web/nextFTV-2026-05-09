package com.stream.iptvrevolut.data.local.dao

import androidx.room.*
import com.stream.iptvrevolut.data.local.entity.RecentEntity
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
}
