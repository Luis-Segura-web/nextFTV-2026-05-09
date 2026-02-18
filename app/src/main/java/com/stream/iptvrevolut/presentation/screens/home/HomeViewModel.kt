package com.stream.iptvrevolut.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.LiveTvRepository
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.SeriesRepository
import com.stream.iptvrevolut.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val liveTvRepository: LiveTvRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository
) : ViewModel() {

    private val syncMutex = Mutex()

    var activeProfile by mutableStateOf<ServerProfile?>(null)
        private set

    val syncStates = mutableStateMapOf<String, SyncState>().apply {
        put("live", SyncState.IDLE)
        put("movies", SyncState.IDLE)
        put("series", SyncState.IDLE)
    }

    val lastSync = mutableStateMapOf<String, String>().apply {
        put("live", "Nunca")
        put("movies", "Nunca")
        put("series", "Nunca")
    }

    val counts = mutableStateMapOf<String, Int>().apply {
        put("live", 0)
        put("movies", 0)
        put("series", 0)
        put("downloads", 0)
    }

    var isGlobalSyncing by mutableStateOf(false)
        private set

    init {
        observeActiveProfile()
    }

    private fun observeActiveProfile() {
        viewModelScope.launch {
            profileRepository.getProfiles().collectLatest { profiles ->
                val profile = profiles.find { it.isActive }
                if (profile != null) {
                    if (profile.id != activeProfile?.id) {
                        counts["live"] = 0
                        counts["movies"] = 0
                        counts["series"] = 0
                        activeProfile = profile
                        updateSyncLabels(profile)
                        checkAutoSync(profile)
                    } else {
                        activeProfile = profile
                        updateSyncLabels(profile)
                    }
                    
                    val profileId = profile.id
                    // Suscripción reactiva a los conteos
                    launch {
                        liveTvRepository.getStreams(profileId, null).collect { streams ->
                            counts["live"] = streams.size
                        }
                    }
                    launch {
                        vodRepository.getStreams(profileId, null).collect { streams ->
                            counts["movies"] = streams.size
                        }
                    }
                    launch {
                        seriesRepository.getSeries(profileId, null).collect { streams ->
                            counts["series"] = streams.size
                        }
                    }
                }
            }
        }
    }

    private fun refreshCountForModule(profileId: Int, module: String) {
        viewModelScope.launch {
            when(module) {
                "live" -> counts["live"] = liveTvRepository.getStreams(profileId, null).first().size
                "movies" -> counts["movies"] = vodRepository.getStreams(profileId, null).first().size
                "series" -> counts["series"] = seriesRepository.getSeries(profileId, null).first().size
            }
        }
    }

    private fun updateSyncLabels(profile: ServerProfile) {
        lastSync["live"] = formatTimestamp(profile.lastLiveSync)
        lastSync["movies"] = formatTimestamp(profile.lastMoviesSync)
        lastSync["series"] = formatTimestamp(profile.lastSeriesSync)
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Nunca"
        val sdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    private fun checkAutoSync(profile: ServerProfile) {
        viewModelScope.launch {
            val intervalHours = profileRepository.getSyncInterval()
            val intervalMillis = intervalHours * 60 * 60 * 1000L
            val now = System.currentTimeMillis()

            if (now - profile.lastLiveSync > intervalMillis) performSyncAction("live")
            if (now - profile.lastMoviesSync > intervalMillis) performSyncAction("movies")
            if (now - profile.lastSeriesSync > intervalMillis) performSyncAction("series")
        }
    }

    private suspend fun performSyncAction(module: String) {
        val profile = activeProfile ?: return
        
        syncMutex.withLock {
            syncStates[module] = SyncState.SYNCING
            isGlobalSyncing = true
            
            val result = when(module) {
                "live" -> {
                    val res = liveTvRepository.syncCategories(profile)
                    if (res.isSuccess) liveTvRepository.syncStreams(profile) else res
                }
                "movies" -> {
                    val res = vodRepository.syncCategories(profile)
                    if (res.isSuccess) vodRepository.syncStreams(profile) else res
                }
                "series" -> {
                    val res = seriesRepository.syncCategories(profile)
                    if (res.isSuccess) seriesRepository.syncSeries(profile) else res
                }
                else -> Result.failure(Exception("Desconocido"))
            }
            
            if (result.isSuccess) {
                syncStates[module] = SyncState.SUCCESS
                val now = System.currentTimeMillis()
                profileRepository.updateSyncTime(profile.id, module, now)
                lastSync[module] = formatTimestamp(now)
                refreshCountForModule(profile.id, module)
            } else {
                syncStates[module] = SyncState.ERROR
            }
            isGlobalSyncing = syncStates.values.any { it == SyncState.SYNCING }
        }
    }

    fun syncModule(module: String) {
        viewModelScope.launch {
            performSyncAction(module)
        }
    }

    fun syncAll() {
        if (isGlobalSyncing) return
        viewModelScope.launch {
            activeProfile?.let { profile ->
                profileRepository.refreshAccountInfo(profile)
                performSyncAction("live")
                performSyncAction("movies")
                performSyncAction("series")
            }
        }
    }
}
