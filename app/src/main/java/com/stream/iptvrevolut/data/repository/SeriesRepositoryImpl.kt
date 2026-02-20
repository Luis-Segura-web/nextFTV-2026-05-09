package com.stream.iptvrevolut.data.repository
import com.stream.iptvrevolut.data.local.dao.FavoriteDao
import com.stream.iptvrevolut.data.local.dao.RecentDao
import com.stream.iptvrevolut.data.local.dao.SeriesDao
import com.stream.iptvrevolut.data.local.entity.FavoriteEntity
import com.stream.iptvrevolut.data.local.entity.RecentEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesCategoryEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import com.stream.iptvrevolut.data.remote.XtreamApiService
import com.stream.iptvrevolut.data.remote.SeriesEpisodeDto
import com.stream.iptvrevolut.data.remote.tmdb.TmdbApiService
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.iptvrevolut.data.utils.StringUtils
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.SeriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.google.gson.Gson
import com.stream.iptvrevolut.data.local.dao.DetailCacheDao
import com.stream.iptvrevolut.data.local.entity.cache.DetailCacheEntity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import android.util.Log
class SeriesRepositoryImpl @Inject constructor(
    private val xtreamApi: XtreamApiService,
    private val tmdbApi: TmdbApiService,
    private val profileRepository: ProfileRepository,
    private val seriesDao: SeriesDao,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao,
    private val detailCacheDao: DetailCacheDao,
    private val gson: Gson
) : SeriesRepository {
    override suspend fun getSeriesEpisodes(
        profile: ServerProfile,
        seriesId: Int
    ): Result<Map<String, List<SeriesEpisodeDto>>> {
        val cacheKey = "episodes_$seriesId"
        val now = System.currentTimeMillis()
        val cached = detailCacheDao.getDetail(cacheKey)
        if (cached != null && (now - cached.lastUpdated < 12 * 60 * 60 * 1000L)) {
            return try {
                val type = object : com.google.gson.reflect.TypeToken<Map<String, List<SeriesEpisodeDto>>>() {}.type
                val info = gson.fromJson<Map<String, List<SeriesEpisodeDto>>>(cached.dataJson, type)
                Result.success(info)
            } catch (e: Exception) { Result.failure(e) }
        }
        return try {
            val response = xtreamApi.getSeriesInfo(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password,
                seriesId = seriesId
            )
            val newBackdropList = response.info?.backdropPath
            val newTmdbId = response.info?.tmdbId
            if (!newBackdropList.isNullOrEmpty() || newTmdbId != null) {
                try {
                    seriesDao.getAllStreams(profile.id).first().find { it.seriesId == seriesId }?.let { series ->
                        val updatedSeries = series.copy(
                            tmdbId = series.tmdbId ?: newTmdbId,
                            backdropPath = series.backdropPath ?: newBackdropList
                        )
                        seriesDao.insertStreams(listOf(updatedSeries))
                    }
                } catch (e: Exception) { Log.e("SeriesRepo", "Error updating info: ${e.message}") }
            }
            val episodes = response.episodes ?: emptyMap()
            detailCacheDao.insertDetail(DetailCacheEntity(cacheKey, "series_episodes", gson.toJson(episodes), now))
            Result.success(episodes)
        } catch (e: Exception) { Result.failure(e) }
    }
    override fun getCategories(profileId: Int): Flow<List<SeriesCategoryEntity>> = seriesDao.getCategories(profileId)
    override fun getSeries(profileId: Int, categoryId: String?): Flow<List<SeriesStreamEntity>> {
        return when (categoryId) {
            "favorites" -> getFavorites(profileId, "series")
            "recents" -> getRecents(profileId, "series")
            null, "all" -> seriesDao.getAllStreams(profileId)
            else -> seriesDao.getStreamsByCategory(profileId, categoryId)
        }
    }
    override fun getPagedSeries(
        profileId: Int,
        categoryId: String?,
        query: String,
        sortOrder: String
    ): Flow<PagingData<SeriesStreamEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 50, prefetchDistance = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                when (categoryId) {
                    "favorites" -> seriesDao.getFavoriteStreamsPaged(profileId)
                    "recents" -> seriesDao.getRecentStreamsPaged(profileId)
                    else -> seriesDao.getStreamsPaged(profileId, categoryId, StringUtils.normalize(query), sortOrder)
                }
            }
        ).flow
    }
    override fun searchSeries(profileId: Int, query: String): Flow<List<SeriesStreamEntity>> =
        seriesDao.searchStreams(profileId, StringUtils.normalize(query))
    override suspend fun getSeriesDetails(series: SeriesStreamEntity): Result<TmdbMovieDetailsDto> {
        return try {
            if (!profileRepository.isTmdbEnabled()) return Result.failure(Exception("Disabled"))
            series.tmdbId?.let { id -> return Result.success(tmdbApi.getTvDetails(id)) }
            val cleanName = cleanSeriesName(series.name)
            val searchResponse = tmdbApi.searchTv(cleanName)
            val tmdbId = searchResponse.results.firstOrNull()?.id ?: return Result.failure(Exception("Not found"))
            Result.success(tmdbApi.getTvDetails(tmdbId))
        } catch (e: Exception) { Result.failure(e) }
    }
    override suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String) {
        val isFav = favoriteDao.isFavorite(profileId, streamId, contentType).first()
        if (isFav) {
            favoriteDao.deleteFavorite(FavoriteEntity(profileId, streamId, contentType))
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(profileId, streamId, contentType, System.currentTimeMillis()))
        }
    }
    override fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean> = favoriteDao.isFavorite(profileId, streamId, contentType)
    override suspend fun getSeriesListByTmdbId(profileId: Int, tmdbId: String): List<SeriesStreamEntity> = seriesDao.getSeriesListByTmdbId(profileId, tmdbId)
    override suspend fun getSeriesListByName(profileId: Int, normalizedName: String): List<SeriesStreamEntity> = seriesDao.getSeriesListByName(profileId, normalizedName)
    override suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String) {
        recentDao.insertRecentWithLimit(RecentEntity(profileId, streamId, contentType, System.currentTimeMillis()))
    }
    private fun getRecents(profileId: Int, contentType: String): Flow<List<SeriesStreamEntity>> = seriesDao.getRecentStreams(profileId)
    private fun getFavorites(profileId: Int, contentType: String): Flow<List<SeriesStreamEntity>> = seriesDao.getFavoriteStreams(profileId)
    private fun cleanSeriesName(name: String): String {
        var clean = name
        clean = clean.replace(Regex("(?i)\\((esp|lat|en|multi|dub|sub|esp-lat)\\)"), "")
        clean = clean.replace(Regex("(?i)\\[(esp|lat|en|multi|dub|sub)\\]"), "")
        clean = clean.replace(Regex("\\(\\d{4}\\)"), "")
        clean = clean.replace(Regex("(?i)\\b(S\\d+|E\\d+|COMPLETE|SEASON|EPISODE|4K|UHD|HD|1080P|720P)\\b"), "")
        clean = clean.replace(Regex("[\\[\\](){}]"), "")
        clean = clean.replace(".", " ").replace("_", " ")
        return clean.trim().replace(Regex("\\s+"), " ")
    }
}
