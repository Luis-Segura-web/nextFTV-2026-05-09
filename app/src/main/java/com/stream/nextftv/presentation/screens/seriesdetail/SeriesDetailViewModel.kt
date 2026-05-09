package com.stream.nextftv.presentation.screens.seriesdetail

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.data.remote.SeriesEpisodeDto
import com.stream.nextftv.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.SeriesRepository
import com.stream.nextftv.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val seriesRepository: SeriesRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    private companion object {
        const val TAG = "SeriesDetailLoad"
        private const val FINISHED_THRESHOLD = 0.98f
        private const val PROGRESS_PERSIST_INTERVAL_MS = 1_000L
    }

    enum class PrimaryActionMode {
        PLAY,
        CONTINUE,
        NEXT
    }

    private val prefs by lazy { context.getSharedPreferences("playback_resume_prefs", Context.MODE_PRIVATE) }

    var seriesDetails by mutableStateOf<TmdbMovieDetailsDto?>(null)
    var seriesStream by mutableStateOf<SeriesStreamEntity?>(null)
    var episodes by mutableStateOf<Map<String, List<SeriesEpisodeDto>>?>(null)
    var activeProfile by mutableStateOf<ServerProfile?>(null)
    
    var recommendedSeries by mutableStateOf<List<Pair<com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto, Int>>>(emptyList())
    var similarSeries by mutableStateOf<List<Pair<com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto, Int>>>(emptyList())
    
    // Variaciones para series
    data class SeriesVariationItem(
        val tmdbItem: com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto,
        val localId: Int,
        val isActual: Boolean
    )
    data class SeriesVariantOption(
        val source: SeriesStreamEntity,
        val isCurrent: Boolean
    )
    var variationItems by mutableStateOf<List<SeriesVariationItem>>(emptyList())

    var variations by mutableStateOf<List<SeriesVariantOption>>(emptyList())
    var showVariationSelector by mutableStateOf(false)
    var variationSelectorTitle by mutableStateOf("Seleccionar version")
    private var sourceCategoryId: String? = null

    var isPlayerActive by mutableStateOf(false)
    var isPlayerLoading by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
    var autoPlayNextEnabled by mutableStateOf(true)
    var localEpisodePaths = mutableStateMapOf<String, String>()
    var episodeProgressById = mutableStateMapOf<String, Long>()
    var episodeDurationById = mutableStateMapOf<String, Long>()
    var tmdbEpisodeTitleByKey = mutableStateMapOf<String, String>()
    var lastEpisodeId by mutableStateOf<String?>(null)
    var lastEpisodeSeason by mutableStateOf<Int?>(null)
    var lastEpisodeNumber by mutableStateOf<Int?>(null)
    var lastEpisodePositionMs by mutableStateOf(0L)
    var primaryActionMode by mutableStateOf(PrimaryActionMode.PLAY)
    private var lastPersistedEpisodeProgressMs = mutableStateMapOf<String, Long>()
    private var loadGeneration = 0

    val episodeDownloads: Flow<Map<String, DownloadEntity>> = combine(
        profileRepository.getProfiles().map { it.find { p -> p.isActive }?.id },
        snapshotFlow { seriesStream }
    ) { profileId, currentSeries ->
        profileId to currentSeries
    }.flatMapLatest { (profileId, currentSeries) ->
        if (profileId != null && currentSeries != null) {
            downloadRepository.getDownloads(profileId).map { downloads ->
                downloads.filter { it.type == "episode" && it.parentId == currentSeries.seriesId }
                    .associateBy { it.streamId.toString() }
            }
        } else flowOf(emptyMap())
    }

    fun loadSeries(seriesId: Int, initialEpisodeId: String?, autoPlay: Boolean, sourceCategoryId: String? = null) {
        viewModelScope.launch {
            val generation = ++loadGeneration
            val startedAt = SystemClock.elapsedRealtime()
            this@SeriesDetailViewModel.sourceCategoryId = sourceCategoryId
            isLoading = true
            seriesDetails = null
            recommendedSeries = emptyList()
            similarSeries = emptyList()
            variationItems = emptyList()
            try {
                val profiles = profileRepository.getProfiles().first()
                activeProfile = profiles.find { it.isActive }
                val profile = activeProfile ?: return@launch
                autoPlayNextEnabled = profileRepository.isAutoPlayNextEnabled()
                Log.d(TAG, "loadSeries start seriesId=$seriesId t=${elapsedMs(startedAt)}ms")

                val stream = seriesRepository.getSeries(profile.id, seriesId) ?: return@launch
                if (generation != loadGeneration) return@launch
                seriesStream = stream
                loadSeriesProgress(profile.id, stream.seriesId)
                Log.d(TAG, "loadSeries series_resolved seriesId=$seriesId t=${elapsedMs(startedAt)}ms")

                isLoading = false
                Log.d(TAG, "loadSeries ui_ready seriesId=$seriesId t=${elapsedMs(startedAt)}ms")

                viewModelScope.launch {
                    val episodesStartedAt = SystemClock.elapsedRealtime()
                    val episodesResult = seriesRepository.getSeriesEpisodes(profile, seriesId)
                    if (generation != loadGeneration) return@launch
                    episodesResult.onSuccess { eps ->
                        episodes = eps

                        val downloads = downloadRepository.getDownloads(profile.id).first()
                        localEpisodePaths.clear()
                        eps.values.flatten().forEach { ep ->
                            downloads.find { it.streamId == (ep.id?.toIntOrNull() ?: 0) && it.type == "episode" }?.let { d ->
                                if (
                                    d.status == "completed" &&
                                    File(d.filePath).exists() &&
                                    File(d.filePath).length() > 0L
                                ) {
                                    localEpisodePaths[ep.id ?: ""] = d.filePath
                                }
                            }
                        }

                        if (autoPlay && initialEpisodeId != null) {
                            eps.values.flatten().find { it.id == initialEpisodeId }?.let { targetEp ->
                                currentEpisode = targetEp
                                selectedSeason = targetEp.season ?: 1
                                isPlayerActive = true
                            }
                        } else if (eps.isNotEmpty()) {
                            selectedSeason = eps.keys.sortedBy { it.toIntOrNull() ?: 0 }.firstOrNull()?.toIntOrNull() ?: 1
                        }
                        loadEpisodeProgressMap(profile.id, seriesId, eps)
                        val seasonNumbers = eps.keys.mapNotNull { it.toIntOrNull() }.toSet()
                        seriesRepository.getSeriesEpisodeTitles(stream, seasonNumbers)
                            .onSuccess { titleMap ->
                                tmdbEpisodeTitleByKey.clear()
                                titleMap.forEach { (key, value) ->
                                    tmdbEpisodeTitleByKey["${key.first}:${key.second}"] = value
                                }
                            }
                        Log.d(
                            TAG,
                            "loadSeries episodes_done seriesId=$seriesId seasons=${eps.size} request=${elapsedMs(episodesStartedAt)}ms total=${elapsedMs(startedAt)}ms"
                        )
                    }.onFailure { error ->
                        Log.w(TAG, "loadSeries episodes_failed seriesId=$seriesId total=${elapsedMs(startedAt)}ms error=${error.message}")
                    }
                }

                viewModelScope.launch {
                    val tmdbStartedAt = SystemClock.elapsedRealtime()
                    val tmdbResult = seriesRepository.getSeriesDetails(stream)
                    if (generation != loadGeneration) return@launch
                    tmdbResult.onSuccess { details ->
                        seriesDetails = details

                        details.recommendations?.results?.let { list ->
                            recommendedSeries = list.mapNotNull { tmdbItem ->
                                val locals = seriesRepository.getSeriesListByTmdbId(profile.id, tmdbItem.id.toString())
                                locals.firstOrNull()?.let { local ->
                                    preferredLocalSeriesCard(tmdbItem, locals) to local.seriesId
                                }
                            }
                        }

                        details.similar?.results?.let { list ->
                            similarSeries = list.mapNotNull { tmdbItem ->
                                val locals = seriesRepository.getSeriesListByTmdbId(profile.id, tmdbItem.id.toString())
                                locals.firstOrNull()?.let { local ->
                                    preferredLocalSeriesCard(tmdbItem, locals) to local.seriesId
                                }
                            }
                        }

                        Log.d(
                            TAG,
                            "loadSeries tmdb_done seriesId=$seriesId recommendations=${details.recommendations?.results?.size ?: 0} similar=${details.similar?.results?.size ?: 0} request=${elapsedMs(tmdbStartedAt)}ms total=${elapsedMs(startedAt)}ms"
                        )
                    }.onFailure { error ->
                        Log.w(TAG, "loadSeries tmdb_failed seriesId=$seriesId total=${elapsedMs(startedAt)}ms error=${error.message}")
                    }
                }

                viewModelScope.launch {
                    val variationsStartedAt = SystemClock.elapsedRealtime()
                    val currentTmdbId = stream.tmdbId
                    val allVariations = if (currentTmdbId != null) {
                        seriesRepository.getSeriesListByTmdbId(profile.id, currentTmdbId.toString()).distinctBy { it.seriesId }
                    } else {
                        emptyList()
                    }

                    if (generation != loadGeneration) return@launch
                    if (allVariations.size > 1) {
                        variationItems = allVariations.map { variation ->
                            SeriesVariationItem(
                                tmdbItem = com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto(
                                    id = variation.tmdbId ?: 0,
                                    title = null,
                                    tvName = variation.name,
                                    posterPath = variation.cover
                                ),
                                localId = variation.seriesId,
                                isActual = variation.seriesId == seriesId
                            )
                        }
                        Log.d(
                            TAG,
                            "loadSeries variations_done seriesId=$seriesId items=${variationItems.size} request=${elapsedMs(variationsStartedAt)}ms total=${elapsedMs(startedAt)}ms"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadSeries failed seriesId=$seriesId total=${elapsedMs(startedAt)}ms error=${e.message}", e)
            } finally {
                if (seriesStream == null || generation != loadGeneration) {
                    isLoading = false
                }
            }
        }
    }

    private fun elapsedMs(startedAt: Long): Long = SystemClock.elapsedRealtime() - startedAt

    var selectedSeason by mutableStateOf(1)
    var currentEpisode by mutableStateOf<SeriesEpisodeDto?>(null)

    fun onEpisodeClick(episode: SeriesEpisodeDto) {
        selectedSeason = episode.season ?: selectedSeason
        currentEpisode = episode
        isPlayerActive = true 
    }

    fun onEpisodeClickFromStart(episode: SeriesEpisodeDto) {
        val episodeId = episode.id
        if (episodeId != null) {
            val profileId = activeProfile?.id
            val seriesId = seriesStream?.seriesId
            episodeProgressById[episodeId] = 0L
            if (lastEpisodeId == episodeId) {
                lastEpisodePositionMs = 0L
                if (primaryActionMode == PrimaryActionMode.CONTINUE) {
                    primaryActionMode = PrimaryActionMode.PLAY
                }
            }
            if (profileId != null && seriesId != null) {
                prefs.edit()
                    .putLong(seriesEpisodeProgressKey(profileId, seriesId, episodeId), 0L)
                    .putLong(seriesPositionKey(profileId, seriesId), if (lastEpisodeId == episodeId) 0L else lastEpisodePositionMs)
                    .putString(seriesActionModeKey(profileId, seriesId), primaryActionMode.name)
                    .apply()
            }
        }
        onEpisodeClick(episode)
    }

    private fun orderedEpisodes(): List<SeriesEpisodeDto> {
        val allEps = episodes ?: return emptyList()
        return allEps
            .toList()
            .sortedBy { (seasonKey, _) -> seasonKey.toIntOrNull() ?: Int.MAX_VALUE }
            .flatMap { (_, eps) ->
                eps.sortedWith(
                    compareBy<SeriesEpisodeDto> { it.episodeNum ?: Int.MAX_VALUE }
                        .thenBy { it.id?.toIntOrNull() ?: Int.MAX_VALUE }
                )
            }
    }

    fun onNextEpisode() {
        val current = currentEpisode ?: return
        val flattened = orderedEpisodes()
        if (flattened.isEmpty()) return

        val currentIndex = flattened.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) {
            onEpisodeClick(flattened.first())
            return
        }
        if (currentIndex < flattened.lastIndex) {
            onEpisodeClick(flattened[currentIndex + 1])
        }
    }

    fun onPreviousEpisode() {
        val current = currentEpisode ?: return
        val flattened = orderedEpisodes()
        if (flattened.isEmpty()) return

        val currentIndex = flattened.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) {
            onEpisodeClick(flattened.first())
            return
        }
        if (currentIndex > 0) {
            onEpisodeClick(flattened[currentIndex - 1])
        }
    }
    
    fun onSeasonSelect(season: Int) {
        selectedSeason = season
    }

    fun onPlaybackStarted(seriesId: Int) {
        viewModelScope.launch {
            activeProfile?.let { profile ->
                seriesRepository.addToRecents(profile.id, seriesId, "series")
            }
        }
    }

    fun updateSeriesProgress(episode: SeriesEpisodeDto, positionMs: Long) {
        val profileId = activeProfile?.id ?: return
        val seriesId = seriesStream?.seriesId ?: return
        if (positionMs < 0L) return

        val episodeId = episode.id ?: return
        val season = episode.season
        val number = episode.episodeNum

        lastEpisodeId = episodeId
        lastEpisodeSeason = season
        lastEpisodeNumber = number
        lastEpisodePositionMs = positionMs
        episodeProgressById[episodeId] = positionMs

        val lastPersisted = lastPersistedEpisodeProgressMs[episodeId] ?: 0L
        val shouldPersistNow =
            positionMs == 0L ||
                positionMs < lastPersisted ||
                positionMs - lastPersisted >= PROGRESS_PERSIST_INTERVAL_MS

        if (!shouldPersistNow) return

        lastPersistedEpisodeProgressMs[episodeId] = positionMs

        prefs.edit().apply {
            putString(seriesEpisodeIdKey(profileId, seriesId), episodeId)
            if (season != null) putInt(seriesSeasonKey(profileId, seriesId), season) else remove(seriesSeasonKey(profileId, seriesId))
            if (number != null) putInt(seriesEpisodeNumberKey(profileId, seriesId), number) else remove(seriesEpisodeNumberKey(profileId, seriesId))
            putLong(seriesPositionKey(profileId, seriesId), positionMs)
            putLong(seriesEpisodeProgressKey(profileId, seriesId, episodeId), positionMs)
            apply()
        }
    }

    fun updateEpisodePlaybackSnapshot(episode: SeriesEpisodeDto, positionMs: Long, durationMs: Long) {
        val profileId = activeProfile?.id ?: return
        val seriesId = seriesStream?.seriesId ?: return
        val episodeId = episode.id ?: return
        val safePosition = positionMs.coerceAtLeast(0L)
        val safeDuration = durationMs.coerceAtLeast(0L)

        if (safePosition > 0L) {
            episodeProgressById[episodeId] = safePosition
            if (lastEpisodeId == episodeId) {
                lastEpisodePositionMs = safePosition
            }
        }

        if (safeDuration <= 0L) return
        val previousDuration = episodeDurationById[episodeId] ?: 0L
        if (safeDuration == previousDuration) return

        episodeDurationById[episodeId] = safeDuration
        prefs.edit()
            .putLong(seriesEpisodeDurationKey(profileId, seriesId, episodeId), safeDuration)
            .apply()
    }

    fun onPlayerClosed(episode: SeriesEpisodeDto, positionMs: Long, durationMs: Long) {
        val profileId = activeProfile?.id ?: return
        val seriesId = seriesStream?.seriesId ?: return
        val episodeId = episode.id ?: return
        val clampedPosition = positionMs.coerceAtLeast(0L)
        val clampedDuration = durationMs.coerceAtLeast(0L)
        val completionThreshold = (clampedDuration * FINISHED_THRESHOLD).toLong()
        val isCompleted = clampedDuration > 0L && clampedPosition >= completionThreshold

        if (clampedDuration > 0L) {
            episodeDurationById[episodeId] = clampedDuration
            prefs.edit()
                .putLong(seriesEpisodeDurationKey(profileId, seriesId, episodeId), clampedDuration)
                .apply()
        }

        if (isCompleted) {
            episodeProgressById[episodeId] = clampedDuration
            prefs.edit()
                .putLong(seriesEpisodeProgressKey(profileId, seriesId, episodeId), clampedDuration)
                .apply()

            val nextEpisode = nextEpisodeFor(episode)
            if (nextEpisode != null) {
                persistPrimaryTarget(
                    profileId = profileId,
                    seriesId = seriesId,
                    episode = nextEpisode,
                    positionMs = 0L,
                    mode = PrimaryActionMode.NEXT
                )
            } else {
                val firstEpisode = orderedEpisodes().firstOrNull() ?: episode
                persistPrimaryTarget(
                    profileId = profileId,
                    seriesId = seriesId,
                    episode = firstEpisode,
                    positionMs = 0L,
                    mode = PrimaryActionMode.PLAY
                )
            }
            return
        }

        persistPrimaryTarget(
            profileId = profileId,
            seriesId = seriesId,
            episode = episode,
            positionMs = clampedPosition,
            mode = if (clampedPosition > 0L) PrimaryActionMode.CONTINUE else PrimaryActionMode.PLAY
        )
    }

    fun onPlaybackCompleted(episode: SeriesEpisodeDto, positionMs: Long, durationMs: Long) {
        onPlayerClosed(episode, positionMs, durationMs)
        if (!autoPlayNextEnabled) return

        val nextEpisode = nextEpisodeFor(episode) ?: return
        onEpisodeClickFromStart(nextEpisode)
    }

    fun willAutoPlayNextEpisode(episode: SeriesEpisodeDto?): Boolean {
        if (!autoPlayNextEnabled) return false
        episode ?: return false
        return nextEpisodeFor(episode) != null
    }

    fun playLastSeenEpisodeOrFallback() {
        val allEpisodes = episodes ?: return
        val target = lastEpisodeId?.let { id ->
            allEpisodes.values.flatten().firstOrNull { it.id == id }
        } ?: allEpisodes[selectedSeason.toString()]?.firstOrNull()
            ?: allEpisodes.values.flatten().firstOrNull()

        target?.let { onEpisodeClick(it) }
    }

    private fun loadSeriesProgress(profileId: Int, seriesId: Int) {
        lastEpisodeId = prefs.getString(seriesEpisodeIdKey(profileId, seriesId), null)
        lastEpisodeSeason = prefs.getInt(seriesSeasonKey(profileId, seriesId), -1).takeIf { it >= 0 }
        lastEpisodeNumber = prefs.getInt(seriesEpisodeNumberKey(profileId, seriesId), -1).takeIf { it >= 0 }
        lastEpisodePositionMs = prefs.getLong(seriesPositionKey(profileId, seriesId), 0L)
        primaryActionMode = prefs.getString(seriesActionModeKey(profileId, seriesId), null)
            ?.let { raw -> runCatching { PrimaryActionMode.valueOf(raw) }.getOrNull() }
            ?: if (lastEpisodePositionMs > 0L) PrimaryActionMode.CONTINUE else PrimaryActionMode.PLAY
        lastEpisodeSeason?.let { selectedSeason = it }
    }

    private fun loadEpisodeProgressMap(
        profileId: Int,
        seriesId: Int,
        episodesMap: Map<String, List<SeriesEpisodeDto>>
    ) {
        episodeProgressById.clear()
        episodeDurationById.clear()
        lastPersistedEpisodeProgressMs.clear()
        episodesMap.values.flatten().forEach { ep ->
            val epId = ep.id ?: return@forEach
            val progress = prefs.getLong(seriesEpisodeProgressKey(profileId, seriesId, epId), 0L)
            if (progress > 0L) {
                episodeProgressById[epId] = progress
                lastPersistedEpisodeProgressMs[epId] = progress
            }
            val duration = prefs.getLong(seriesEpisodeDurationKey(profileId, seriesId, epId), 0L)
            if (duration > 0L) {
                episodeDurationById[epId] = duration
            }
        }
    }

    private fun seriesEpisodeIdKey(profileId: Int, seriesId: Int): String {
        return "series_last_episode_id_${profileId}_$seriesId"
    }

    private fun seriesSeasonKey(profileId: Int, seriesId: Int): String {
        return "series_last_episode_season_${profileId}_$seriesId"
    }

    private fun seriesEpisodeNumberKey(profileId: Int, seriesId: Int): String {
        return "series_last_episode_number_${profileId}_$seriesId"
    }

    private fun seriesPositionKey(profileId: Int, seriesId: Int): String {
        return "series_last_episode_position_${profileId}_$seriesId"
    }

    private fun seriesEpisodeProgressKey(profileId: Int, seriesId: Int, episodeId: String): String {
        return "series_episode_progress_${profileId}_${seriesId}_$episodeId"
    }

    private fun seriesEpisodeDurationKey(profileId: Int, seriesId: Int, episodeId: String): String {
        return "series_episode_duration_${profileId}_${seriesId}_$episodeId"
    }

    private fun seriesActionModeKey(profileId: Int, seriesId: Int): String {
        return "series_action_mode_${profileId}_$seriesId"
    }

    private fun persistPrimaryTarget(
        profileId: Int,
        seriesId: Int,
        episode: SeriesEpisodeDto,
        positionMs: Long,
        mode: PrimaryActionMode
    ) {
        val targetEpisodeId = episode.id ?: return
        val targetSeason = episode.season
        val targetNumber = episode.episodeNum
        val safePosition = positionMs.coerceAtLeast(0L)

        lastEpisodeId = targetEpisodeId
        lastEpisodeSeason = targetSeason
        lastEpisodeNumber = targetNumber
        lastEpisodePositionMs = safePosition
        primaryActionMode = mode
        episodeProgressById[targetEpisodeId] = safePosition
        lastPersistedEpisodeProgressMs[targetEpisodeId] = safePosition

        prefs.edit().apply {
            putString(seriesEpisodeIdKey(profileId, seriesId), targetEpisodeId)
            if (targetSeason != null) putInt(seriesSeasonKey(profileId, seriesId), targetSeason) else remove(seriesSeasonKey(profileId, seriesId))
            if (targetNumber != null) putInt(seriesEpisodeNumberKey(profileId, seriesId), targetNumber) else remove(seriesEpisodeNumberKey(profileId, seriesId))
            putLong(seriesPositionKey(profileId, seriesId), safePosition)
            putLong(seriesEpisodeProgressKey(profileId, seriesId, targetEpisodeId), safePosition)
            putString(seriesActionModeKey(profileId, seriesId), mode.name)
            apply()
        }
    }

    private fun nextEpisodeFor(episode: SeriesEpisodeDto): SeriesEpisodeDto? {
        val flattened = orderedEpisodes()
        if (flattened.isEmpty()) return null
        val currentIndex = flattened.indexOfFirst { it.id == episode.id }
        if (currentIndex == -1 || currentIndex >= flattened.lastIndex) return null
        return flattened[currentIndex + 1]
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val seriesId = seriesStream?.seriesId ?: return@launch
            seriesRepository.toggleFavorite(profile.id, seriesId, "series")
        }
    }

    fun checkAndNavigateToSeries(tmdbId: Int, title: String?, onSingleResult: (Int) -> Unit) {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            
            val locals = seriesRepository.getSeriesListByTmdbId(profile.id, tmdbId.toString())

            when {
                locals.isEmpty() -> {}
                locals.size == 1 -> {
                    onSingleResult(locals.first().seriesId)
                }
                else -> {
                    variationSelectorTitle = title ?: seriesStream?.name ?: "Seleccionar version"
                    val currentSeriesId = seriesStream?.seriesId
                    variations = locals.map { SeriesVariantOption(it, it.seriesId == currentSeriesId) }
                    showVariationSelector = true
                }
            }
        }
    }

    fun handleEpisodeDownloadClick(episode: SeriesEpisodeDto) {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val series = seriesStream ?: return@launch
            val currentStatus = episodeDownloads.first()[episode.id]
            Log.d(TAG, "handleEpisodeDownloadClick episodeId=${episode.id} status=${currentStatus?.status} filePath=${currentStatus?.filePath}")

            when (currentStatus?.status) {
                "downloading", "queued", "retrying" -> {
                    Log.d(TAG, "pause_existing id=${currentStatus.id}")
                    downloadRepository.pauseDownload(currentStatus.id)
                }
                "paused", "error" -> {
                    Log.d(TAG, "resume_existing id=${currentStatus.id}")
                    downloadRepository.resumeDownload(currentStatus)
                }
                "completed" -> {
                    Log.d(TAG, "already_completed id=${currentStatus.id}")
                    // Ya descargado
                }
                null -> {
                    val url = "${profile.url}series/${profile.username}/${profile.password}/${episode.id}.${episode.containerExtension ?: "mp4"}"
                    val downloadTitle = buildEpisodeDisplayTitle(episode, series.name)
                    
                    val sanitizedName = com.stream.nextftv.data.utils.StringUtils.sanitizeFileName(series.name)
                    val s = episode.season?.toString()?.padStart(2, '0') ?: "00"
                    val e = episode.episodeNum?.toString()?.padStart(2, '0') ?: "00"
                    val fileName = "${series.seriesId}_S${s}E${e}_$sanitizedName.${episode.containerExtension ?: "mp4"}"
                    Log.d(TAG, "enqueue_new episodeId=${episode.id} url=$url fileName=$fileName")
                    
                    downloadRepository.addDownload(
                        profile = profile, 
                        streamId = episode.id?.toIntOrNull() ?: 0, 
                        title = downloadTitle, 
                        url = url, 
                        type = "episode", 
                        fileName = fileName,
                        posterUrl = series.cover,
                        parentId = series.seriesId,
                        parentName = series.name
                    )
                }
            }
        }
    }

    private fun preferredLocalSeriesCard(
        tmdbItem: com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto,
        localVariants: List<SeriesStreamEntity>
    ): com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto {
        if (localVariants.size != 1) return tmdbItem
        val local = localVariants.first()
        return tmdbItem.copy(
            title = null,
            tvName = local.name,
            posterPath = local.cover ?: tmdbItem.posterPath
        )
    }

    fun isFavorite(): Flow<Boolean> {
        val profile = activeProfile ?: return flowOf(false)
        val seriesId = seriesStream?.seriesId ?: return flowOf(false)
        return seriesRepository.isFavorite(profile.id, seriesId, "series")
    }
}
