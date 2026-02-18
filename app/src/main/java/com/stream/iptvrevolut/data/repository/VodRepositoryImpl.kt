package com.stream.iptvrevolut.data.repository

import com.stream.iptvrevolut.data.local.dao.FavoriteDao
import com.stream.iptvrevolut.data.local.dao.RecentDao
import com.stream.iptvrevolut.data.local.dao.VodDao
import com.stream.iptvrevolut.data.local.entity.FavoriteEntity
import com.stream.iptvrevolut.data.local.entity.RecentEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodCategoryEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodStreamEntity
import com.stream.iptvrevolut.data.remote.XtreamApiService
import com.stream.iptvrevolut.data.remote.tmdb.TmdbApiService
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.iptvrevolut.data.remote.tmdb.TmdbCollectionDto
import com.stream.iptvrevolut.data.utils.StringUtils
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.VodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.google.gson.Gson
import com.stream.iptvrevolut.data.local.dao.DetailCacheDao
import com.stream.iptvrevolut.data.local.entity.cache.DetailCacheEntity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VodRepositoryImpl @Inject constructor(
    private val xtreamApi: XtreamApiService,
    private val tmdbApi: TmdbApiService,
    private val profileRepository: ProfileRepository,
    private val vodDao: VodDao,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao,
    private val detailCacheDao: DetailCacheDao,
    private val gson: Gson
) : VodRepository {

    override suspend fun getVodInfo(
        profile: ServerProfile,
        streamId: Int
    ): Result<com.stream.iptvrevolut.data.remote.VodInfoDto> {
        val cacheKey = "vod_$streamId"
        val now = System.currentTimeMillis()
        val cached = detailCacheDao.getDetail(cacheKey)
        if (cached != null && (now - cached.lastUpdated < 24 * 60 * 60 * 1000L)) {
            return try {
                val info = gson.fromJson(cached.dataJson, com.stream.iptvrevolut.data.remote.VodInfoDto::class.java)
                Result.success(info)
            } catch (e: Exception) { Result.failure(e) }
        }

        return try {
            val response = xtreamApi.getVodInfo(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password,
                vodId = streamId
            )
            
            // Lógica Oportunista: Si la info trae imagen, guardarla como backdrop si no teníamos uno
            response.info?.movieImage?.let { img ->
                vodDao.getAllStreams(profile.id).first().find { it.streamId == streamId }?.let { stream ->
                    if (stream.backdropUrl == null) {
                        vodDao.insertStreams(listOf(stream.copy(backdropUrl = img)))
                    }
                }
            }

            response.info?.tmdbId?.let { id ->
                try {
                    vodDao.getAllStreams(profile.id).first().find { it.streamId == streamId }?.let { stream ->
                        if (stream.tmdbId == null) {
                            vodDao.insertStreams(listOf(stream.copy(tmdbId = id)))
                        }
                    }
                } catch (e: Exception) { Log.e("VodRepo", "Error updating tmdbId: ${e.message}") }
            }

            detailCacheDao.insertDetail(DetailCacheEntity(cacheKey, "vod", gson.toJson(response), now))
            Result.success(response)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun syncCategories(profile: ServerProfile): Result<Unit> {
        return try {
            val response = xtreamApi.getVodCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
            vodDao.replaceCategories(profile.id, response.map { VodCategoryEntity(profile.id, it.categoryId, it.categoryName, it.parentId ?: 0) })
            Result.success(Unit)
        } catch (e: Exception) { 
            Log.e("VodRepo", "SyncCategories Error: ${e.message}")
            Result.failure(e) 
        }
    }

    override suspend fun syncStreams(profile: ServerProfile, categoryId: String?): Result<Unit> {
        var lastException: Exception? = null
        repeat(3) { attempt ->
            try {
                val response = xtreamApi.getVodStreams(
                    url = "${profile.url}player_api.php",
                    username = profile.username,
                    password = profile.password,
                    categoryId = categoryId
                )
                val entities = withContext(Dispatchers.Default) {
                    response.map { dto ->
                        val safeName = dto.name ?: "Unknown"
                        VodStreamEntity(
                            profileId = profile.id,
                            categoryId = dto.categoryId ?: "0",
                            streamId = dto.streamId ?: 0,
                            num = dto.num ?: 0,
                            name = safeName,
                            normalizedName = StringUtils.normalize(safeName),
                            naturalSortName = StringUtils.naturalSort(safeName),
                            streamType = dto.streamType ?: "movie",
                            streamIcon = dto.streamIcon,
                            rating = dto.rating,
                            added = dto.added,
                            containerExtension = dto.containerExtension,
                            directSource = dto.directSource,
                            tmdbId = dto.tmdbId,
                            backdropUrl = dto.backdropPath?.firstOrNull()
                        )
                    }
                }
                
                if (categoryId == null) {
                    vodDao.deleteStreamsByProfile(profile.id)
                    entities.chunked(1000).forEach { chunk ->
                        vodDao.insertStreams(chunk)
                    }
                } else {
                    entities.chunked(1000).forEach { chunk ->
                        vodDao.insertStreams(chunk)
                    }
                }
                return Result.success(Unit)
            } catch (e: Exception) {
                lastException = e
                Log.e("VodRepo", "SyncStreams Attempt ${attempt + 1} Error: ${e.message}")
                if (attempt < 2) kotlinx.coroutines.delay(2000)
            }
        }
        return Result.failure(lastException ?: Exception("Unknown error"))
    }

    override fun getCategories(profileId: Int): Flow<List<VodCategoryEntity>> = vodDao.getCategories(profileId)

    override fun getStreams(profileId: Int, categoryId: String?): Flow<List<VodStreamEntity>> {
        return when (categoryId) {
            "favorites" -> getFavorites(profileId, "vod")
            "recents" -> getRecents(profileId, "vod")
            null, "all" -> vodDao.getAllStreams(profileId)
            else -> vodDao.getStreamsByCategory(profileId, categoryId)
        }
    }

    override fun getPagedStreams(profileId: Int, categoryId: String?, query: String, sortOrder: String): Flow<PagingData<VodStreamEntity>> {
        return Pager(config = PagingConfig(50), pagingSourceFactory = {
            when (categoryId) {
                "favorites" -> vodDao.getFavoriteStreamsPaged(profileId)
                "recents" -> vodDao.getRecentStreamsPaged(profileId)
                else -> vodDao.getStreamsPaged(profileId, categoryId, StringUtils.normalize(query), sortOrder)
            }
        }).flow
    }

    override fun searchStreams(profileId: Int, query: String): Flow<List<VodStreamEntity>> = vodDao.searchStreams(profileId, StringUtils.normalize(query))

    override suspend fun getMovieDetails(stream: VodStreamEntity): Result<TmdbMovieDetailsDto> {
        return try {
            if (!profileRepository.isTmdbEnabled()) return Result.failure(Exception("TMDB disabled"))
            stream.tmdbId?.let { id -> return Result.success(tmdbApi.getMovieDetails(id)) }
            val searchResponse = tmdbApi.searchMovie(cleanVodName(stream.name))
            val tmdbId = searchResponse.results.firstOrNull()?.id ?: return Result.failure(Exception("Not found"))
            Result.success(tmdbApi.getMovieDetails(tmdbId))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getMovieCollection(collectionId: Int): Result<TmdbCollectionDto> {
        return try { Result.success(tmdbApi.getCollection(collectionId)) } catch (e: Exception) { Result.failure(e) }
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

    override suspend fun getStreamsByTmdbId(profileId: Int, tmdbId: String): List<VodStreamEntity> = vodDao.getStreamsByTmdbId(profileId, tmdbId)

    override suspend fun getStreamsByName(profileId: Int, normalizedName: String): List<VodStreamEntity> = vodDao.getStreamsByName(profileId, normalizedName)

    override suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String) {
        recentDao.insertRecentWithLimit(RecentEntity(profileId, streamId, contentType, System.currentTimeMillis()))
    }

    private fun getRecents(profileId: Int, contentType: String): Flow<List<VodStreamEntity>> = vodDao.getRecentStreams(profileId)
    private fun getFavorites(profileId: Int, contentType: String): Flow<List<VodStreamEntity>> = vodDao.getFavoriteStreams(profileId)

    private fun cleanVodName(name: String): String {
        var clean = name
        clean = clean.replace(Regex("\\(\\d{4}\\)"), "")
        clean = clean.replace(Regex("(?i)\\b(4K|UHD|HD|1080P|720P|SD|CAM|TS|TC|DVD|BLURAY|RIP|H264|H265|HEVC|x264|x265)\\b"), "")
        clean = clean.replace(Regex("(?i)\\b(LATINO|ESPAÑOL|CASTELLANO|SUBTITULADO|SUB|LAT|DOB|DOBLADO|MULTI|ENG|SPA|ITA|FRA|GER|DEU|ESP)\\b"), "")
        clean = clean.replace(Regex("[\\[\\](){}]"), "")
        clean = clean.replace(".", " ").replace("_", " ")
        return clean.trim().replace(Regex("\\s+"), " ")
    }
}
