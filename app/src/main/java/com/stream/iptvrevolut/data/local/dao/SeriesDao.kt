package com.stream.iptvrevolut.data.local.dao

import androidx.room.*
import com.stream.iptvrevolut.data.local.entity.series.SeriesCategoryEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface SeriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<SeriesCategoryEntity>)

    @Query("SELECT * FROM series_categories WHERE profileId = :profileId")
    fun getCategories(profileId: Int): Flow<List<SeriesCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<SeriesStreamEntity>)

    @Query("SELECT * FROM series_streams WHERE profileId = :profileId AND categoryId = :categoryId ORDER BY num ASC")
    fun getStreamsByCategory(profileId: Int, categoryId: String): Flow<List<SeriesStreamEntity>>

    @Query("SELECT * FROM series_streams WHERE profileId = :profileId GROUP BY seriesId ORDER BY num ASC")
    fun getAllStreams(profileId: Int): Flow<List<SeriesStreamEntity>>

    @Query("SELECT * FROM series_streams WHERE profileId = :profileId AND normalizedName LIKE '%' || :query || '%'")
    fun searchStreams(profileId: Int, query: String): Flow<List<SeriesStreamEntity>>

    @Query("""
        SELECT * FROM series_streams 
        WHERE profileId = :profileId 
        AND (:categoryId IS NULL OR :categoryId = 'all' OR categoryId = :categoryId)
        AND (:query = '' OR normalizedName LIKE '%' || :query || '%')
        GROUP BY seriesId 
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
    ): PagingSource<Int, SeriesStreamEntity>

    @Query("SELECT s.* FROM series_streams s INNER JOIN recents r ON s.seriesId = r.streamId AND s.profileId = r.profileId WHERE s.profileId = :profileId AND r.contentType = 'series' GROUP BY s.seriesId ORDER BY r.timestamp DESC LIMIT 15")
    fun getRecentStreams(profileId: Int): Flow<List<SeriesStreamEntity>>

    @Query("""
        SELECT s.* FROM series_streams s 
        WHERE s.profileId = :profileId 
        AND s.seriesId IN (SELECT f.streamId FROM favorites f WHERE f.profileId = :profileId AND f.contentType = 'series')
        GROUP BY s.seriesId
    """)
    fun getFavoriteStreams(profileId: Int): Flow<List<SeriesStreamEntity>>

    @Query("SELECT s.* FROM series_streams s INNER JOIN favorites f ON s.seriesId = f.streamId AND s.profileId = f.profileId WHERE s.profileId = :profileId AND f.contentType = 'series' GROUP BY s.seriesId ORDER BY f.timestamp DESC")
    fun getFavoriteStreamsPaged(profileId: Int): PagingSource<Int, SeriesStreamEntity>

    @Query("SELECT s.* FROM series_streams s INNER JOIN recents r ON s.seriesId = r.streamId AND s.profileId = r.profileId WHERE s.profileId = :profileId AND r.contentType = 'series' GROUP BY s.seriesId ORDER BY r.timestamp DESC")
    fun getRecentStreamsPaged(profileId: Int): PagingSource<Int, SeriesStreamEntity>

    @Query("SELECT s.* FROM series_streams s WHERE s.profileId = :profileId AND s.tmdbId = :tmdbId")
    suspend fun getSeriesListByTmdbId(profileId: Int, tmdbId: String): List<SeriesStreamEntity>

    @Query("SELECT s.* FROM series_streams s WHERE s.profileId = :profileId AND s.normalizedName = :normalizedName")
    suspend fun getSeriesListByName(profileId: Int, normalizedName: String): List<SeriesStreamEntity>

    @Query("DELETE FROM series_categories WHERE profileId = :profileId")
    suspend fun deleteCategoriesByProfile(profileId: Int)

    @Query("DELETE FROM series_streams WHERE profileId = :profileId")
    suspend fun deleteStreamsByProfile(profileId: Int)

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
        deleteStreamsByProfile(profileId)
        insertStreams(streams)
    }
}
