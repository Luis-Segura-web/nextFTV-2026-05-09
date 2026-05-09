package com.stream.nextftv.presentation.screens.moviedetail

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.remote.tmdb.TmdbMovieDetailsDto
import com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto
import com.stream.nextftv.data.remote.tmdb.TmdbCollectionDto
import com.stream.nextftv.data.remote.VodInfoDto
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.VodRepository
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
class MovieDetailViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val vodRepository: VodRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    private companion object {
        const val TAG = "MovieDetailDownload"
    }
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
        val isActual: Boolean,
        val localVariants: List<VodStreamEntity>
    )
    data class MovieVariantOption(
        val source: VodStreamEntity,
        val isCurrent: Boolean
    )
    var collectionItems by mutableStateOf<List<CollectionItem>>(emptyList())
    var collectionTitle by mutableStateOf("")

    var variations by mutableStateOf<List<MovieVariantOption>>(emptyList())
    var showVariationSelector by mutableStateOf(false)
    var variationSelectorTitle by mutableStateOf("Seleccionar version")

    var isPlayerActive by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
    var localFilePath by mutableStateOf<String?>(null)
    var lastMoviePositionMs by mutableStateOf(0L)
    var lastMovieDurationMs by mutableStateOf(0L)
    private var loadGeneration = 0

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
            val generation = ++loadGeneration
            val startedAt = SystemClock.elapsedRealtime()
            isLoading = true
            movieDetails = null
            movieCollection = null
            xtreamInfo = null
            recommendedMovies = emptyList()
            similarMovies = emptyList()
            collectionItems = emptyList()
            collectionTitle = ""
            try {
                val profiles = profileRepository.getProfiles().first()
                activeProfile = profiles.find { it.isActive }
                val profile = activeProfile ?: throw Exception("No hay perfil activo")
                Log.d(TAG, "loadMovie start streamId=$streamId t=${elapsedMs(startedAt)}ms")

                val stream = vodRepository.getStream(profile.id, streamId)
                    ?: throw Exception("Película no encontrada")
                if (generation != loadGeneration) return@launch
                vodStream = stream
                lastMoviePositionMs = readMovieProgress(stream.streamId, profile.id)
                lastMovieDurationMs = readMovieDuration(stream.streamId, profile.id)
                Log.d(TAG, "loadMovie stream_resolved streamId=$streamId t=${elapsedMs(startedAt)}ms")
                
                val downloads = downloadRepository.getDownloads(profile.id).first()
                val download = downloads.find { it.streamId == streamId && it.type == "movie" }
                if (
                    download?.status == "completed" &&
                    File(download.filePath).exists() &&
                    File(download.filePath).length() > 0L
                ) {
                    localFilePath = download.filePath
                } else {
                    localFilePath = null
                }

                if (autoPlay) isPlayerActive = true
                isLoading = false
                Log.d(TAG, "loadMovie ui_ready streamId=$streamId t=${elapsedMs(startedAt)}ms")

                viewModelScope.launch {
                    val xtreamStartedAt = SystemClock.elapsedRealtime()
                    val xtreamResult = vodRepository.getVodInfo(profile, streamId)
                    if (generation != loadGeneration) return@launch
                    xtreamInfo = xtreamResult.getOrNull()
                    Log.d(
                        TAG,
                        "loadMovie xtream_done streamId=$streamId success=${xtreamResult.isSuccess} request=${elapsedMs(xtreamStartedAt)}ms total=${elapsedMs(startedAt)}ms"
                    )
                }

                viewModelScope.launch {
                    val tmdbStartedAt = SystemClock.elapsedRealtime()
                    val tmdbResult = vodRepository.getMovieDetails(stream)
                    if (generation != loadGeneration) return@launch
                    tmdbResult.onSuccess { details ->
                        movieDetails = details
                        Log.d(
                            TAG,
                            "loadMovie tmdb_done streamId=$streamId recommendations=${details.recommendations?.results?.size ?: 0} similar=${details.similar?.results?.size ?: 0} request=${elapsedMs(tmdbStartedAt)}ms total=${elapsedMs(startedAt)}ms"
                        )

                        details.recommendations?.results?.let { list ->
                            recommendedMovies = list.mapNotNull { tmdbMovie ->
                                val locals = vodRepository.getStreamsByTmdbId(profile.id, tmdbMovie.id.toString())
                                locals.firstOrNull()?.let { local ->
                                    preferredLocalMovieCard(tmdbMovie, locals) to local.streamId
                                }
                            }
                        }

                        details.similar?.results?.let { list ->
                            similarMovies = list.mapNotNull { tmdbMovie ->
                                val locals = vodRepository.getStreamsByTmdbId(profile.id, tmdbMovie.id.toString())
                                locals.firstOrNull()?.let { local ->
                                    preferredLocalMovieCard(tmdbMovie, locals) to local.streamId
                                }
                            }
                        }

                        details.belongsToCollection?.id?.let { collId ->
                            val collectionStartedAt = SystemClock.elapsedRealtime()
                            vodRepository.getMovieCollection(collId).onSuccess { coll ->
                                if (generation != loadGeneration) return@onSuccess
                                movieCollection = coll
                                collectionTitle = coll.name.orEmpty()
                                collectionItems = coll.parts
                                    ?.sortedWith(
                                        compareBy<TmdbMovieShortDto> {
                                            it.releaseDate?.takeIf { value -> value.isNotBlank() }
                                        }.thenBy { it.title.orEmpty() }
                                    )
                                    ?.map { part ->
                                        val localVariants = vodRepository
                                            .getStreamsByTmdbId(profile.id, part.id.toString())
                                            .distinctBy { it.streamId }

                                        CollectionItem(
                                            tmdbMovie = preferredLocalMovieCard(part, localVariants),
                                            isActual = localVariants.any { it.streamId == streamId } || part.id == stream.tmdbId,
                                            localVariants = localVariants
                                        )
                                    } ?: emptyList()
                                Log.d(
                                    TAG,
                                    "loadMovie collection_done streamId=$streamId items=${collectionItems.size} request=${elapsedMs(collectionStartedAt)}ms total=${elapsedMs(startedAt)}ms"
                                )
                            }
                        }

                        if (collectionItems.isEmpty()) {
                            val currentTmdbId = stream.tmdbId
                            val allVariations = if (currentTmdbId != null) {
                                vodRepository.getStreamsByTmdbId(profile.id, currentTmdbId.toString()).distinctBy { it.streamId }
                            } else {
                                emptyList()
                            }

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
                                        isActual = variation.streamId == streamId,
                                        localVariants = listOf(variation)
                                    )
                                }
                                Log.d(
                                    TAG,
                                    "loadMovie local_variations_done streamId=$streamId items=${collectionItems.size} total=${elapsedMs(startedAt)}ms"
                                )
                            }
                        }
                    }.onFailure { error ->
                        Log.w(
                            TAG,
                            "loadMovie tmdb_failed streamId=$streamId total=${elapsedMs(startedAt)}ms error=${error.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                // error handling
                Log.e(TAG, "loadMovie failed streamId=$streamId total=${elapsedMs(startedAt)}ms error=${e.message}", e)
            } finally {
                if (vodStream == null || generation != loadGeneration) {
                    isLoading = false
                }
            }
        }
    }

    private fun elapsedMs(startedAt: Long): Long = SystemClock.elapsedRealtime() - startedAt

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

    fun checkAndNavigateToMovie(tmdbId: Int, title: String?, onSingleResult: (Int) -> Unit) {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            
            // Resolver solo por TMDB ID para evitar agrupaciones ambiguas por nombre.
            val locals = vodRepository.getStreamsByTmdbId(profile.id, tmdbId.toString())

            when {
                locals.isEmpty() -> { /* No debería pasar si se muestra en la lista */ }
                locals.size == 1 -> {
                    // Solo una opción, ir directo
                    onSingleResult(locals.first().streamId)
                }
                else -> {
                    // Múltiples opciones, mostrar selector
                    val currentStreamId = vodStream?.streamId
                    variationSelectorTitle = title ?: vodStream?.name ?: "Seleccionar version"
                    variations = locals.map { MovieVariantOption(it, it.streamId == currentStreamId) }
                    showVariationSelector = true
                }
            }
        }
    }

    fun onCollectionItemClick(item: CollectionItem, onSingleResult: (Int) -> Unit) {
        val currentStreamId = vodStream?.streamId
        val alternativeVariants = item.localVariants.filterNot { it.streamId == currentStreamId }

        when {
            item.isActual && alternativeVariants.isEmpty() -> Unit
            item.isActual -> {
                variationSelectorTitle = item.tmdbMovie.title ?: item.tmdbMovie.tvName ?: vodStream?.name ?: "Seleccionar version"
                variations = item.localVariants.map { MovieVariantOption(it, it.streamId == currentStreamId) }
                showVariationSelector = true
            }
            item.localVariants.isEmpty() -> Unit
            item.localVariants.size == 1 -> onSingleResult(item.localVariants.first().streamId)
            else -> {
                variationSelectorTitle = item.tmdbMovie.title ?: item.tmdbMovie.tvName ?: vodStream?.name ?: "Seleccionar version"
                variations = item.localVariants.map { MovieVariantOption(it, it.streamId == currentStreamId) }
                showVariationSelector = true
            }
        }
    }

    fun handleDownloadClick() {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val movie = vodStream ?: return@launch
            val currentStatus = downloadState.first()
            Log.d(TAG, "handleDownloadClick streamId=${movie.streamId} status=${currentStatus?.status} filePath=${currentStatus?.filePath}")

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
                    // Ya está descargado, no hacer nada o abrir
                }
                null -> {
                    val url = "${profile.url}movie/${profile.username}/${profile.password}/${movie.streamId}.${movie.containerExtension ?: "mp4"}"
                    val sanitizedName = com.stream.nextftv.data.utils.StringUtils.sanitizeFileName(movie.name)
                    val fileName = "${movie.streamId}_$sanitizedName.${movie.containerExtension ?: "mp4"}"
                    Log.d(TAG, "enqueue_new streamId=${movie.streamId} url=$url fileName=$fileName")
                    
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

    private fun preferredLocalMovieCard(
        tmdbMovie: TmdbMovieShortDto,
        localVariants: List<VodStreamEntity>
    ): TmdbMovieShortDto {
        if (localVariants.size != 1) return tmdbMovie
        val local = localVariants.first()
        return tmdbMovie.copy(
            title = local.name,
            posterPath = local.streamIcon ?: tmdbMovie.posterPath
        )
    }
}
