package com.stream.nextftv.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.stream.nextftv.data.local.entity.series.SeriesCategoryEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamCategoryRefEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    companion object {
        const val SERIES_STREAM_COLUMNS = """
            s.profileId,
            COALESCE(
                (
                    SELECT MIN(r2.categoryId)
                    FROM series_stream_category_refs r2
                    WHERE r2.profileId = s.profileId
                    AND r2.seriesId = s.seriesId
                ),
                s.categoryId
            ) AS categoryId,
            s.seriesId,
            s.num,
            s.name,
            s.normalizedName,
            s.naturalSortName,
            s.streamType,
            s.streamIcon,
            s.cover,
            s.plot,
            s.cast,
            s.director,
            s.genre,
            s.releaseDate,
            s.lastModified,
            s.rating,
            s.rating5Based,
            s.backdropPath,
            s.backdropUrl,
            s.youtubeTrailer,
            s.episodeRunTime,
            s.tmdbId
        """
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<SeriesCategoryEntity>)

    @Query(
        """
        SELECT *
        FROM series_categories c
        WHERE c.profileId = :profileId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = c.profileId
            AND h.contentType = 'series'
            AND h.categoryId = c.categoryId
        )
        ORDER BY orderIndex ASC
        """
    )
    fun getCategories(profileId: Int): Flow<List<SeriesCategoryEntity>>

    @Query("SELECT * FROM series_categories WHERE profileId = :profileId ORDER BY orderIndex ASC")
    fun getAllCategoriesRaw(profileId: Int): Flow<List<SeriesCategoryEntity>>

    @Query("SELECT * FROM series_categories WHERE profileId = :profileId")
    suspend fun getAllCategoriesSnapshot(profileId: Int): List<SeriesCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<SeriesStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamCategoryRefs(refs: List<SeriesStreamCategoryRefEntity>)

    @Query("SELECT * FROM series_stream_category_refs WHERE profileId = :profileId")
    suspend fun getAllStreamCategoryRefsSnapshot(profileId: Int): List<SeriesStreamCategoryRefEntity>

    @Query(
        """
        SELECT
            s.profileId,
            :categoryId AS categoryId,
            s.seriesId,
            s.num,
            s.name,
            s.normalizedName,
            s.naturalSortName,
            s.streamType,
            s.streamIcon,
            s.cover,
            s.plot,
            s.cast,
            s.director,
            s.genre,
            s.releaseDate,
            s.lastModified,
            s.rating,
            s.rating5Based,
            s.backdropPath,
            s.backdropUrl,
            s.youtubeTrailer,
            s.episodeRunTime,
            s.tmdbId
        FROM series_streams s
        INNER JOIN series_stream_category_refs r
            ON r.profileId = s.profileId
            AND r.seriesId = s.seriesId
        WHERE s.profileId = :profileId
        AND r.categoryId = :categoryId
        AND NOT EXISTS (
            SELECT 1
            FROM parental_hidden_categories h
            WHERE h.profileId = r.profileId
            AND h.contentType = 'series'
            AND h.categoryId = r.categoryId
        )
        ORDER BY s.num ASC
        """
    )
    fun getStreamsByCategory(profileId: Int, categoryId: String): Flow<List<SeriesStreamEntity>>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        ORDER BY s.num ASC
        """
    )
    fun getAllStreams(profileId: Int): Flow<List<SeriesStreamEntity>>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.seriesId = :seriesId
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        LIMIT 1
        """
    )
    suspend fun getSeriesById(profileId: Int, seriesId: Int): SeriesStreamEntity?

    @Query(
        """
        SELECT COUNT(DISTINCT s.seriesId)
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    fun getSeriesCount(profileId: Int): Flow<Int>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.normalizedName LIKE '%' || :query || '%'
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    fun searchStreams(profileId: Int, query: String): Flow<List<SeriesStreamEntity>>

    @Query(
        """
        SELECT
            s.profileId,
            COALESCE(
                CASE
                    WHEN :categoryId IS NOT NULL AND :categoryId != 'all' THEN :categoryId
                    ELSE NULL
                END,
                (
                    SELECT MIN(r2.categoryId)
                    FROM series_stream_category_refs r2
                    WHERE r2.profileId = s.profileId
                    AND r2.seriesId = s.seriesId
                ),
                s.categoryId
            ) AS categoryId,
            s.seriesId,
            s.num,
            s.name,
            s.normalizedName,
            s.naturalSortName,
            s.streamType,
            s.streamIcon,
            s.cover,
            s.plot,
            s.cast,
            s.director,
            s.genre,
            s.releaseDate,
            s.lastModified,
            s.rating,
            s.rating5Based,
            s.backdropPath,
            s.backdropUrl,
            s.youtubeTrailer,
            s.episodeRunTime,
            s.tmdbId
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM series_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.seriesId = s.seriesId
                AND v.categoryId = :categoryId
            )
        )
        AND s.normalizedName LIKE '%' || :query || '%'
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.seriesId
        ORDER BY s.num ASC
        """
    )
    suspend fun searchStreamsSnapshot(profileId: Int, categoryId: String?, query: String): List<SeriesStreamEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT s.seriesId)
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM series_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.seriesId = s.seriesId
                AND v.categoryId = :categoryId
            )
        )
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    suspend fun countVisibleStreams(profileId: Int, categoryId: String?): Int

    @Query(
        """
        SELECT COUNT(DISTINCT s.seriesId)
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.tmdbId IS NOT NULL
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM series_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.seriesId = s.seriesId
                AND v.categoryId = :categoryId
            )
        )
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    suspend fun countVisibleStreamsWithTmdbId(profileId: Int, categoryId: String?): Int

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.tmdbId IN (:tmdbIds)
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM series_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.seriesId = s.seriesId
                AND v.categoryId = :categoryId
            )
        )
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.seriesId
        """
    )
    suspend fun getSeriesByTmdbIds(profileId: Int, categoryId: String?, tmdbIds: List<Int>): List<SeriesStreamEntity>

    @Query(
        """
        SELECT
            s.profileId,
            COALESCE(
                CASE
                    WHEN :categoryId IS NOT NULL AND :categoryId != 'all' THEN :categoryId
                    ELSE NULL
                END,
                (
                    SELECT MIN(r2.categoryId)
                    FROM series_stream_category_refs r2
                    WHERE r2.profileId = s.profileId
                    AND r2.seriesId = s.seriesId
                ),
                s.categoryId
            ) AS categoryId,
            s.seriesId,
            s.num,
            s.name,
            s.normalizedName,
            s.naturalSortName,
            s.streamType,
            s.streamIcon,
            s.cover,
            s.plot,
            s.cast,
            s.director,
            s.genre,
            s.releaseDate,
            s.lastModified,
            s.rating,
            s.rating5Based,
            s.backdropPath,
            s.backdropUrl,
            s.youtubeTrailer,
            s.episodeRunTime,
            s.tmdbId
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND (
            :categoryId IS NULL
            OR :categoryId = 'all'
            OR EXISTS (
                SELECT 1
                FROM series_stream_category_refs v
                WHERE v.profileId = s.profileId
                AND v.seriesId = s.seriesId
                AND v.categoryId = :categoryId
            )
        )
        AND (:query = '' OR s.normalizedName LIKE '%' || :query || '%')
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.seriesId
        ORDER BY
            CASE WHEN :sortOrder = 'A_Z' THEN s.naturalSortName END ASC,
            CASE WHEN :sortOrder = 'Z_A' THEN s.naturalSortName END DESC,
            s.num ASC
        """
    )
    fun getStreamsPaged(
        profileId: Int,
        categoryId: String?,
        query: String = "",
        sortOrder: String = "DEFAULT"
    ): PagingSource<Int, SeriesStreamEntity>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        INNER JOIN recents r ON s.seriesId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'series'
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        ORDER BY r.timestamp DESC
        LIMIT 15
        """
    )
    fun getRecentStreams(profileId: Int): Flow<List<SeriesStreamEntity>>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.seriesId IN (
            SELECT f.streamId
            FROM favorites f
            WHERE f.profileId = :profileId
            AND f.contentType = 'series'
        )
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        """
    )
    fun getFavoriteStreams(profileId: Int): Flow<List<SeriesStreamEntity>>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        INNER JOIN favorites f ON s.seriesId = f.streamId AND s.profileId = f.profileId
        WHERE s.profileId = :profileId
        AND f.contentType = 'series'
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        ORDER BY f.timestamp DESC
        """
    )
    fun getFavoriteStreamsPaged(profileId: Int): PagingSource<Int, SeriesStreamEntity>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        INNER JOIN recents r ON s.seriesId = r.streamId AND s.profileId = r.profileId
        WHERE s.profileId = :profileId
        AND r.contentType = 'series'
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        ORDER BY r.timestamp DESC
        """
    )
    fun getRecentStreamsPaged(profileId: Int): PagingSource<Int, SeriesStreamEntity>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.tmdbId = :tmdbId
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.seriesId
        """
    )
    suspend fun getSeriesListByTmdbId(profileId: Int, tmdbId: String): List<SeriesStreamEntity>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.tmdbId IN (:tmdbIds)
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.seriesId
        """
    )
    suspend fun getSeriesListByTmdbIds(profileId: Int, tmdbIds: List<Int>): List<SeriesStreamEntity>

    @Query(
        """
        SELECT ${SERIES_STREAM_COLUMNS}
        FROM series_streams s
        WHERE s.profileId = :profileId
        AND s.normalizedName = :normalizedName
        AND EXISTS (
            SELECT 1
            FROM series_stream_category_refs v
            WHERE v.profileId = s.profileId
            AND v.seriesId = s.seriesId
            AND NOT EXISTS (
                SELECT 1
                FROM parental_hidden_categories h
                WHERE h.profileId = v.profileId
                AND h.contentType = 'series'
                AND h.categoryId = v.categoryId
            )
        )
        GROUP BY s.profileId, s.seriesId
        """
    )
    suspend fun getSeriesListByName(profileId: Int, normalizedName: String): List<SeriesStreamEntity>

    @Query("DELETE FROM series_categories WHERE profileId = :profileId")
    suspend fun deleteCategoriesByProfile(profileId: Int)

    @Delete
    suspend fun deleteCategories(categories: List<SeriesCategoryEntity>)

    @Query("DELETE FROM series_stream_category_refs WHERE profileId = :profileId")
    suspend fun deleteStreamCategoryRefsByProfile(profileId: Int)

    @Delete
    suspend fun deleteStreamCategoryRefs(refs: List<SeriesStreamCategoryRefEntity>)

    @Query("DELETE FROM series_streams WHERE profileId = :profileId")
    suspend fun deleteStreamsByProfile(profileId: Int)

    @Query(
        """
        DELETE FROM series_streams
        WHERE profileId = :profileId
        AND seriesId NOT IN (
            SELECT DISTINCT seriesId
            FROM series_stream_category_refs
            WHERE profileId = :profileId
        )
        """
    )
    suspend fun deleteOrphanStreams(profileId: Int)

    @Transaction
    suspend fun replaceCategories(profileId: Int, categories: List<SeriesCategoryEntity>) {
        deleteCategoriesByProfile(profileId)
        insertCategories(categories)
    }

    @Transaction
    suspend fun replaceStreams(profileId: Int, streams: List<SeriesStreamEntity>) {
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }

    @Transaction
    suspend fun replaceData(profileId: Int, categories: List<SeriesCategoryEntity>, streams: List<SeriesStreamEntity>) {
        deleteCategoriesByProfile(profileId)
        insertCategories(categories)
        deleteStreamCategoryRefsByProfile(profileId)
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }
}
