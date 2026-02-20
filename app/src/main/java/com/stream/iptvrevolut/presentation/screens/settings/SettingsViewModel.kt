package com.stream.iptvrevolut.presentation.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.presentation.player.BackgroundPlaybackPreferences
import com.stream.iptvrevolut.presentation.player.PipPreferences
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
    
    // Configuración simulada (en una app real se usaría DataStore)
    var isPipEnabled by mutableStateOf(true)
    var backgroundPlaybackEnabled by mutableStateOf(true)
    var autoPlayNext by mutableStateOf(true)
    var useExternalPlayer by mutableStateOf(false)
    var parentalControlEnabled by mutableStateOf(false)
    var syncInterval by mutableIntStateOf(12)
    var isTmdbEnabled by mutableStateOf(true)

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
            PipPreferences.updateEnabled(isPipEnabled)
            BackgroundPlaybackPreferences.updateEnabled(backgroundPlaybackEnabled)
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

    fun onSyncIntervalChange(hours: Int) {
        viewModelScope.launch {
            profileRepository.setSyncInterval(hours)
            syncInterval = hours
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
            
            // Limpiar caché de detalles (Room)
            profileRepository.clearDetailCache()
        }
    }
}
