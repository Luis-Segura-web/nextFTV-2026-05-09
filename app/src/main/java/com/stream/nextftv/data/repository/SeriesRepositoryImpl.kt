package com.stream.nextftv.data.repository
import com.stream.nextftv.data.local.dao.FavoriteDao
import com.stream.nextftv.data.local.dao.RecentDao
import com.stream.nextftv.data.local.dao.SeriesDao
import com.stream.nextftv.data.local.entity.FavoriteEntity
import com.stream.nextftv.data.local.entity.RecentEntity
import com.stream.nextftv.data.local.entity.series.SeriesCategoryEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.remote.XtreamApiService
import com.stream.nextftv.data.remote.SeriesEpisodeDto
import com.stream.nextftv.data.remote.tmdb.TmdbApiService
import com.stream.nextftv.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.nextftv.data.remote.tmdb.TmdbPersonDetailsDto
import com.stream.nextftv.data.remote.tmdb.TmdbPersonSearchResultDto
import com.stream.nextftv.data.remote.tmdb.getPersonDetailsWithFallback
import com.stream.nextftv.data.remote.tmdb.getTvDetailsWithFallback
import com.stream.nextftv.data.remote.tmdb.getTvSeasonDetailsWithFallback
import com.stream.nextftv.data.remote.tmdb.searchPersonWithFallback
import com.stream.nextftv.data.remote.tmdb.searchTvWithFallback
import com.stream.nextftv.data.utils.StringUtils
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.SeriesRepository
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
    private companion object {
        const val MIN_REMOTE_QUERY_LENGTH = 3
        const val MIN_TMDB_COVERAGE_COUNT = 50
        const val MIN_TMDB_COVERAGE_RATIO = 0.15
        const val MAX_TITLE_TMDB_RESULTS = 12
        const val MAX_ACTOR_RESULTS = 8
        const val MAX_ACTOR_CATALOG_PROBES = 10
        const val MAX_ACTOR_CREDITS_PROBE_RESULTS = 24
    }

    override suspend fun getSeries(profileId: Int, seriesId: Int): SeriesStreamEntity? =
        seriesDao.getSeriesById(profileId, seriesId)

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
                    getSeries(profile.id, seriesId)?.let { series ->
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
    override fun getSeriesCount(profileId: Int): Flow<Int> = seriesDao.getSeriesCount(profileId)
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
    override suspend fun searchSeriesEnhanced(profileId: Int, categoryId: String?, query: String): List<SeriesStreamEntity> {
        val normalizedQuery = StringUtils.normalize(query)
        if (normalizedQuery.isBlank()) return emptyList()

        val localTitleMatches = getLocalTitleMatches(profileId, categoryId, normalizedQuery)
        if (!profileRepository.isTmdbEnabled()) {
            return dedupeBySeriesId(localTitleMatches)
        }
        if (normalizedQuery.length < MIN_REMOTE_QUERY_LENGTH) {
            return dedupeBySeriesId(localTitleMatches)
        }
        if (!hasEnoughTmdbCoverage(profileId)) {
            return dedupeBySeriesId(localTitleMatches)
        }

        val titleTmdbIds = runCatching {
            tmdbApi.searchTvWithFallback(query)
                .results
                .map { it.id }
                .distinct()
                .take(MAX_TITLE_TMDB_RESULTS)
        }.getOrDefault(emptyList())

        val titleMatches = getScopedTmdbMatches(profileId, categoryId, titleTmdbIds)

        return dedupeBySeriesId(buildList {
            addAll(localTitleMatches)
            addAll(orderByTmdbSequence(titleMatches, titleTmdbIds))
        })
    }

    override suspend fun searchActors(profileId: Int, query: String): List<TmdbPersonSearchResultDto> {
        val normalizedQuery = StringUtils.normalize(query)
        if (normalizedQuery.length < MIN_REMOTE_QUERY_LENGTH) return emptyList()
        if (!profileRepository.isTmdbEnabled()) return emptyList()

        data class ActorSeriesMatchScore(
            val actor: TmdbPersonSearchResultDto,
            val totalLocalCount: Int,
            val directTmdbCount: Int,
            val details: TmdbPersonDetailsDto?
        )

        return runCatching {
            tmdbApi.searchPersonWithFallback(query)
                .results
                .filterRelevantActors(query)
                .take(MAX_ACTOR_CATALOG_PROBES)
                .mapNotNull { actor ->
                    val actorDetails = runCatching { tmdbApi.getPersonDetailsWithFallback(actor.id) }.getOrNull()
                    val actorSeriesCredits = actorDetails
                        ?.combinedCredits
                        ?.cast
                        .orEmpty()
                        .asSequence()
                        .filter { it.mediaType == "tv" }
                        .distinctBy { it.id }
                        .take(MAX_ACTOR_CREDITS_PROBE_RESULTS)
                        .toList()
                    if (actorSeriesCredits.isEmpty()) return@mapNotNull null

                    val localByTmdb = getSeriesListByTmdbIds(profileId, actorSeriesCredits.map { it.id })
                    val localByTitleFallback = actorSeriesCredits.flatMap { credit ->
                        val normalizedTitle = StringUtils.normalize(credit.tvName ?: credit.title)
                        if (normalizedTitle.isBlank()) {
                            emptyList()
                        } else {
                            getSeriesListByName(profileId, normalizedTitle)
                        }
                    }
                    val directTmdbCount = dedupeBySeriesId(localByTmdb).size
                    val totalLocalCount = dedupeBySeriesId(localByTmdb + localByTitleFallback).size
                    if (totalLocalCount <= 0) return@mapNotNull null

                    ActorSeriesMatchScore(
                        actor = actor,
                        totalLocalCount = totalLocalCount,
                        directTmdbCount = directTmdbCount,
                        details = actorDetails
                    )
                }
                .sortedWith(
                    compareByDescending<ActorSeriesMatchScore> { it.totalLocalCount }
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

    override suspend fun getSeriesDetails(series: SeriesStreamEntity): Result<TmdbMovieDetailsDto> {
        return try {
            if (!profileRepository.isTmdbEnabled()) return Result.failure(Exception("Disabled"))
            series.tmdbId?.let { id -> return Result.success(tmdbApi.getTvDetailsWithFallback(id)) }
            val cleanName = cleanSeriesName(series.name)
            val searchResponse = tmdbApi.searchTvWithFallback(cleanName)
            val tmdbId = searchResponse.results.firstOrNull()?.id ?: return Result.failure(Exception("Not found"))
            Result.success(tmdbApi.getTvDetailsWithFallback(tmdbId))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getSeriesEpisodeTitles(
        series: SeriesStreamEntity,
        seasonNumbers: Set<Int>
    ): Result<Map<Pair<Int, Int>, String>> {
        return try {
            if (!profileRepository.isTmdbEnabled()) return Result.failure(Exception("Disabled"))
            if (seasonNumbers.isEmpty()) return Result.success(emptyMap())

            val tmdbId = series.tmdbId ?: run {
                val cleanName = cleanSeriesName(series.name)
                val searchResponse = tmdbApi.searchTvWithFallback(cleanName)
                searchResponse.results.firstOrNull()?.id ?: return Result.failure(Exception("Not found"))
            }

            val episodeTitles = mutableMapOf<Pair<Int, Int>, String>()
            seasonNumbers.sorted().forEach { seasonNumber ->
                val seasonDetails = tmdbApi.getTvSeasonDetailsWithFallback(tmdbId, seasonNumber)
                seasonDetails.episodes.orEmpty().forEach { episode ->
                    val episodeNumber = episode.episodeNumber ?: return@forEach
                    val name = episode.name?.trim().orEmpty()
                    if (name.isNotBlank()) {
                        episodeTitles[seasonNumber to episodeNumber] = name
                    }
                }
            }
            Result.success(episodeTitles)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    override suspend fun getSeriesListByTmdbIds(profileId: Int, tmdbIds: List<Int>): List<SeriesStreamEntity> =
        if (tmdbIds.isEmpty()) emptyList() else seriesDao.getSeriesListByTmdbIds(profileId, tmdbIds)
    override suspend fun getSeriesListByName(profileId: Int, normalizedName: String): List<SeriesStreamEntity> = seriesDao.getSeriesListByName(profileId, normalizedName)
    override suspend fun addToRecents(profileId: Int, streamId: Int, contentType: String) {
        recentDao.insertRecentWithLimit(RecentEntity(profileId, streamId, contentType, System.currentTimeMillis()))
    }
    private fun getRecents(profileId: Int, contentType: String): Flow<List<SeriesStreamEntity>> = seriesDao.getRecentStreams(profileId)
    private fun getFavorites(profileId: Int, contentType: String): Flow<List<SeriesStreamEntity>> = seriesDao.getFavoriteStreams(profileId)

    private suspend fun getLocalTitleMatches(
        profileId: Int,
        categoryId: String?,
        normalizedQuery: String
    ): List<SeriesStreamEntity> {
        return when (categoryId) {
            "favorites", "recents" -> getSeries(profileId, categoryId)
                .first()
                .filter { it.normalizedName.contains(normalizedQuery) }

            else -> seriesDao.searchStreamsSnapshot(profileId, categoryId, normalizedQuery)
        }
    }

    private suspend fun hasEnoughTmdbCoverage(profileId: Int): Boolean {
        val visibleCount = seriesDao.countVisibleStreams(profileId, "all")
        val tmdbCount = seriesDao.countVisibleStreamsWithTmdbId(profileId, "all")
        if (visibleCount <= 0) return false
        return tmdbCount >= MIN_TMDB_COVERAGE_COUNT &&
            (tmdbCount.toDouble() / visibleCount.toDouble()) >= MIN_TMDB_COVERAGE_RATIO
    }

    private suspend fun getScopedTmdbMatches(
        profileId: Int,
        categoryId: String?,
        tmdbIds: List<Int>
    ): List<SeriesStreamEntity> {
        if (tmdbIds.isEmpty()) return emptyList()
        return when (categoryId) {
            "favorites", "recents" -> {
                val idSet = tmdbIds.toSet()
                getSeries(profileId, categoryId).first().filter { series ->
                    series.tmdbId != null && series.tmdbId in idSet
                }
            }

            else -> seriesDao.getSeriesByTmdbIds(profileId, categoryId, tmdbIds)
        }
    }

    private fun orderByTmdbSequence(
        items: List<SeriesStreamEntity>,
        tmdbIds: List<Int>
    ): List<SeriesStreamEntity> {
        if (items.isEmpty() || tmdbIds.isEmpty()) return items
        val indexByTmdbId = tmdbIds.withIndex().associate { it.value to it.index }
        return items.sortedWith(
            compareBy<SeriesStreamEntity> { series -> indexByTmdbId[series.tmdbId ?: -1] ?: Int.MAX_VALUE }
                .thenBy { it.naturalSortName }
        )
    }

    private fun dedupeBySeriesId(items: List<SeriesStreamEntity>): List<SeriesStreamEntity> {
        if (items.isEmpty()) return emptyList()
        return LinkedHashMap<Int, SeriesStreamEntity>(items.size).apply {
            items.forEach { series -> putIfAbsent(series.seriesId, series) }
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
        personDetails: TmdbPersonDetailsDto?,
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
