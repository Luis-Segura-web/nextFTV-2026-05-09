package com.stream.iptvrevolut.data.local.dao

import androidx.room.*
import com.stream.iptvrevolut.data.local.entity.live.LiveCategoryEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveStreamEntity
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface LiveTvDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<LiveCategoryEntity>)

    @Query("SELECT * FROM live_categories WHERE profileId = :profileId")
    fun getCategories(profileId: Int): Flow<List<LiveCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<LiveStreamEntity>)

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND categoryId = :categoryId ORDER BY num ASC")
    fun getStreamsByCategory(profileId: Int, categoryId: String): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId GROUP BY streamId ORDER BY num ASC")
    fun getAllStreams(profileId: Int): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE profileId = :profileId AND normalizedName LIKE '%' || :query || '%'")
    fun searchStreams(profileId: Int, query: String): Flow<List<LiveStreamEntity>>

    @Query("""
        SELECT * FROM live_streams 
        WHERE profileId = :profileId 
        AND (:categoryId IS NULL OR :categoryId = 'all' OR categoryId = :categoryId)
        AND (:query = '' OR normalizedName LIKE '%' || :query || '%')
        GROUP BY streamId 
        ORDER BY 
            CASE WHEN :sortOrder = 'A_Z' THEN naturalSortName END ASC,
            CASE WHEN :sortOrder = 'Z_A' THEN naturalSortName END DESC,
            num ASC
    """)
    fun getStreamsPaged(
        profileId: Int, 
        categoryId: String?, 
        query: String = "", 
        sortOrder: String = "DEFAULT"
    ): PagingSource<Int, LiveStreamEntity>

    @Query("""
        SELECT * FROM live_streams 
        WHERE profileId = :profileId 
        AND (:categoryId IS NULL OR :categoryId = 'all' OR categoryId = :categoryId)
        AND (:query = '' OR normalizedName LIKE '%' || :query || '%')
        GROUP BY streamId 
        ORDER BY 
            CASE WHEN :sortOrder = 'A_Z' THEN naturalSortName END ASC,
            CASE WHEN :sortOrder = 'Z_A' THEN naturalSortName END DESC,
            num ASC
    """)
    suspend fun getStreamsFiltered(
        profileId: Int, 
        categoryId: String?, 
        query: String = "", 
        sortOrder: String = "DEFAULT"
    ): List<LiveStreamEntity>

    @Query("SELECT s.* FROM live_streams s INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId WHERE s.profileId = :profileId AND r.contentType = 'live' GROUP BY s.streamId ORDER BY r.timestamp DESC LIMIT 15")
    fun getRecentStreams(profileId: Int): Flow<List<LiveStreamEntity>>

    @Query("""
        SELECT s.* FROM live_streams s 
        WHERE s.profileId = :profileId 
        AND s.streamId IN (SELECT f.streamId FROM favorites f WHERE f.profileId = :profileId AND f.contentType = 'live')
        GROUP BY s.streamId
    """)
    fun getFavoriteStreams(profileId: Int): Flow<List<LiveStreamEntity>>

    @Query("SELECT s.* FROM live_streams s INNER JOIN favorites f ON s.streamId = f.streamId AND s.profileId = f.profileId WHERE s.profileId = :profileId AND f.contentType = 'live' GROUP BY s.streamId ORDER BY f.timestamp DESC")
    fun getFavoriteStreamsPaged(profileId: Int): PagingSource<Int, LiveStreamEntity>

    @Query("SELECT s.* FROM live_streams s INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId WHERE s.profileId = :profileId AND r.contentType = 'live' GROUP BY s.streamId ORDER BY r.timestamp DESC")
    fun getRecentStreamsPaged(profileId: Int): PagingSource<Int, LiveStreamEntity>

    @Query("DELETE FROM live_categories WHERE profileId = :profileId")
    suspend fun deleteCategoriesByProfile(profileId: Int)

    @Query("DELETE FROM live_streams WHERE profileId = :profileId")
    suspend fun deleteStreamsByProfile(profileId: Int)

    @Transaction
    suspend fun replaceCategories(profileId: Int, categories: List<LiveCategoryEntity>) {
        deleteCategoriesByProfile(profileId)
        insertCategories(categories)
    }

    @Transaction
    suspend fun replaceStreams(profileId: Int, streams: List<LiveStreamEntity>) {
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }

    @Transaction
    suspend fun replaceData(profileId: Int, categories: List<LiveCategoryEntity>, streams: List<LiveStreamEntity>) {
        deleteCategoriesByProfile(profileId)
        insertCategories(categories)
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }
}
