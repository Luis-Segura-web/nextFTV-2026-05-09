package com.stream.iptvrevolut.presentation.screens.moviedetail

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.iptvrevolut.data.local.entity.vod.VodStreamEntity
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.iptvrevolut.data.remote.tmdb.TmdbMovieShortDto
import com.stream.iptvrevolut.data.remote.tmdb.TmdbCollectionDto
import com.stream.iptvrevolut.data.remote.VodInfoDto
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.VodRepository
import com.stream.iptvrevolut.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val vodRepository: VodRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    private val prefs by lazy { context.getSharedPreferences("playback_resume_prefs", Context.MODE_PRIVATE) }

    var movieDetails by mutableStateOf<TmdbMovieDetailsDto?>(null)
    var movieCollection by mutableStateOf<TmdbCollectionDto?>(null)
    var xtreamInfo by mutableStateOf<VodInfoDto?>(null)
    var vodStream by mutableStateOf<VodStreamEntity?>(null)
    var activeProfile by mutableStateOf<ServerProfile?>(null)
    
    var recommendedMovies by mutableStateOf<List<Pair<TmdbMovieShortDto, Int>>>(emptyList())
    var similarMovies by mutableStateOf<List<Pair<TmdbMovieShortDto, Int>>>(emptyList())
    
    // Nueva estructura para la colección/variaciones
    data class CollectionItem(
        val tmdbMovie: TmdbMovieShortDto,
        val localId: Int?,
        val isActual: Boolean,
        val isFound: Boolean
    )
    var collectionItems by mutableStateOf<List<CollectionItem>>(emptyList())
    var collectionTitle by mutableStateOf("")

    var variations by mutableStateOf<List<VodStreamEntity>>(emptyList())
    var showVariationSelector by mutableStateOf(false)

    var isPlayerActive by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
    var localFilePath by mutableStateOf<String?>(null)
    var lastMoviePositionMs by mutableStateOf(0L)
    var lastMovieDurationMs by mutableStateOf(0L)

    //snapshotFlow asegura que cuando vodStream cambie, se vuelva a calcular el flujo de descarga
    val downloadState: Flow<DownloadEntity?> = combine(
        profileRepository.getProfiles().map { it.find { p -> p.isActive }?.id },
        snapshotFlow { vodStream }
    ) { profileId, currentStream ->
        profileId to currentStream
    }.flatMapLatest { (profileId, currentStream) ->
        if (profileId != null && currentStream != null) {
            downloadRepository.getDownloads(profileId).map { downloads ->
                downloads.find { it.streamId == currentStream.streamId && it.type == "movie" }
            }
        } else flowOf(null)
    }

    fun loadMovie(streamId: Int, autoPlay: Boolean) {
        viewModelScope.launch {
            isLoading = true
            recommendedMovies = emptyList()
            similarMovies = emptyList()
            collectionItems = emptyList()
            collectionTitle = ""
            try {
                val profiles = profileRepository.getProfiles().first()
                activeProfile = profiles.find { it.isActive }
                val profile = activeProfile ?: throw Exception("No hay perfil activo")
                
                val streams = vodRepository.getStreams(profile.id, null).first()
                val stream = streams.find { it.streamId == streamId } ?: throw Exception("Película no encontrada")
                vodStream = stream
                lastMoviePositionMs = readMovieProgress(stream.streamId, profile.id)
                lastMovieDurationMs = readMovieDuration(stream.streamId, profile.id)
                
                val downloads = downloadRepository.getDownloads(profile.id).first()
                val download = downloads.find { it.streamId == streamId && it.type == "movie" && it.status == "completed" }
                if (download != null && File(download.filePath).exists()) {
                    localFilePath = download.filePath
                }

                if (autoPlay) isPlayerActive = true

                val xtreamResult = vodRepository.getVodInfo(profile, streamId)
                xtreamInfo = xtreamResult.getOrNull()
                
                val tmdbResult = vodRepository.getMovieDetails(stream)
                tmdbResult.onSuccess { details ->
                    movieDetails = details
                    
                    // Filtrar Recomendadas
                    details.recommendations?.results?.let { list ->
                        recommendedMovies = list.mapNotNull { tmdbMovie ->
                            // 1. Intentar por TMDB ID
                            var locals = vodRepository.getStreamsByTmdbId(profile.id, tmdbMovie.id.toString())
                            
                            // 2. Fallback: Intentar por Nombre (Normalizado)
                            if (locals.isEmpty()) {
                                val normalizedTitle = com.stream.iptvrevolut.data.utils.StringUtils.normalize(tmdbMovie.title ?: "")
                                locals = vodRepository.getStreamsByName(profile.id, normalizedTitle)
                            }
                            
                            locals.firstOrNull()?.let { tmdbMovie to it.streamId }
                        }
                    }

                    // Filtrar Similares
                    details.similar?.results?.let { list ->
                        similarMovies = list.mapNotNull { tmdbMovie ->
                            // 1. Intentar por TMDB ID
                            var locals = vodRepository.getStreamsByTmdbId(profile.id, tmdbMovie.id.toString())
                            
                            // 2. Fallback: Intentar por Nombre
                            if (locals.isEmpty()) {
                                val normalizedTitle = com.stream.iptvrevolut.data.utils.StringUtils.normalize(tmdbMovie.title ?: "")
                                locals = vodRepository.getStreamsByName(profile.id, normalizedTitle)
                            }
                            
                            locals.firstOrNull()?.let { tmdbMovie to it.streamId }
                        }
                    }

                    // Procesar Colección
                    details.belongsToCollection?.id?.let { collId ->
                        vodRepository.getMovieCollection(collId).onSuccess { coll ->
                            movieCollection = coll
                            collectionTitle = "Saga: ${coll.name}"
                            collectionItems = coll.parts?.map { part ->
                                var localId: Int? = null
                                // Buscar por TMDB
                                val byId = vodRepository.getStreamsByTmdbId(profile.id, part.id.toString())
                                if (byId.isNotEmpty()) {
                                    localId = byId.first().streamId
                                } else {
                                    // Buscar por nombre
                                    val byName = vodRepository.getStreamsByName(profile.id, com.stream.iptvrevolut.data.utils.StringUtils.normalize(part.title ?: ""))
                                    if (byName.isNotEmpty()) localId = byName.first().streamId
                                }

                                CollectionItem(
                                    tmdbMovie = part,
                                    localId = localId,
                                    isActual = localId == streamId || part.id == stream.tmdbId,
                                    isFound = localId != null
                                )
                            } ?: emptyList()
                        }
                    }

                    // Fallback: Si no hay colección pero hay otras versiones del mismo nombre/TMDB
                    if (collectionItems.isEmpty()) {
                        val currentTmdbId = stream.tmdbId
                        val sameTmdb = if (currentTmdbId != null) vodRepository.getStreamsByTmdbId(profile.id, currentTmdbId.toString()) else emptyList()
                        val sameName = vodRepository.getStreamsByName(profile.id, stream.normalizedName)
                        val allVariations = (sameTmdb + sameName).distinctBy { it.streamId }
                        
                        if (allVariations.size > 1) {
                            collectionTitle = "Versiones Disponibles"
                            collectionItems = allVariations.map { variation ->
                                CollectionItem(
                                    tmdbMovie = TmdbMovieShortDto(
                                        id = variation.tmdbId ?: 0,
                                        title = variation.name,
                                        tvName = null,
                                        posterPath = variation.streamIcon
                                    ),
                                    localId = variation.streamId,
                                    isActual = variation.streamId == streamId,
                                    isFound = true
                                )
                            }
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

    fun onPlaybackStarted(streamId: Int) {
        viewModelScope.launch {
            activeProfile?.let { profile ->
                vodRepository.addToRecents(profile.id, streamId, "vod")
            }
        }
    }

    fun updateMovieProgress(streamId: Int, positionMs: Long) {
        val profileId = activeProfile?.id ?: return
        if (positionMs < 0L) return
        lastMoviePositionMs = positionMs
        prefs.edit().putLong(movieProgressKey(profileId, streamId), positionMs).apply()
    }

    fun updateMovieDuration(streamId: Int, durationMs: Long) {
        val profileId = activeProfile?.id ?: return
        val safeDuration = durationMs.coerceAtLeast(0L)
        if (safeDuration == 0L) return
        lastMovieDurationMs = safeDuration
        prefs.edit().putLong(movieDurationKey(profileId, streamId), safeDuration).apply()
    }

    fun onPlayerClosed(streamId: Int, positionMs: Long, durationMs: Long) {
        val profileId = activeProfile?.id ?: return
        val safePosition = positionMs.coerceAtLeast(0L)
        val resolvedDuration = durationMs.coerceAtLeast(0L).takeIf { it > 0L }
            ?: readMovieDuration(streamId, profileId)
        val isCompleted = resolvedDuration > 0L && safePosition >= resolvedDuration

        if (isCompleted) {
            clearMovieProgress(streamId, profileId)
            return
        }

        lastMoviePositionMs = safePosition
        if (resolvedDuration > 0L) {
            lastMovieDurationMs = resolvedDuration
        }
        prefs.edit()
            .putLong(movieProgressKey(profileId, streamId), safePosition)
            .putLong(movieDurationKey(profileId, streamId), resolvedDuration)
            .apply()
    }

    private fun movieProgressKey(profileId: Int, streamId: Int): String {
        return "movie_progress_${profileId}_$streamId"
    }

    private fun movieDurationKey(profileId: Int, streamId: Int): String {
        return "movie_duration_${profileId}_$streamId"
    }

    private fun readMovieProgress(streamId: Int, profileId: Int): Long {
        return prefs.getLong(movieProgressKey(profileId, streamId), 0L)
    }

    private fun readMovieDuration(streamId: Int, profileId: Int): Long {
        return prefs.getLong(movieDurationKey(profileId, streamId), 0L)
    }

    private fun clearMovieProgress(streamId: Int, profileId: Int) {
        lastMoviePositionMs = 0L
        prefs.edit().putLong(movieProgressKey(profileId, streamId), 0L).apply()
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val streamId = vodStream?.streamId ?: return@launch
            vodRepository.toggleFavorite(profile.id, streamId, "vod")
        }
    }

    fun onNextMovie() {
        viewModelScope.launch {
            val current = vodStream ?: return@launch
            val profile = activeProfile ?: return@launch
            val allMovies = vodRepository.getStreams(profile.id, current.categoryId).first()
            val currentIndex = allMovies.indexOfFirst { it.streamId == current.streamId }
            if (currentIndex != -1 && currentIndex < allMovies.size - 1) {
                loadMovie(allMovies[currentIndex + 1].streamId, true)
            }
        }
    }

    fun onPreviousMovie() {
        viewModelScope.launch {
            val current = vodStream ?: return@launch
            val profile = activeProfile ?: return@launch
            val allMovies = vodRepository.getStreams(profile.id, current.categoryId).first()
            val currentIndex = allMovies.indexOfFirst { it.streamId == current.streamId }
            if (currentIndex > 0) {
                loadMovie(allMovies[currentIndex - 1].streamId, true)
            }
        }
    }

    fun checkAndNavigateToMovie(tmdbId: Int, title: String?, onSingleResult: (Int) -> Unit) {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            
            // 1. Buscar todas las variaciones locales
            var locals = vodRepository.getStreamsByTmdbId(profile.id, tmdbId.toString())
            if (locals.isEmpty() && title != null) {
                val normalized = com.stream.iptvrevolut.data.utils.StringUtils.normalize(title)
                locals = vodRepository.getStreamsByName(profile.id, normalized)
            }

            when {
                locals.isEmpty() -> { /* No debería pasar si se muestra en la lista */ }
                locals.size == 1 -> {
                    // Solo una opción, ir directo
                    onSingleResult(locals.first().streamId)
                }
                else -> {
                    // Múltiples opciones, mostrar selector
                    variations = locals
                    showVariationSelector = true
                }
            }
        }
    }

    fun handleDownloadClick() {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val movie = vodStream ?: return@launch
            val currentStatus = downloadState.first()

            when (currentStatus?.status) {
                "downloading", "queued", "retrying" -> {
                    downloadRepository.pauseDownload(currentStatus.id)
                }
                "paused", "error" -> {
                    downloadRepository.resumeDownload(currentStatus)
                }
                "completed" -> {
                    // Ya está descargado, no hacer nada o abrir
                }
                null -> {
                    val url = "${profile.url}movie/${profile.username}/${profile.password}/${movie.streamId}.${movie.containerExtension ?: "mp4"}"
                    val sanitizedName = com.stream.iptvrevolut.data.utils.StringUtils.sanitizeFileName(movie.name)
                    val fileName = "${movie.streamId}_$sanitizedName.${movie.containerExtension ?: "mp4"}"
                    
                    downloadRepository.addDownload(
                        profile = profile, 
                        streamId = movie.streamId, 
                        title = movie.name, 
                        url = url, 
                        type = "movie", 
                        fileName = fileName,
                        posterUrl = movie.streamIcon
                    )
                }
            }
        }
    }

    fun isFavorite(): Flow<Boolean> {
        val profile = activeProfile ?: return flowOf(false)
        val streamId = vodStream?.streamId ?: return flowOf(false)
        return vodRepository.isFavorite(profile.id, streamId, "vod")
    }
}
