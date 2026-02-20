package com.stream.iptvrevolut.data.local.dao

import androidx.room.*
import com.stream.iptvrevolut.data.local.entity.vod.VodCategoryEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodStreamEntity
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface VodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<VodCategoryEntity>)

    @Query(
        """
        SELECT *
        FROM vod_categories c
        WHERE c.profileId = :profileId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = c.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = c.categoryId
        )
        ORDER BY orderIndex ASC
        """
    )
    fun getCategories(profileId: Int): Flow<List<VodCategoryEntity>>

    @Query("SELECT * FROM vod_categories WHERE profileId = :profileId ORDER BY orderIndex ASC")
    fun getAllCategoriesRaw(profileId: Int): Flow<List<VodCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<VodStreamEntity>)

    @Query(
        """
        SELECT *
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.categoryId = :categoryId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        ORDER BY s.num ASC
        """
    )
    fun getStreamsByCategory(profileId: Int, categoryId: String): Flow<List<VodStreamEntity>>

    @Query(
        """
        SELECT *
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        GROUP BY s.streamId
        ORDER BY s.num ASC
        """
    )
    fun getAllStreams(profileId: Int): Flow<List<VodStreamEntity>>

    @Query(
        """
        SELECT *
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.normalizedName LIKE '%' || :query || '%'
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        """
    )
    fun searchStreams(profileId: Int, query: String): Flow<List<VodStreamEntity>>

    @Query("""
        SELECT * FROM vod_streams s
        WHERE s.profileId = :profileId
        AND (:categoryId IS NULL OR :categoryId = 'all' OR s.categoryId = :categoryId)
        AND (:query = '' OR s.normalizedName LIKE '%' || :query || '%')
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        GROUP BY s.streamId
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
    ): PagingSource<Int, VodStreamEntity>

    @Query(
        """
        SELECT s.*
        FROM vod_streams s
        INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'vod'
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        GROUP BY s.streamId
        ORDER BY r.timestamp DESC
        LIMIT 15
        """
    )
    fun getRecentStreams(profileId: Int): Flow<List<VodStreamEntity>>

    @Query("""
        SELECT s.* FROM vod_streams s 
        WHERE s.profileId = :profileId
        AND s.streamId IN (SELECT f.streamId FROM favorites f WHERE f.profileId = :profileId AND f.contentType = 'vod')
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        GROUP BY s.streamId
    """)
    fun getFavoriteStreams(profileId: Int): Flow<List<VodStreamEntity>>

    @Query(
        """
        SELECT s.*
        FROM vod_streams s
        INNER JOIN favorites f ON s.streamId = f.streamId AND s.profileId = f.profileId
        WHERE s.profileId = :profileId
        AND f.contentType = 'vod'
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        GROUP BY s.streamId
        ORDER BY f.timestamp DESC
        """
    )
    fun getFavoriteStreamsPaged(profileId: Int): PagingSource<Int, VodStreamEntity>

    @Query(
        """
        SELECT s.*
        FROM vod_streams s
        INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'vod'
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = s.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = s.categoryId
        )
        GROUP BY s.streamId
        ORDER BY r.timestamp DESC
        """
    )
    fun getRecentStreamsPaged(profileId: Int): PagingSource<Int, VodStreamEntity>

    @Query("SELECT s.* FROM vod_streams s WHERE s.profileId = :profileId AND s.tmdbId = :tmdbId")
    suspend fun getStreamsByTmdbId(profileId: Int, tmdbId: String): List<VodStreamEntity>

    @Query("SELECT s.* FROM vod_streams s WHERE s.profileId = :profileId AND s.normalizedName = :normalizedName")
    suspend fun getStreamsByName(profileId: Int, normalizedName: String): List<VodStreamEntity>

    @Query("DELETE FROM vod_categories WHERE profileId = :profileId")
    suspend fun deleteCategoriesByProfile(profileId: Int)

    @Query("DELETE FROM vod_streams WHERE profileId = :profileId")
    suspend fun deleteStreamsByProfile(profileId: Int)

    @Transaction
    suspend fun replaceCategories(profileId: Int, categories: List<VodCategoryEntity>) {
        deleteCategoriesByProfile(profileId)
        insertCategories(categories)
    }

    @Transaction
    suspend fun replaceStreams(profileId: Int, streams: List<VodStreamEntity>) {
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }

    @Transaction
    suspend fun replaceData(profileId: Int, categories: List<VodCategoryEntity>, streams: List<VodStreamEntity>) {
        deleteCategoriesByProfile(profileId)
        insertCategories(categories)
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }
}
