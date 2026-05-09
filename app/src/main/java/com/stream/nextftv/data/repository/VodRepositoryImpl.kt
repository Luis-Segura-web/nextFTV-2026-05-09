package com.stream.nextftv.data.repository
import com.stream.nextftv.data.local.dao.FavoriteDao
import com.stream.nextftv.data.local.dao.RecentDao
import com.stream.nextftv.data.local.dao.VodDao
import com.stream.nextftv.data.local.entity.FavoriteEntity
import com.stream.nextftv.data.local.entity.RecentEntity
import com.stream.nextftv.data.local.entity.vod.VodCategoryEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.remote.XtreamApiService
import com.stream.nextftv.data.remote.tmdb.TmdbApiService
import com.stream.nextftv.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.nextftv.data.remote.tmdb.TmdbCollectionDto
import com.stream.nextftv.data.remote.tmdb.TmdbPersonSearchResultDto
import com.stream.nextftv.data.remote.tmdb.discoverMoviesByActorWithFallback
import com.stream.nextftv.data.remote.tmdb.getCollectionWithFallback
import com.stream.nextftv.data.remote.tmdb.getMovieDetailsWithFallback
import com.stream.nextftv.data.remote.tmdb.getPersonDetailsWithFallback
import com.stream.nextftv.data.remote.tmdb.searchMovieWithFallback
import com.stream.nextftv.data.remote.tmdb.searchPersonWithFallback
import com.stream.nextftv.data.utils.StringUtils
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.VodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.google.gson.Gson
import com.stream.nextftv.data.local.dao.DetailCacheDao
import com.stream.nextftv.data.local.entity.cache.DetailCacheEntity
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import android.util.Log
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
    private companion object {
        const val MIN_REMOTE_QUERY_LENGTH = 3
        const val MIN_TMDB_COVERAGE_COUNT = 50
        const val MIN_TMDB_COVERAGE_RATIO = 0.15
        const val MAX_TITLE_TMDB_RESULTS = 12
        const val MAX_ACTOR_RESULTS = 8
        const val MAX_ACTOR_CATALOG_PROBES = 10
        const val MAX_ACTOR_FILMOGRAPHY_PROBE_RESULTS = 20
    }

    override suspend fun getStream(profileId: Int, streamId: Int): VodStreamEntity? =
        vodDao.getStreamById(profileId, streamId)

    override suspend fun getVodInfo(
        profile: ServerProfile,
        streamId: Int
    ): Result<com.stream.nextftv.data.remote.VodInfoDto> {
        val cacheKey = "vod_$streamId"
        val now = System.currentTimeMillis()
        val cached = detailCacheDao.getDetail(cacheKey)
        if (cached != null && (now - cached.lastUpdated < 24 * 60 * 60 * 1000L)) {
            return try {
                val info = gson.fromJson(cached.dataJson, com.stream.nextftv.data.remote.VodInfoDto::class.java)
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
            // Opportunistic: update backdrop and tmdbId if available
            response.info?.movieImage?.let { img ->
                getStream(profile.id, streamId)?.let { stream ->
                    if (stream.backdropUrl == null) {
                        vodDao.insertStreams(listOf(stream.copy(backdropUrl = img)))
                    }
                }
            }
            response.info?.tmdbId?.let { id ->
                try {
                    getStream(profile.id, streamId)?.let { stream ->
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
    override fun getCategories(profileId: Int): Flow<List<VodCategoryEntity>> = vodDao.getCategories(profileId)
    override fun getStreams(profileId: Int, categoryId: String?): Flow<List<VodStreamEntity>> {
        return when (categoryId) {
            "favorites" -> getFavorites(profileId, "vod")
            "recents" -> getRecents(profileId, "vod")
            null, "all" -> vodDao.getAllStreams(profileId)
            else -> vodDao.getStreamsByCategory(profileId, categoryId)
        }
    }
    override fun getStreamCount(profileId: Int): Flow<Int> = vodDao.getStreamCount(profileId)
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
    override suspend fun searchStreamsEnhanced(profileId: Int, categoryId: String?, query: String): List<VodStreamEntity> {
        val normalizedQuery = StringUtils.normalize(query)
        if (normalizedQuery.isBlank()) return emptyList()

        val localTitleMatches = getLocalTitleMatches(profileId, categoryId, normalizedQuery)
        if (!profileRepository.isTmdbEnabled()) {
            return dedupeByStreamId(localTitleMatches)
        }
        if (normalizedQuery.length < MIN_REMOTE_QUERY_LENGTH) {
            return dedupeByStreamId(localTitleMatches)
        }
        if (!hasEnoughTmdbCoverage(profileId)) {
            return dedupeByStreamId(localTitleMatches)
        }

        val titleTmdbIds = runCatching {
            tmdbApi.searchMovieWithFallback(query)
                .results
                .map { it.id }
                .distinct()
                .take(MAX_TITLE_TMDB_RESULTS)
        }.getOrDefault(emptyList())

        val titleMatches = getScopedTmdbMatches(profileId, categoryId, titleTmdbIds)

        return dedupeByStreamId(buildList {
            addAll(localTitleMatches)
            addAll(orderByTmdbSequence(titleMatches, titleTmdbIds))
        })
    }

    override suspend fun searchActors(profileId: Int, query: String): List<TmdbPersonSearchResultDto> {
        val normalizedQuery = StringUtils.normalize(query)
        if (normalizedQuery.length < MIN_REMOTE_QUERY_LENGTH) return emptyList()
        if (!profileRepository.isTmdbEnabled()) return emptyList()

        data class ActorMovieMatchScore(
            val actor: TmdbPersonSearchResultDto,
            val totalLocalCount: Int,
            val directTmdbCount: Int,
            val details: com.stream.nextftv.data.remote.tmdb.TmdbPersonDetailsDto?
        )

        return runCatching {
            tmdbApi.searchPersonWithFallback(query)
                .results
                .filterRelevantActors(query)
                .take(MAX_ACTOR_CATALOG_PROBES)
                .mapNotNull { actor ->
                    val actorDetails = runCatching { tmdbApi.getPersonDetailsWithFallback(actor.id) }.getOrNull()
                    val actorMovieCredits = tmdbApi.discoverMoviesByActorWithFallback(actor.id)
                        .results
                        .orEmpty()
                        .distinctBy { it.id }
                        .take(MAX_ACTOR_FILMOGRAPHY_PROBE_RESULTS)
                    if (actorMovieCredits.isEmpty()) return@mapNotNull null

                    val actorMovieTmdbIds = actorMovieCredits.map { it.id }
                    val localByTmdb = getStreamsByTmdbIds(profileId, actorMovieTmdbIds)
                    val localByTitleFallback = actorMovieCredits.flatMap { credit ->
                        val normalizedTitle = StringUtils.normalize(credit.title)
                        if (normalizedTitle.isBlank()) {
                            emptyList()
                        } else {
                            getStreamsByName(profileId, normalizedTitle)
                        }
                    }
                    val directTmdbCount = dedupeByStreamId(localByTmdb).size
                    val totalLocalCount = dedupeByStreamId(localByTmdb + localByTitleFallback).size
                    if (totalLocalCount <= 0) return@mapNotNull null

                    ActorMovieMatchScore(
                        actor = actor,
                        totalLocalCount = totalLocalCount,
                        directTmdbCount = directTmdbCount,
                        details = actorDetails
                    )
                }
                .sortedWith(
                    compareByDescending<ActorMovieMatchScore> { it.totalLocalCount }
                        .thenByDescending { it.directTmdbCount }
                        .thenBy { actorRankBucket(it.actor, it.details, query) }
                        .thenBy { if (it.actor.profilePath.isNullOrBlank()) 1 else 0 }
                        .thenByDescending { it.actor.popularity ?: 0.0 }
                        .thenBy { StringUtils.normalize(it.actor.name) }
                )
                .map { it.actor }
                .take(MAX_ACTOR_RESULTS)
        }.getOrDefault(emptyList())
    }

    override suspend fun getMovieDetails(stream: VodStreamEntity): Result<TmdbMovieDetailsDto> {
        return try {
            if (!profileRepository.isTmdbEnabled()) return Result.failure(Exception("TMDB disabled"))
            stream.tmdbId?.let { id -> return Result.success(tmdbApi.getMovieDetailsWithFallback(id)) }
            val searchResponse = tmdbApi.searchMovieWithFallback(cleanVodName(stream.name))
            val tmdbId = searchResponse.results.firstOrNull()?.id ?: return Result.failure(Exception("Not found"))
            Result.success(tmdbApi.getMovieDetailsWithFallback(tmdbId))
        } catch (e: Exception) { Result.failure(e) }
    }
    override suspend fun getMovieCollection(collectionId: Int): Result<TmdbCollectionDto> {
        return try { Result.success(tmdbApi.getCollectionWithFallback(collectionId)) } catch (e: Exception) { Result.failure(e) }
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
    override suspend fun getStreamsByTmdbIds(profileId: Int, tmdbIds: List<Int>): List<VodStreamEntity> =
        if (tmdbIds.isEmpty()) emptyList() else vodDao.getStreamsByTmdbIds(profileId, "all", tmdbIds)
    override suspend fun getStreamsByName(profileId: Int, normalizedName: String): List<VodStreamEntity> = vodDao.getStreamsByName(profileId, normalizedName)
    override suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String) {
        recentDao.insertRecentWithLimit(RecentEntity(profileId, streamId, contentType, System.currentTimeMillis()))
    }

    private fun getRecents(profileId: Int, contentType: String): Flow<List<VodStreamEntity>> = vodDao.getRecentStreams(profileId)
    private fun getFavorites(profileId: Int, contentType: String): Flow<List<VodStreamEntity>> = vodDao.getFavoriteStreams(profileId)

    private suspend fun getLocalTitleMatches(
        profileId: Int,
        categoryId: String?,
        normalizedQuery: String
    ): List<VodStreamEntity> {
        return when (categoryId) {
            "favorites", "recents" -> getStreams(profileId, categoryId)
                .first()
                .filter { it.normalizedName.contains(normalizedQuery) }

            else -> vodDao.searchStreamsSnapshot(profileId, categoryId, normalizedQuery)
        }
    }

    private suspend fun hasEnoughTmdbCoverage(profileId: Int): Boolean {
        val visibleCount = vodDao.countVisibleStreams(profileId, "all")
        val tmdbCount = vodDao.countVisibleStreamsWithTmdbId(profileId, "all")
        if (visibleCount <= 0) return false
        return tmdbCount >= MIN_TMDB_COVERAGE_COUNT &&
            (tmdbCount.toDouble() / visibleCount.toDouble()) >= MIN_TMDB_COVERAGE_RATIO
    }

    private suspend fun getScopedTmdbMatches(
        profileId: Int,
        categoryId: String?,
        tmdbIds: List<Int>
    ): List<VodStreamEntity> {
        if (tmdbIds.isEmpty()) return emptyList()
        return when (categoryId) {
            "favorites", "recents" -> {
                val idSet = tmdbIds.toSet()
                getStreams(profileId, categoryId).first().filter { movie ->
                    movie.tmdbId != null && movie.tmdbId in idSet
                }
            }

            else -> vodDao.getStreamsByTmdbIds(profileId, categoryId, tmdbIds)
        }
    }

    private fun orderByTmdbSequence(
        items: List<VodStreamEntity>,
        tmdbIds: List<Int>
    ): List<VodStreamEntity> {
        if (items.isEmpty() || tmdbIds.isEmpty()) return items
        val indexByTmdbId = tmdbIds.withIndex().associate { it.value to it.index }
        return items.sortedWith(
            compareBy<VodStreamEntity> { movie -> indexByTmdbId[movie.tmdbId ?: -1] ?: Int.MAX_VALUE }
                .thenBy { it.naturalSortName }
        )
    }

    private fun dedupeByStreamId(items: List<VodStreamEntity>): List<VodStreamEntity> {
        if (items.isEmpty()) return emptyList()
        return LinkedHashMap<Int, VodStreamEntity>(items.size).apply {
            items.forEach { movie -> putIfAbsent(movie.streamId, movie) }
        }.values.toList()
    }

    private fun List<TmdbPersonSearchResultDto>.filterRelevantActors(query: String): List<TmdbPersonSearchResultDto> {
        return sortedWith(
            compareBy<TmdbPersonSearchResultDto>(
                { person -> actorRankBucket(person, null, query) },
                { person -> if (person.profilePath.isNullOrBlank()) 1 else 0 },
                { person -> -((person.popularity ?: 0.0) * 100).toInt() },
                { person -> StringUtils.normalize(person.name) }
            )
        )
    }

    private fun actorRankBucket(
        person: TmdbPersonSearchResultDto,
        personDetails: com.stream.nextftv.data.remote.tmdb.TmdbPersonDetailsDto?,
        query: String
    ): Int {
        val normalizedQuery = StringUtils.normalize(query)
        val normalizedName = StringUtils.normalize(person.name)
        val aliasMatches = personDetails
            ?.alsoKnownAs
            .orEmpty()
            .map { StringUtils.normalize(it) }
        return when {
            normalizedName == normalizedQuery -> 0
            normalizedName.startsWith(normalizedQuery) -> 1
            aliasMatches.any { it == normalizedQuery } -> 1
            aliasMatches.any { it.startsWith(normalizedQuery) } -> 2
            normalizedName.contains(normalizedQuery) -> 2
            aliasMatches.any { it.contains(normalizedQuery) } -> 3
            else -> 3
        }
    }

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
