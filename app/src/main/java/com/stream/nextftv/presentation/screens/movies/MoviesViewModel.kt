package com.stream.nextftv.presentation.screens.movies

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import android.util.Log
import com.stream.nextftv.data.local.entity.vod.VodCategoryEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.model.SortOrder
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repository: VodRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private companion object {
        const val TAG = "MoviesSearch"
    }

    var activeProfile by mutableStateOf<ServerProfile?>(null)
    var selectedCategoryId by mutableStateOf("all")
    
    var searchQuery by mutableStateOf("")
    var sortOrder by mutableStateOf(SortOrder.DEFAULT)
    var isSearchActive by mutableStateOf(false)
    var enhancedSearchResults by mutableStateOf<List<VodStreamEntity>>(emptyList())
    var isEnhancedSearchLoading by mutableStateOf(false)

    private val _categories = MutableStateFlow<List<VodCategoryEntity>>(emptyList())
    val categories: StateFlow<List<VodCategoryEntity>> = _categories.asStateFlow()

    val pagedMovies: Flow<PagingData<VodStreamEntity>> = combine(
        snapshotFlow { activeProfile }.filterNotNull(),
        snapshotFlow { selectedCategoryId },
        snapshotFlow { searchQuery },
        snapshotFlow { sortOrder }
    ) { profile, categoryId, query, order ->
        repository.getPagedStreams(profile.id, categoryId, query, order.name)
    }.flatMapLatest { it }
    .cachedIn(viewModelScope)

    init {
        loadData()
        observeEnhancedSearch()
    }

    private fun loadData() {
        viewModelScope.launch {
            profileRepository.getProfiles().collectLatest { profiles ->
                val profile = profiles.find { it.isActive }
                if (profile != null) {
                    activeProfile = profile
                    repository.getCategories(profile.id).collect {
                        _categories.value = it
                    }
                }
            }
        }
    }

    fun onCategorySelect(categoryId: String) {
        selectedCategoryId = categoryId
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }

    private fun observeEnhancedSearch() {
        viewModelScope.launch {
            combine(
                snapshotFlow { activeProfile }.filterNotNull(),
                snapshotFlow { selectedCategoryId },
                snapshotFlow { searchQuery }
            ) { profile, categoryId, query ->
                Triple(profile, categoryId, query)
            }
                .debounce(350)
                .collectLatest { (profile, categoryId, query) ->
                    val trimmedQuery = query.trim()
                    if (trimmedQuery.isEmpty()) {
                        enhancedSearchResults = emptyList()
                        isEnhancedSearchLoading = false
                        return@collectLatest
                    }

                    isEnhancedSearchLoading = true
                    val startedAt = System.currentTimeMillis()
                    val movieResults = runCatching {
                        repository.searchStreamsEnhanced(profile.id, categoryId, trimmedQuery)
                    }.onFailure { error ->
                        Log.w(TAG, "search_enhanced_failed query=$trimmedQuery error=${error.message}")
                    }.getOrDefault(emptyList())
                    enhancedSearchResults = movieResults
                    Log.d(
                        TAG,
                        "search_enhanced_done query=$trimmedQuery movies=${enhancedSearchResults.size} elapsed=${System.currentTimeMillis() - startedAt}ms"
                    )
                    isEnhancedSearchLoading = false
                }
        }
    }

    fun onPlaybackStarted(streamId: Int) {
        if (selectedCategoryId != "recents") {
            viewModelScope.launch {
                activeProfile?.let { profile ->
                    repository.addToRecents(profile.id, streamId, "vod")
                }
            }
        }
    }

    fun isFavorite(streamId: Int): Flow<Boolean> {
        return activeProfile?.let {
            repository.isFavorite(it.id, streamId, "vod")
        } ?: flowOf(false)
    }

    fun onToggleFavorite(movie: VodStreamEntity) {
        viewModelScope.launch {
            activeProfile?.let {
                repository.toggleFavorite(it.id, movie.streamId, "vod")
            }
        }
    }
}
