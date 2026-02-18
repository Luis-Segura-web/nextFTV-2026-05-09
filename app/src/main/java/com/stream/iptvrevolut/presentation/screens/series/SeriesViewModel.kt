package com.stream.iptvrevolut.presentation.screens.series

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.stream.iptvrevolut.data.local.entity.series.SeriesCategoryEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.model.SortOrder
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val repository: SeriesRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var activeProfile by mutableStateOf<ServerProfile?>(null)
    var selectedCategoryId by mutableStateOf("all")
    
    var searchQuery by mutableStateOf("")
    var sortOrder by mutableStateOf(SortOrder.DEFAULT)
    var isSearchActive by mutableStateOf(false)

    private val _categories = MutableStateFlow<List<SeriesCategoryEntity>>(emptyList())
    val categories: StateFlow<List<SeriesCategoryEntity>> = _categories.asStateFlow()

    val pagedSeries: Flow<PagingData<SeriesStreamEntity>> = combine(
        snapshotFlow { activeProfile }.filterNotNull(),
        snapshotFlow { selectedCategoryId },
        snapshotFlow { searchQuery },
        snapshotFlow { sortOrder }
    ) { profile, categoryId, query, order ->
        repository.getPagedSeries(profile.id, categoryId, query, order.name)
    }.flatMapLatest { it }
    .cachedIn(viewModelScope)

    init {
        loadData()
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

    fun onPlaybackStarted(streamId: Int) {
        if (selectedCategoryId != "recents") {
            viewModelScope.launch {
                activeProfile?.let { profile ->
                    repository.addToRecents(profile.id, streamId, "series")
                }
            }
        }
    }

    fun isFavorite(seriesId: Int): Flow<Boolean> {
        return activeProfile?.let {
            repository.isFavorite(it.id, seriesId, "series")
        } ?: flowOf(false)
    }

    fun onToggleFavorite(series: SeriesStreamEntity) {
        viewModelScope.launch {
            activeProfile?.let {
                repository.toggleFavorite(it.id, series.seriesId, "series")
            }
        }
    }
}
