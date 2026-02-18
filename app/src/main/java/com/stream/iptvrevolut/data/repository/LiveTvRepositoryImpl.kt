package com.stream.iptvrevolut.data.repository

import com.stream.iptvrevolut.data.local.dao.FavoriteDao
import com.stream.iptvrevolut.data.local.dao.LiveTvDao
import com.stream.iptvrevolut.data.local.dao.RecentDao
import com.stream.iptvrevolut.data.local.entity.FavoriteEntity
import com.stream.iptvrevolut.data.local.entity.RecentEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveCategoryEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveStreamEntity
import com.stream.iptvrevolut.data.remote.XtreamApiService
import com.stream.iptvrevolut.data.utils.StringUtils
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.LiveTvRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LiveTvRepositoryImpl @Inject constructor(
    private val apiService: XtreamApiService,
    private val liveTvDao: LiveTvDao,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao
) : LiveTvRepository {

    override suspend fun syncCategories(profile: ServerProfile): Result<Unit> {
        return try {
            val response = apiService.getLiveCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
            val entities = response.map { dto ->
                LiveCategoryEntity(
                    profileId = profile.id,
                    categoryId = dto.categoryId,
                    categoryName = dto.categoryName,
                    parentId = dto.parentId ?: 0
                )
            }
            liveTvDao.replaceCategories(profile.id, entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncStreams(profile: ServerProfile, categoryId: String?): Result<Unit> {
        var lastException: Exception? = null
        repeat(3) { attempt ->
            try {
                val response = apiService.getLiveStreams(
                    url = "${profile.url}player_api.php",
                    username = profile.username,
                    password = profile.password,
                    categoryId = categoryId
                )
                val entities = withContext(Dispatchers.Default) {
                    response.map { dto ->
                        val safeName = dto.name ?: "Unknown"
                        LiveStreamEntity(
                            profileId = profile.id,
                            categoryId = dto.categoryId ?: "0",
                            streamId = dto.streamId ?: 0,
                            num = dto.num ?: 0,
                            name = safeName,
                            normalizedName = StringUtils.normalize(safeName),
                            naturalSortName = StringUtils.naturalSort(safeName),
                            streamType = dto.streamType ?: "live",
                            streamIcon = dto.streamIcon,
                            epgChannelId = dto.epgChannelId,
                            added = dto.added,
                            customSid = dto.customSid,
                            tvArchive = dto.tvArchive ?: 0,
                            directSource = dto.directSource,
                            tvArchiveDuration = dto.tvArchiveDuration ?: 0
                        )
                    }
                }

                if (categoryId == null) {
                    liveTvDao.deleteStreamsByProfile(profile.id)
                    entities.chunked(1000).forEach { chunk ->
                        liveTvDao.insertStreams(chunk)
                    }
                } else {
                    entities.chunked(1000).forEach { chunk ->
                        liveTvDao.insertStreams(chunk)
                    }
                }
                return Result.success(Unit)
            } catch (e: Exception) {
                lastException = e
                if (attempt < 2) kotlinx.coroutines.delay(2000)
            }
        }
        return Result.failure(lastException ?: Exception("Unknown error"))
    }

    override fun getCategories(profileId: Int): Flow<List<LiveCategoryEntity>> {
        return liveTvDao.getCategories(profileId)
    }

    override fun getStreams(profileId: Int, categoryId: String?): Flow<List<LiveStreamEntity>> {
        return when (categoryId) {
            "favorites" -> getFavorites(profileId, "live")
            "recents" -> getRecents(profileId, "live")
            null, "all" -> liveTvDao.getAllStreams(profileId)
            else -> liveTvDao.getStreamsByCategory(profileId, categoryId)
        }
    }

    override fun getPagedStreams(
        profileId: Int,
        categoryId: String?,
        query: String,
        sortOrder: String
    ): Flow<PagingData<LiveStreamEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { 
                when (categoryId) {
                    "favorites" -> liveTvDao.getFavoriteStreamsPaged(profileId)
                    "recents" -> liveTvDao.getRecentStreamsPaged(profileId)
                    else -> liveTvDao.getStreamsPaged(profileId, categoryId, StringUtils.normalize(query), sortOrder)
                }
            }
        ).flow
    }

    override suspend fun getFilteredStreams(
        profileId: Int,
        categoryId: String?,
        query: String,
        sortOrder: String
    ): List<LiveStreamEntity> {
        return when (categoryId) {
            "favorites" -> liveTvDao.getFavoriteStreams(profileId).first()
            "recents" -> liveTvDao.getRecentStreams(profileId).first()
            else -> liveTvDao.getStreamsFiltered(profileId, categoryId, StringUtils.normalize(query), sortOrder)
        }
    }

    override fun searchStreams(profileId: Int, query: String): Flow<List<LiveStreamEntity>> {
        return liveTvDao.searchStreams(profileId, StringUtils.normalize(query))
    }

    override suspend fun toggleFavorite(profileId: Int, streamId: Int, contentType: String) {
        val isFav = favoriteDao.isFavorite(profileId, streamId, contentType).first()
        if (isFav) {
            favoriteDao.deleteFavorite(FavoriteEntity(profileId, streamId, contentType))
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(profileId, streamId, contentType, System.currentTimeMillis()))
        }
    }

    override fun isFavorite(profileId: Int, streamId: Int, contentType: String): Flow<Boolean> {
        return favoriteDao.isFavorite(profileId, streamId, contentType)
    }

    override fun getFavorites(profileId: Int, contentType: String): Flow<List<LiveStreamEntity>> {
        return liveTvDao.getFavoriteStreams(profileId)
    }

    override suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String) {
        recentDao.insertRecentWithLimit(RecentEntity(profileId, streamId, contentType, System.currentTimeMillis()))
    }

    private fun getRecents(profileId: Int, contentType: String): Flow<List<LiveStreamEntity>> {
        return liveTvDao.getRecentStreams(profileId)
    }
}
