package com.stream.iptvrevolut.presentation.screens.seriesdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity
import com.stream.iptvrevolut.data.remote.SeriesEpisodeDto
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.SeriesRepository
import com.stream.iptvrevolut.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val seriesRepository: SeriesRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    var seriesDetails by mutableStateOf<TmdbMovieDetailsDto?>(null)
    var seriesStream by mutableStateOf<SeriesStreamEntity?>(null)
    var episodes by mutableStateOf<Map<String, List<SeriesEpisodeDto>>?>(null)
    var activeProfile by mutableStateOf<ServerProfile?>(null)
    
    var recommendedSeries by mutableStateOf<List<Pair<com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieShortDto, Int>>>(emptyList())
    var similarSeries by mutableStateOf<List<Pair<com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieShortDto, Int>>>(emptyList())
    
    // Variaciones para series
    data class SeriesVariationItem(
        val tmdbItem: com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieShortDto,
        val localId: Int,
        val isActual: Boolean
    )
    var variationItems by mutableStateOf<List<SeriesVariationItem>>(emptyList())

    var variations by mutableStateOf<List<SeriesStreamEntity>>(emptyList())
    var showVariationSelector by mutableStateOf(false)

    var isPlayerActive by mutableStateOf(false)
    var isPlayerLoading by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
    var localEpisodePaths = mutableStateMapOf<String, String>()

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

    fun loadSeries(seriesId: Int, initialEpisodeId: String?, autoPlay: Boolean) {
        viewModelScope.launch {
            isLoading = true
            recommendedSeries = emptyList()
            similarSeries = emptyList()
            variationItems = emptyList()
            try {
                val profiles = profileRepository.getProfiles().first()
                activeProfile = profiles.find { it.isActive }
                val profile = activeProfile ?: return@launch
                
                val streams = seriesRepository.getSeries(profile.id, null).first()
                val stream = streams.find { it.seriesId == seriesId }?.let { s ->
                    seriesStream = s
                    
                    val episodesResult = seriesRepository.getSeriesEpisodes(profile, seriesId)
                    episodesResult.onSuccess { eps ->
                        episodes = eps
                        
                        val downloads = downloadRepository.getDownloads(profile.id).first()
                        eps.values.flatten().forEach { ep ->
                            downloads.find { it.streamId == (ep.id?.toIntOrNull() ?: 0) && it.type == "episode" && it.status == "completed" }?.let { d ->
                                if (File(d.filePath).exists()) {
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
                    }

                    val tmdbResult = seriesRepository.getSeriesDetails(s)
                    tmdbResult.onSuccess { details ->
                        seriesDetails = details
                        
                        // Filtrar Recomendadas
                        details.recommendations?.results?.let { list ->
                            recommendedSeries = list.mapNotNull { tmdbItem ->
                                // 1. Intentar por TMDB ID
                                var locals = seriesRepository.getSeriesListByTmdbId(profile.id, tmdbItem.id.toString())
                                
                                // 2. Fallback: Intentar por Nombre (Normalizado)
                                if (locals.isEmpty()) {
                                    val normalizedName = com.stream.iptvrevolut.data.utils.StringUtils.normalize(tmdbItem.tvName ?: tmdbItem.title ?: "")
                                    locals = seriesRepository.getSeriesListByName(profile.id, normalizedName)
                                }
                                
                                locals.firstOrNull()?.let { tmdbItem to it.seriesId }
                            }
                        }

                        // Filtrar Similares
                        details.similar?.results?.let { list ->
                            similarSeries = list.mapNotNull { tmdbItem ->
                                // 1. Intentar por TMDB ID
                                var locals = seriesRepository.getSeriesListByTmdbId(profile.id, tmdbItem.id.toString())
                                
                                // 2. Fallback: Intentar por Nombre
                                if (locals.isEmpty()) {
                                    val normalizedName = com.stream.iptvrevolut.data.utils.StringUtils.normalize(tmdbItem.tvName ?: tmdbItem.title ?: "")
                                    locals = seriesRepository.getSeriesListByName(profile.id, normalizedName)
                                }
                                
                                locals.firstOrNull()?.let { tmdbItem to it.seriesId }
                            }
                        }
                    }

                    // Buscar variaciones locales para la serie actual
                    val currentTmdbId = s.tmdbId
                    val sameTmdb = if (currentTmdbId != null) seriesRepository.getSeriesListByTmdbId(profile.id, currentTmdbId.toString()) else emptyList()
                    val sameName = seriesRepository.getSeriesListByName(profile.id, s.normalizedName)
                    val allVariations = (sameTmdb + sameName).distinctBy { it.seriesId }
                    
                    if (allVariations.size > 1) {
                        variationItems = allVariations.map { variation ->
                            SeriesVariationItem(
                                tmdbItem = com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieShortDto(
                                    id = variation.tmdbId ?: 0,
                                    title = null,
                                    tvName = variation.name,
                                    posterPath = variation.cover
                                ),
                                localId = variation.seriesId,
                                isActual = variation.seriesId == seriesId
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // error handling
            } finally {
                isLoading = false
            }
        }
    }

    var selectedSeason by mutableStateOf(1)
    var currentEpisode by mutableStateOf<SeriesEpisodeDto?>(null)

    fun onEpisodeClick(episode: SeriesEpisodeDto) {
        selectedSeason = episode.season ?: selectedSeason
        currentEpisode = episode
        isPlayerActive = true 
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
            
            var locals = seriesRepository.getSeriesListByTmdbId(profile.id, tmdbId.toString())
            if (locals.isEmpty() && title != null) {
                val normalized = com.stream.iptvrevolut.data.utils.StringUtils.normalize(title)
                locals = seriesRepository.getSeriesListByName(profile.id, normalized)
            }

            when {
                locals.isEmpty() -> {}
                locals.size == 1 -> {
                    onSingleResult(locals.first().seriesId)
                }
                else -> {
                    variations = locals
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

            when (currentStatus?.status) {
                "downloading", "queued", "retrying" -> {
                    downloadRepository.pauseDownload(currentStatus.id)
                }
                "paused", "error" -> {
                    downloadRepository.resumeDownload(currentStatus)
                }
                "completed" -> {
                    // Ya descargado
                }
                null -> {
                    val url = "${profile.url}series/${profile.username}/${profile.password}/${episode.id}.${episode.containerExtension ?: "mp4"}"
                    val downloadTitle = "${series.name} - S${episode.season}E${episode.episodeNum}"
                    
                    val sanitizedName = com.stream.iptvrevolut.data.utils.StringUtils.sanitizeFileName(series.name)
                    val s = episode.season?.toString()?.padStart(2, '0') ?: "00"
                    val e = episode.episodeNum?.toString()?.padStart(2, '0') ?: "00"
                    val fileName = "${series.seriesId}_S${s}E${e}_$sanitizedName.${episode.containerExtension ?: "mp4"}"
                    
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

    fun isFavorite(): Flow<Boolean> {
        val profile = activeProfile ?: return flowOf(false)
        val seriesId = seriesStream?.seriesId ?: return flowOf(false)
        return seriesRepository.isFavorite(profile.id, seriesId, "series")
    }
}
