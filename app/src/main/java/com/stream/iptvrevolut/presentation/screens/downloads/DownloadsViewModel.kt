package com.stream.iptvrevolut.presentation.screens.downloads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity
import com.stream.iptvrevolut.data.utils.StringUtils
import com.stream.iptvrevolut.domain.repository.DownloadRepository
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var selectedTab by mutableStateOf(0)
    var downloadToDelete by mutableStateOf<DownloadEntity?>(null)

    val downloads: StateFlow<List<DownloadEntity>> = profileRepository.getProfiles()
        .map { profiles -> profiles.find { it.isActive }?.id }
        .flatMapLatest { profileId ->
            if (profileId != null) downloadRepository.getDownloads(profileId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Películas ordenadas alfabéticamente (Orden Natural)
    val movieDownloads = downloads.map { list -> 
        list.filter { it.type == "movie" }
            .sortedBy { StringUtils.naturalSort(it.title) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Series agrupadas y ordenadas
    val seriesDownloads = downloads.map { list -> 
        list.filter { it.type == "episode" }
            .sortedWith(compareBy<DownloadEntity> { StringUtils.naturalSort(it.parentName) }
                .thenBy { it.parentId }
                .thenBy { it.id }) // Fallback
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadRepository.removeDownload(download)
            downloadToDelete = null
        }
    }

    fun pauseDownload(downloadId: String) {
        viewModelScope.launch {
            downloadRepository.pauseDownload(downloadId)
        }
    }

    fun resumeDownload(download: DownloadEntity) {
        viewModelScope.launch {
            downloadRepository.resumeDownload(download)
        }
    }
}
