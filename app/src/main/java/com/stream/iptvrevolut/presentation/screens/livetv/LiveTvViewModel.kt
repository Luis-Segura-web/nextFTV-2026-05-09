package com.stream.iptvrevolut.presentation.screens.livetv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.stream.iptvrevolut.data.local.entity.live.LiveCategoryEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveStreamEntity
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.model.SortOrder
import com.stream.iptvrevolut.domain.repository.LiveTvRepository
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val repository: LiveTvRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var activeProfile by mutableStateOf<ServerProfile?>(null)
    var selectedCategoryId by mutableStateOf("all")
    
    var searchQuery by mutableStateOf("")
    var sortOrder by mutableStateOf(SortOrder.DEFAULT)
    var isSearchActive by mutableStateOf(false)

    var currentPlayingStream by mutableStateOf<LiveStreamEntity?>(null)
    var playerError by mutableStateOf<String?>(null)
    var isPlayerLoading by mutableStateOf(false)

    private val _categories = MutableStateFlow<List<LiveCategoryEntity>>(emptyList())
    val categories: StateFlow<List<LiveCategoryEntity>> = _categories.asStateFlow()

    // Flujo de Paginación
    val pagedStreams: Flow<PagingData<LiveStreamEntity>> = combine(
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

    fun onChannelClick(stream: LiveStreamEntity) {
        currentPlayingStream = stream
        playerError = null
    }

    fun onPlaybackStarted() {
        if (selectedCategoryId != "recents") {
            currentPlayingStream?.let { stream ->
                viewModelScope.launch {
                    activeProfile?.let { profile ->
                        repository.addToRecents(profile.id, stream.streamId, "live")
                    }
                }
            }
        }
    }

    fun onNextChannel() {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val current = currentPlayingStream ?: return@launch
            val streams = repository.getFilteredStreams(profile.id, selectedCategoryId, searchQuery, sortOrder.name)
            if (streams.isNotEmpty()) {
                val currentIndex = streams.indexOfFirst { it.streamId == current.streamId }
                if (currentIndex != -1) {
                    val nextIndex = (currentIndex + 1) % streams.size
                    currentPlayingStream = streams[nextIndex]
                }
            }
        }
    }

    fun onPreviousChannel() {
        viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val current = currentPlayingStream ?: return@launch
            val streams = repository.getFilteredStreams(profile.id, selectedCategoryId, searchQuery, sortOrder.name)
            if (streams.isNotEmpty()) {
                val currentIndex = streams.indexOfFirst { it.streamId == current.streamId }
                if (currentIndex != -1) {
                    val prevIndex = if (currentIndex - 1 < 0) streams.size - 1 else currentIndex - 1
                    currentPlayingStream = streams[prevIndex]
                }
            }
        }
    }

    fun onToggleFavorite(stream: LiveStreamEntity) {
        viewModelScope.launch {
            activeProfile?.let {
                repository.toggleFavorite(it.id, stream.streamId, "live")
            }
        }
    }

    fun isFavorite(streamId: Int): Flow<Boolean> {
        return activeProfile?.let {
            repository.isFavorite(it.id, streamId, "live")
        } ?: flowOf(false)
    }
}
