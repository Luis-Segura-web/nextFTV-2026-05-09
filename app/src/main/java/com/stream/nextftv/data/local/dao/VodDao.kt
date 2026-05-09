package com.stream.nextftv.data.local.dao

import androidx.room.*
import com.stream.nextftv.data.local.entity.vod.VodCategoryEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamCategoryRefEntity
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface VodDao {
    companion object {
        const val VOD_STREAM_COLUMNS = """
            s.profileId,
            COALESCE(
                (
                    SELECT MIN(r2.categoryId)
                    FROM vod_stream_category_refs r2
                    WHERE r2.profileId = s.profileId
                    AND r2.streamId = s.streamId
                ),
                s.categoryId
            ) AS categoryId,
            s.streamId,
            s.num,
            s.name,
            s.normalizedName,
            s.naturalSortName,
            s.streamType,
            s.streamIcon,
            s.rating,
            s.rating5Based,
            s.added,
            s.customSid,
            s.containerExtension,
            s.directSource,
            s.backdropPath,
            s.tmdbId,
            s.backdropUrl
        """
    }

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

    @Query("SELECT * FROM vod_categories WHERE profileId = :profileId")
    suspend fun getAllCategoriesSnapshot(profileId: Int): List<VodCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<VodStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamCategoryRefs(refs: List<VodStreamCategoryRefEntity>)

    @Query("SELECT * FROM vod_stream_category_refs WHERE profileId = :profileId")
    suspend fun getAllStreamCategoryRefsSnapshot(profileId: Int): List<VodStreamCategoryRefEntity>

    @Query(
        """
        SELECT
            s.profileId,
            :categoryId AS categoryId,
            s.streamId,
            s.num,
            s.name,
            s.normalizedName,
            s.naturalSortName,
            s.streamType,
            s.streamIcon,
            s.rating,
            s.rating5Based,
            s.added,
            s.customSid,
            s.containerExtension,
            s.directSource,
            s.backdropPath,
            s.tmdbId,
            s.backdropUrl
        FROM vod_streams s
        INNER JOIN vod_stream_category_refs r
            ON r.profileId = s.profileId
            AND r.streamId = s.streamId
        WHERE s.profileId = :profileId
        AND r.categoryId = :categoryId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = r.profileId
            AND h.contentType = 'vod'
            AND h.categoryId = r.categoryId
        )
        ORDER BY s.num ASC
        """
    )
    fun getStreamsByCategory(profileId: Int, categoryId: String): Flow<List<VodStreamEntity>>

    @Query(
        """
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        LEFT JOIN vod_stream_category_refs r
            ON r.profileId = s.profileId
            AND r.streamId = s.streamId
        WHERE s.profileId = :profileId
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        ORDER BY s.num ASC
        """
    )
    fun getAllStreams(profileId: Int): Flow<List<VodStreamEntity>>

    @Query(
        """
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.streamId = :streamId
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        LIMIT 1
        """
    )
    suspend fun getStreamById(profileId: Int, streamId: Int): VodStreamEntity?

    @Query(
        """
        SELECT COUNT(DISTINCT s.streamId)
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    fun getStreamCount(profileId: Int): Flow<Int>

    @Query(
        """
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        LEFT JOIN vod_stream_category_refs r
            ON r.profileId = s.profileId
            AND r.streamId = s.streamId
        WHERE s.profileId = :profileId
        AND s.normalizedName LIKE '%' || :query || '%'
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        """
    )
    fun searchStreams(profileId: Int, query: String): Flow<List<VodStreamEntity>>

    @Query(
        """
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM vod_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.streamId = s.streamId
                AND v.categoryId = :categoryId
            )
        )
        AND s.normalizedName LIKE '%' || :query || '%'
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        ORDER BY s.num ASC
        """
    )
    suspend fun searchStreamsSnapshot(profileId: Int, categoryId: String?, query: String): List<VodStreamEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT s.streamId)
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM vod_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.streamId = s.streamId
                AND v.categoryId = :categoryId
            )
        )
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    suspend fun countVisibleStreams(profileId: Int, categoryId: String?): Int

    @Query(
        """
        SELECT COUNT(DISTINCT s.streamId)
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.tmdbId IS NOT NULL
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM vod_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.streamId = s.streamId
                AND v.categoryId = :categoryId
            )
        )
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    suspend fun countVisibleStreamsWithTmdbId(profileId: Int, categoryId: String?): Int

    @Query(
        """
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.tmdbId IN (:tmdbIds)
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM vod_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.streamId = s.streamId
                AND v.categoryId = :categoryId
            )
        )
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        """
    )
    suspend fun getStreamsByTmdbIds(profileId: Int, categoryId: String?, tmdbIds: List<Int>): List<VodStreamEntity>

    @Query("""
        SELECT
            s.profileId,
            COALESCE(
                CASE
                    WHEN :categoryId IS NOT NULL AND :categoryId != 'all' THEN :categoryId
                    ELSE NULL
                END,
                (
                    SELECT MIN(r2.categoryId)
                    FROM vod_stream_category_refs r2
                    WHERE r2.profileId = s.profileId
                    AND r2.streamId = s.streamId
                ),
                s.categoryId
            ) AS categoryId,
            s.streamId,
            s.num,
            s.name,
            s.normalizedName,
            s.naturalSortName,
            s.streamType,
            s.streamIcon,
            s.rating,
            s.rating5Based,
            s.added,
            s.customSid,
            s.containerExtension,
            s.directSource,
            s.backdropPath,
            s.tmdbId,
            s.backdropUrl
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM vod_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.streamId = s.streamId
                AND v.categoryId = :categoryId
            )
        )
        AND (:query = '' OR s.normalizedName LIKE '%' || :query || '%')
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
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
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'vod'
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        ORDER BY r.timestamp DESC
        LIMIT 15
        """
    )
    fun getRecentStreams(profileId: Int): Flow<List<VodStreamEntity>>

    @Query("""
        SELECT ${VOD_STREAM_COLUMNS} FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.streamId IN (SELECT f.streamId FROM favorites f WHERE f.profileId = :profileId AND f.contentType = 'vod')
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
    """)
    fun getFavoriteStreams(profileId: Int): Flow<List<VodStreamEntity>>

    @Query(
        """
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        INNER JOIN favorites f ON s.streamId = f.streamId AND s.profileId = f.profileId
        WHERE s.profileId = :profileId
        AND f.contentType = 'vod'
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        ORDER BY f.timestamp DESC
        """
    )
    fun getFavoriteStreamsPaged(profileId: Int): PagingSource<Int, VodStreamEntity>

    @Query(
        """
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        INNER JOIN recents r ON s.streamId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'vod'
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
        ORDER BY r.timestamp DESC
        """
    )
    fun getRecentStreamsPaged(profileId: Int): PagingSource<Int, VodStreamEntity>

    @Query("""
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.tmdbId = :tmdbId
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
    """)
    suspend fun getStreamsByTmdbId(profileId: Int, tmdbId: String): List<VodStreamEntity>

    @Query("""
        SELECT ${VOD_STREAM_COLUMNS}
        FROM vod_streams s
        WHERE s.profileId = :profileId
        AND s.normalizedName = :normalizedName
        AND EXISTS (
            SELECT 1
            FROM vod_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.streamId = s.streamId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'vod'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.streamId
    """)
    suspend fun getStreamsByName(profileId: Int, normalizedName: String): List<VodStreamEntity>

    @Query("DELETE FROM vod_categories WHERE profileId = :profileId")
    suspend fun deleteCategoriesByProfile(profileId: Int)

    @Delete
    suspend fun deleteCategories(categories: List<VodCategoryEntity>)

    @Query("DELETE FROM vod_stream_category_refs WHERE profileId = :profileId")
    suspend fun deleteStreamCategoryRefsByProfile(profileId: Int)

    @Delete
    suspend fun deleteStreamCategoryRefs(refs: List<VodStreamCategoryRefEntity>)

    @Query("DELETE FROM vod_streams WHERE profileId = :profileId")
    suspend fun deleteStreamsByProfile(profileId: Int)

    @Query(
        """
        DELETE FROM vod_streams
        WHERE profileId = :profileId
        AND streamId NOT IN (
            SELECT DISTINCT streamId
            FROM vod_stream_category_refs
            WHERE profileId = :profileId
        )
        """
    )
    suspend fun deleteOrphanStreams(profileId: Int)

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
        deleteStreamCategoryRefsByProfile(profileId)
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }
}
