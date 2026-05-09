package com.stream.nextftv.data.local.dao

import androidx.room.*
import com.stream.nextftv.data.local.entity.live.LiveCategoryEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamCategoryRefEntity
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface LiveTvDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<LiveCategoryEntity>)

    @Query(
        """
        SELECT *
        FROM live_categories c
        WHERE c.profileId = :profileId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = c.profileId
            AND h.contentType = 'live'
            AND h.categoryId = c.categoryId
        )
        ORDER BY orderIndex ASC
        """
    )
    fun getCategories(profileId: Int): Flow<List<LiveCategoryEntity>>

    @Query("SELECT * FROM live_categories WHERE profileId = :profileId ORDER BY orderIndex ASC")
    fun getAllCategoriesRaw(profileId: Int): Flow<List<LiveCategoryEntity>>

    @Query("SELECT * FROM live_categories WHERE profileId = :profileId")
    suspend fun getAllCategoriesSnapshot(profileId: Int): List<LiveCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<LiveStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamCategoryRefs(refs: List<LiveStreamCategoryRefEntity>)

    @Query("SELECT * FROM live_stream_category_refs WHERE profileId = :profileId")
    suspend fun getAllStreamCategoryRefsSnapshot(profileId: Int): List<LiveStreamCategoryRefEntity>

    @Query(
        """
        SELECT s.*
        FROM live_streams s
        INNER JOIN live_stream_category_refs r
            ON r.profileId = s.profileId
            AND r.streamId = s.streamId
        WHERE s.profileId = :profileId
        AND r.categoryId = :categoryId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = r.profileId
            AND h.contentType = 'live'
            AND h.categoryId = r.categoryId
        )
        ORDER BY s.num ASC
        """
    )
    fun getStreamsByCategory(profileId: Int, categoryId: String): Flow<List<LiveStreamEntity>>

    @Query(
        """
        SELECT *
        FROM live_streams s
        WHERE s.profileId = :profileId
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs r
            WHERE r.profileId = s.profileId
            AND r.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = r.profileId
                AND h.contentType = 'live'
                AND h.categoryId = r.categoryId
            )
        )
        ORDER BY s.num ASC
        """
    )
    fun getAllStreams(profileId: Int): Flow<List<LiveStreamEntity>>

    @Query(
        """
        SELECT COUNT(DISTINCT s.streamId)
        FROM live_streams s
        WHERE s.profileId = :profileId
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs r
            WHERE r.profileId = s.profileId
            AND r.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = r.profileId
                AND h.contentType = 'live'
                AND h.categoryId = r.categoryId
            )
        )
        """
    )
    fun getStreamCount(profileId: Int): Flow<Int>

    @Query(
        """
        SELECT *
        FROM live_streams s
        WHERE s.profileId = :profileId
        AND s.normalizedName LIKE '%' || :query || '%'
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs r
            WHERE r.profileId = s.profileId
            AND r.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = r.profileId
                AND h.contentType = 'live'
                AND h.categoryId = r.categoryId
            )
        )
        """
    )
    fun searchStreams(profileId: Int, query: String): Flow<List<LiveStreamEntity>>

    @Query("""
        SELECT s.* FROM live_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM live_stream_category_refs r
                WHERE r.profileId = s.profileId
                AND r.streamId = s.streamId
                AND r.categoryId = :categoryId
            )
        )
        AND (:query = '' OR s.normalizedName LIKE '%' || :query || '%')
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs r
            WHERE r.profileId = s.profileId
            AND r.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = r.profileId
                AND h.contentType = 'live'
                AND h.categoryId = r.categoryId
            )
        )
        ORDER BY 
            CASE WHEN :sortOrder = 'A_Z' THEN s.naturalSortName END ASC,
            CASE WHEN :sortOrder = 'Z_A' THEN s.naturalSortName END DESC,
            s.num ASC
    """)
    fun getStreamsPaged(
        profileId: Int, 
        categoryId: String?, 
        query: String = "", 
        sortOrder: String = "DEFAULT"
    ): PagingSource<Int, LiveStreamEntity>

    @Query("""
        SELECT s.* FROM live_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM live_stream_category_refs r
                WHERE r.profileId = s.profileId
                AND r.streamId = s.streamId
                AND r.categoryId = :categoryId
            )
        )
        AND (:query = '' OR s.normalizedName LIKE '%' || :query || '%')
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs r
            WHERE r.profileId = s.profileId
            AND r.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = r.profileId
                AND h.contentType = 'live'
                AND h.categoryId = r.categoryId
            )
        )
        ORDER BY 
            CASE WHEN :sortOrder = 'A_Z' THEN s.naturalSortName END ASC,
            CASE WHEN :sortOrder = 'Z_A' THEN s.naturalSortName END DESC,
            s.num ASC
    """)
    suspend fun getStreamsFiltered(
        profileId: Int, 
        categoryId: String?, 
        query: String = "", 
        sortOrder: String = "DEFAULT"
    ): List<LiveStreamEntity>

    @Query(
        """
        SELECT s.*
        FROM live_streams s
        INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'live'
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs c
            WHERE c.profileId = s.profileId
            AND c.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = c.profileId
                AND h.contentType = 'live'
                AND h.categoryId = c.categoryId
            )
        )
        ORDER BY r.timestamp DESC
        LIMIT 15
        """
    )
    fun getRecentStreams(profileId: Int): Flow<List<LiveStreamEntity>>

    @Query("""
        SELECT s.* FROM live_streams s 
        WHERE s.profileId = :profileId
        AND s.streamId IN (SELECT f.streamId FROM favorites f WHERE f.profileId = :profileId AND f.contentType = 'live')
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs r
            WHERE r.profileId = s.profileId
            AND r.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = r.profileId
                AND h.contentType = 'live'
                AND h.categoryId = r.categoryId
            )
        )
    """)
    fun getFavoriteStreams(profileId: Int): Flow<List<LiveStreamEntity>>

    @Query(
        """
        SELECT s.*
        FROM live_streams s
        INNER JOIN favorites f ON s.streamId = f.streamId AND s.profileId = f.profileId
        WHERE s.profileId = :profileId
        AND f.contentType = 'live'
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs r
            WHERE r.profileId = s.profileId
            AND r.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = r.profileId
                AND h.contentType = 'live'
                AND h.categoryId = r.categoryId
            )
        )
        ORDER BY f.timestamp DESC
        """
    )
    fun getFavoriteStreamsPaged(profileId: Int): PagingSource<Int, LiveStreamEntity>

    @Query(
        """
        SELECT s.*
        FROM live_streams s
        INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'live'
        AND EXISTS (
            SELECT 1
            FROM live_stream_category_refs c
            WHERE c.profileId = s.profileId
            AND c.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = c.profileId
                AND h.contentType = 'live'
                AND h.categoryId = c.categoryId
            )
        )
        ORDER BY r.timestamp DESC
        """
    )
    fun getRecentStreamsPaged(profileId: Int): PagingSource<Int, LiveStreamEntity>

    @Query("DELETE FROM live_categories WHERE profileId = :profileId")
    suspend fun deleteCategoriesByProfile(profileId: Int)

    @Delete
    suspend fun deleteCategories(categories: List<LiveCategoryEntity>)

    @Query("DELETE FROM live_stream_category_refs WHERE profileId = :profileId")
    suspend fun deleteStreamCategoryRefsByProfile(profileId: Int)

    @Delete
    suspend fun deleteStreamCategoryRefs(refs: List<LiveStreamCategoryRefEntity>)

    @Query("DELETE FROM live_streams WHERE profileId = :profileId")
    suspend fun deleteStreamsByProfile(profileId: Int)

    @Query(
        """
        DELETE FROM live_streams
        WHERE profileId = :profileId
        AND streamId NOT IN (
            SELECT DISTINCT streamId
            FROM live_stream_category_refs
            WHERE profileId = :profileId
        )
        """
    )
    suspend fun deleteOrphanStreams(profileId: Int)

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
        deleteStreamCategoryRefsByProfile(profileId)
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }
}
