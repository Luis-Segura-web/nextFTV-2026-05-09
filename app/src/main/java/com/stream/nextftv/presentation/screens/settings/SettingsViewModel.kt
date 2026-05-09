package com.stream.nextftv.presentation.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.data.utils.PlaybackCacheCleanupUtils
import com.stream.nextftv.presentation.player.BackgroundPlaybackPreferences
import com.stream.nextftv.presentation.player.PlaybackCacheMode
import com.stream.nextftv.presentation.player.PlaybackCachePreferences
import com.stream.nextftv.presentation.player.PlaybackContentType
import com.stream.nextftv.presentation.player.PipPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.mutableIntStateOf

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    var activeProfile by mutableStateOf<ServerProfile?>(null)
    var isRefreshingAccount by mutableStateOf(false)
    var accountRefreshMessage by mutableStateOf<String?>(null)
    
    var isPipEnabled by mutableStateOf(true)
    var backgroundPlaybackEnabled by mutableStateOf(true)
    var autoPlayNext by mutableStateOf(true)
    var syncInterval by mutableIntStateOf(12)
    var isTmdbEnabled by mutableStateOf(true)
    var liveTvCacheMode by mutableStateOf(PlaybackCacheMode.DISABLED)
    var moviesCacheMode by mutableStateOf(PlaybackCacheMode.PROXY)
    var seriesCacheMode by mutableStateOf(PlaybackCacheMode.PROXY)

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val profiles = profileRepository.getProfiles().first()
            activeProfile = profiles.find { it.isActive }
            syncInterval = profileRepository.getSyncInterval()
            isTmdbEnabled = profileRepository.isTmdbEnabled()
            isPipEnabled = profileRepository.isPipEnabled()
            backgroundPlaybackEnabled = profileRepository.isBackgroundPlaybackEnabled()
            autoPlayNext = profileRepository.isAutoPlayNextEnabled()
            liveTvCacheMode = profileRepository.getPlaybackCacheMode("live_tv")
            moviesCacheMode = profileRepository.getPlaybackCacheMode("movies")
            seriesCacheMode = profileRepository.getPlaybackCacheMode("series")
            PipPreferences.updateEnabled(isPipEnabled)
            BackgroundPlaybackPreferences.updateEnabled(backgroundPlaybackEnabled)
            PlaybackCachePreferences.update(PlaybackContentType.LIVE, liveTvCacheMode)
            PlaybackCachePreferences.update(PlaybackContentType.MOVIE, moviesCacheMode)
            PlaybackCachePreferences.update(PlaybackContentType.SERIES, seriesCacheMode)
        }
    }

    fun onPipEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setPipEnabled(enabled)
            isPipEnabled = enabled
            PipPreferences.updateEnabled(enabled)
        }
    }

    fun onBackgroundPlaybackEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setBackgroundPlaybackEnabled(enabled)
            backgroundPlaybackEnabled = enabled
            BackgroundPlaybackPreferences.updateEnabled(enabled)
        }
    }

    fun onTmdbEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setTmdbEnabled(enabled)
            isTmdbEnabled = enabled
        }
    }

    fun onAutoPlayNextChange(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setAutoPlayNextEnabled(enabled)
            autoPlayNext = enabled
        }
    }

    fun onSyncIntervalChange(hours: Int) {
        viewModelScope.launch {
            profileRepository.setSyncInterval(hours)
            syncInterval = hours
        }
    }

    fun onPlaybackCacheModeChange(contentType: PlaybackContentType, mode: PlaybackCacheMode) {
        viewModelScope.launch {
            val key = when (contentType) {
                PlaybackContentType.LIVE -> "live_tv"
                PlaybackContentType.MOVIE -> "movies"
                PlaybackContentType.SERIES -> "series"
            }
            profileRepository.setPlaybackCacheMode(key, mode)
            when (contentType) {
                PlaybackContentType.LIVE -> liveTvCacheMode = mode
                PlaybackContentType.MOVIE -> moviesCacheMode = mode
                PlaybackContentType.SERIES -> seriesCacheMode = mode
            }
            PlaybackCachePreferences.update(contentType, mode)
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            profileRepository.setActiveProfile(-1) // Una forma de desactivar todos enviando un ID inexistente
        }
    }

    fun onClearRecents() {
        viewModelScope.launch {
            activeProfile?.let {
                profileRepository.clearRecents(it.id)
            }
        }
    }

    fun onClearCache(context: android.content.Context) {
        viewModelScope.launch {
            // Limpiar caché de imágenes (Coil)
            val imageLoader = coil3.SingletonImageLoader.get(context)
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            
            // Limpiar caché de reproducción (GSY/Exo/Proxy)
            PlaybackCacheCleanupUtils.clearAllPlaybackCaches(context)

            // Limpiar caché de detalles (Room)
            profileRepository.clearDetailCache()
        }
    }

    fun refreshAccountInfo() {
        val profile = activeProfile ?: return
        if (isRefreshingAccount) return

        viewModelScope.launch {
            isRefreshingAccount = true
            accountRefreshMessage = null
            try {
                profileRepository.fetchAccountInfo(profile)
                    .onSuccess { fresh ->
                        profileRepository.refreshAccountInfo(profile)
                        activeProfile = fresh
                        accountRefreshMessage = "Cuenta actualizada"
                    }
                    .onFailure { error ->
                        accountRefreshMessage = error.message ?: "No se pudo actualizar la cuenta"
                    }
            } finally {
                isRefreshingAccount = false
            }
        }
    }
}
