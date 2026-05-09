package com.stream.nextftv.data.repository
import com.stream.nextftv.data.local.dao.FavoriteDao
import com.stream.nextftv.data.local.dao.LiveTvDao
import com.stream.nextftv.data.local.dao.RecentDao
import com.stream.nextftv.data.local.entity.FavoriteEntity
import com.stream.nextftv.data.local.entity.RecentEntity
import com.stream.nextftv.data.local.entity.live.LiveCategoryEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamEntity
import com.stream.nextftv.data.utils.StringUtils
import com.stream.nextftv.domain.repository.LiveTvRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
class LiveTvRepositoryImpl @Inject constructor(
    private val liveTvDao: LiveTvDao,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao
) : LiveTvRepository {
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
    override fun getStreamCount(profileId: Int): Flow<Int> = liveTvDao.getStreamCount(profileId)
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
