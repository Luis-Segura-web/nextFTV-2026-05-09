package com.stream.nextftv.presentation.screens.artistsearch

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.nextftv.data.remote.tmdb.TmdbPersonSearchResultDto
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.SeriesRepository
import com.stream.nextftv.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@OptIn(FlowPreview::class)
@HiltViewModel
class ArtistSearchViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository
) : ViewModel() {
    private companion object {
        const val TAG = "ArtistSearch"
    }

    var activeProfile by mutableStateOf<ServerProfile?>(null)
    var searchQuery by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var actors by mutableStateOf<List<TmdbPersonSearchResultDto>>(emptyList())

    init {
        observeProfile()
        observeSearch()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.getProfiles().collectLatest { profiles ->
                activeProfile = profiles.find { it.isActive }
            }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            combine(
                snapshotFlow { activeProfile }.filterNotNull(),
                snapshotFlow { searchQuery }
            ) { profile, query ->
                profile to query.trim()
            }
                .debounce(350)
                .collectLatest { (profile, query) ->
                    if (query.isBlank()) {
                        actors = emptyList()
                        isLoading = false
                        return@collectLatest
                    }

                    isLoading = true
                    val startedAt = System.currentTimeMillis()
                    actors = runCatching {
                        searchActors(profile.id, query)
                    }.onFailure { error ->
                        Log.w(TAG, "artist_search_failed query=$query error=${error.message}")
                    }.getOrDefault(emptyList())
                    Log.d(
                        TAG,
                        "artist_search_done query=$query actors=${actors.size} elapsed=${System.currentTimeMillis() - startedAt}ms"
                    )
                    isLoading = false
                }
        }
    }

    private suspend fun searchActors(profileId: Int, query: String): List<TmdbPersonSearchResultDto> =
        supervisorScope {
            val movieDeferred = async { vodRepository.searchActors(profileId, query) }
            val seriesDeferred = async { seriesRepository.searchActors(profileId, query) }

            val merged = LinkedHashMap<Int, TmdbPersonSearchResultDto>()
            (movieDeferred.await() + seriesDeferred.await()).forEach { actor ->
                val current = merged[actor.id]
                merged[actor.id] = when {
                    current == null -> actor
                    current.profilePath.isNullOrBlank() && !actor.profilePath.isNullOrBlank() -> actor
                    (actor.popularity ?: 0.0) > (current.popularity ?: 0.0) -> actor
                    else -> current
                }
            }

            merged.values
                .sortedWith(
                    compareBy<TmdbPersonSearchResultDto> { if (it.profilePath.isNullOrBlank()) 1 else 0 }
                        .thenByDescending { it.popularity ?: 0.0 }
                        .thenBy { it.name.orEmpty() }
                )
        }
}
