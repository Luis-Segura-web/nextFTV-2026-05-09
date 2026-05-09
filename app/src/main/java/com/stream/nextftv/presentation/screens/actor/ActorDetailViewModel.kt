package com.stream.nextftv.presentation.screens.actor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.remote.tmdb.TmdbApiService
import com.stream.nextftv.data.remote.tmdb.TmdbCombinedCreditDto
import com.stream.nextftv.data.remote.tmdb.TmdbMovieShortDto
import com.stream.nextftv.data.remote.tmdb.TmdbPersonDetailsDto
import com.stream.nextftv.data.remote.tmdb.getPersonDetailsWithFallback
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.SeriesRepository
import com.stream.nextftv.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActorDetailViewModel @Inject constructor(
    private val tmdbApi: TmdbApiService,
    private val profileRepository: ProfileRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository
) : ViewModel() {
    data class MovieVariantOption(
        val source: VodStreamEntity,
        val isCurrent: Boolean = false
    )

    data class SeriesVariantOption(
        val source: SeriesStreamEntity,
        val isCurrent: Boolean = false
    )

    data class LocalMovieCredit(
        val tmdbItem: TmdbMovieShortDto,
        val streamId: Int,
        val character: String?,
        val overview: String?,
        val year: String?
    )

    data class LocalSeriesCredit(
        val tmdbItem: TmdbMovieShortDto,
        val seriesId: Int,
        val character: String?,
        val overview: String?,
        val year: String?
    )

    var activeProfile by mutableStateOf<ServerProfile?>(null)
        private set

    var actor by mutableStateOf<TmdbPersonDetailsDto?>(null)
        private set

    var movieCredits by mutableStateOf<List<LocalMovieCredit>>(emptyList())
        private set

    var seriesCredits by mutableStateOf<List<LocalSeriesCredit>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var variationSelectorTitle by mutableStateOf("Seleccionar version")
        private set

    var movieVariations by mutableStateOf<List<MovieVariantOption>>(emptyList())
        private set

    var seriesVariations by mutableStateOf<List<SeriesVariantOption>>(emptyList())
        private set

    var showMovieVariationSelector by mutableStateOf(false)
        private set

    var showSeriesVariationSelector by mutableStateOf(false)
        private set

    private var currentActorId: Int? = null

    init {
        viewModelScope.launch {
            profileRepository.getProfiles().collectLatest { profiles ->
                activeProfile = profiles.find { it.isActive }
                val pendingActorId = currentActorId
                if (pendingActorId != null && actor == null && !isLoading && activeProfile != null) {
                    loadActor(pendingActorId)
                }
            }
        }
    }

    fun loadActor(actorId: Int) {
        if (currentActorId == actorId && (actor != null || isLoading)) return
        currentActorId = actorId
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            isLoading = true
            errorMessage = null
            movieCredits = emptyList()
            seriesCredits = emptyList()
            movieVariations = emptyList()
            seriesVariations = emptyList()
            showMovieVariationSelector = false
            showSeriesVariationSelector = false

            runCatching {
                tmdbApi.getPersonDetailsWithFallback(actorId)
            }.onSuccess { details ->
                actor = details
                val credits = details.combinedCredits?.cast.orEmpty()
                movieCredits = buildMovieCredits(profile.id, credits)
                seriesCredits = buildSeriesCredits(profile.id, credits)
            }.onFailure { error ->
                actor = null
                errorMessage = error.message ?: "No se pudo cargar la filmografía"
            }

            isLoading = false
        }
    }

    private suspend fun buildMovieCredits(
        profileId: Int,
        credits: List<TmdbCombinedCreditDto>
    ): List<LocalMovieCredit> {
        val movieCredits = credits.filter { it.mediaType == "movie" }.distinctBy { it.id }
        if (movieCredits.isEmpty()) return emptyList()
        val localMovies = vodRepository.getStreamsByTmdbIds(profileId, movieCredits.map { it.id })
        val localByTmdbId = localMovies.groupBy { it.tmdbId }
        return movieCredits.mapNotNull { credit ->
            val local = localByTmdbId[credit.id]?.firstOrNull() ?: return@mapNotNull null
            LocalMovieCredit(
                tmdbItem = TmdbMovieShortDto(
                    id = credit.id,
                    title = credit.title,
                    tvName = null,
                    posterPath = local.streamIcon ?: credit.posterPath,
                    releaseDate = credit.releaseDate
                ),
                streamId = local.streamId,
                character = credit.character,
                overview = credit.overview,
                year = credit.releaseDate?.take(4)
            )
        }.sortedByDescending { it.year.orEmpty() }
    }

    fun onMovieCreditClick(
        credit: LocalMovieCredit,
        onSingleResult: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val locals = vodRepository.getStreamsByTmdbId(profile.id, credit.tmdbItem.id.toString())

            when {
                locals.isEmpty() -> onSingleResult(credit.streamId)
                locals.size == 1 -> onSingleResult(locals.first().streamId)
                else -> {
                    variationSelectorTitle = credit.tmdbItem.title ?: "Seleccionar version"
                    movieVariations = locals.map { MovieVariantOption(it) }
                    showMovieVariationSelector = true
                }
            }
        }
    }

    private suspend fun buildSeriesCredits(
        profileId: Int,
        credits: List<TmdbCombinedCreditDto>
    ): List<LocalSeriesCredit> {
        val tvCredits = credits.filter { it.mediaType == "tv" }.distinctBy { it.id }
        if (tvCredits.isEmpty()) return emptyList()
        val localSeries = seriesRepository.getSeriesListByTmdbIds(profileId, tvCredits.map { it.id })
        val localByTmdbId = localSeries.groupBy { it.tmdbId }
        return tvCredits.mapNotNull { credit ->
            val local = localByTmdbId[credit.id]?.firstOrNull() ?: return@mapNotNull null
            LocalSeriesCredit(
                tmdbItem = TmdbMovieShortDto(
                    id = credit.id,
                    title = null,
                    tvName = credit.tvName,
                    posterPath = local.cover ?: local.streamIcon ?: credit.posterPath,
                    releaseDate = credit.firstAirDate
                ),
                seriesId = local.seriesId,
                character = credit.character,
                overview = credit.overview,
                year = credit.firstAirDate?.take(4)
            )
        }.sortedByDescending { it.year.orEmpty() }
    }

    fun onSeriesCreditClick(
        credit: LocalSeriesCredit,
        onSingleResult: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val locals = seriesRepository.getSeriesListByTmdbId(profile.id, credit.tmdbItem.id.toString())

            when {
                locals.isEmpty() -> onSingleResult(credit.seriesId)
                locals.size == 1 -> onSingleResult(locals.first().seriesId)
                else -> {
                    variationSelectorTitle = credit.tmdbItem.tvName ?: credit.tmdbItem.title ?: "Seleccionar version"
                    seriesVariations = locals.map { SeriesVariantOption(it) }
                    showSeriesVariationSelector = true
                }
            }
        }
    }

    fun dismissMovieVariationSelector() {
        showMovieVariationSelector = false
        movieVariations = emptyList()
    }

    fun dismissSeriesVariationSelector() {
        showSeriesVariationSelector = false
        seriesVariations = emptyList()
    }
}
