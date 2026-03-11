package com.stream.iptvrevolut.presentation.screens.home
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.iptvrevolut.data.sync.ContentModule
import com.stream.iptvrevolut.data.sync.SyncOrchestrator
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.LiveTvRepository
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.SeriesRepository
import com.stream.iptvrevolut.domain.repository.VodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR }
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val liveTvRepository: LiveTvRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository,
    private val syncOrchestrator: SyncOrchestrator
) : ViewModel() {
    private val syncMutex = Mutex()
    private val latestDbCounts = mutableMapOf(
        "live" to 0,
        "movies" to 0,
        "series" to 0
    )
    var activeProfile by mutableStateOf<ServerProfile?>(null)
        private set
    var liveAccountStatus by mutableStateOf<String?>(null)
        private set
    var liveExpirationDate by mutableStateOf<String?>(null)
        private set
    var isValidatingAccount by mutableStateOf(false)
        private set
    private var validationJob: Job? = null
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

    private fun isAccountActiveNow(): Boolean {
        val expiration = liveExpirationDate ?: activeProfile?.expirationDate
        if (isExpiredByLocalDate(expiration)) return false
        val status = (liveAccountStatus ?: activeProfile?.accountStatus).orEmpty()
        return status.equals("Active", ignoreCase = true)
    }

    private fun isExpiredByLocalDate(expirationDate: String?): Boolean {
        val value = expirationDate?.trim().orEmpty()
        if (value.isBlank() || value.equals("Ilimitada", ignoreCase = true) || value.equals("Desconocida", ignoreCase = true)) {
            return false
        }
        return runCatching {
            val format = SimpleDateFormat("yyyy/MM/dd - HH:mm 'hrs'", Locale.getDefault()).apply {
                isLenient = false
            }
            val parsed = format.parse(value) ?: return@runCatching false
            parsed.time <= System.currentTimeMillis()
        }.getOrElse { false }
    }
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
                        liveAccountStatus = null
                        liveExpirationDate = null
                        loadSyncLabels(profile.id)
                        checkAutoSync(profile)
                    } else {
                        activeProfile = profile
                        loadSyncLabels(profile.id)
                    }
                    val profileId = profile.id
                    launch {
                        liveTvRepository.getStreams(profileId, null).collect { streams ->
                            latestDbCounts["live"] = streams.size
                            if (syncStates["live"] != SyncState.SYNCING) {
                                counts["live"] = streams.size
                            }
                        }
                    }
                    launch {
                        vodRepository.getStreams(profileId, null).collect { streams ->
                            latestDbCounts["movies"] = streams.size
                            if (syncStates["movies"] != SyncState.SYNCING) {
                                counts["movies"] = streams.size
                            }
                        }
                    }
                    launch {
                        seriesRepository.getSeries(profileId, null).collect { streams ->
                            latestDbCounts["series"] = streams.size
                            if (syncStates["series"] != SyncState.SYNCING) {
                                counts["series"] = streams.size
                            }
                        }
                    }
                }
            }
        }
    }

    fun validateAccountOnHomeEnter() {
        if (isValidatingAccount) return
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            val profile = activeProfile ?: return@launch
            val currentExpiration = liveExpirationDate ?: profile.expirationDate
            // Local hard rule: if local expiration is already in the past, block immediately.
            if (isExpiredByLocalDate(currentExpiration)) {
                liveAccountStatus = "Expired"
                liveExpirationDate = currentExpiration
            }
            isValidatingAccount = true
            try {
                profileRepository.fetchAccountInfo(profile)
                    .onSuccess { fresh ->
                        liveExpirationDate = fresh.expirationDate
                        liveAccountStatus = if (isExpiredByLocalDate(fresh.expirationDate)) {
                            "Expired"
                        } else {
                            fresh.accountStatus
                        }
                    }
                    .onFailure {
                        // Keep last known status silently (no visual verification side effects).
                    }
            } finally {
                isValidatingAccount = false
            }
        }
    }
    private suspend fun refreshCountForModule(profileId: Int, module: String): Int {
        val finalCount = when (module) {
            "live" -> liveTvRepository.getStreams(profileId, null).first().size
            "movies" -> vodRepository.getStreams(profileId, null).first().size
            "series" -> seriesRepository.getSeries(profileId, null).first().size
            else -> latestDbCounts[module] ?: 0
        }
        latestDbCounts[module] = finalCount
        counts[module] = finalCount
        return finalCount
    }
    private fun loadSyncLabels(profileId: Int) {
        viewModelScope.launch {
            ContentModule.entries.forEach { module ->
                val timestamp = syncOrchestrator.getLastSyncTimestamp(profileId, module)
                lastSync[module.key] = formatTimestamp(timestamp)
            }
        }
    }
    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Nunca"
        val sdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
    private fun checkAutoSync(profile: ServerProfile) {
        if (!isAccountActiveNow()) return
        if (isGlobalSyncing || syncMutex.isLocked) return
        viewModelScope.launch {
            val intervalHours = profileRepository.getSyncInterval()
            val intervalMillis = intervalHours * 60 * 60 * 1000L
            val modulesToSync = ContentModule.entries.filter { module ->
                syncOrchestrator.isSyncNeeded(profile.id, module, intervalMillis)
            }
            if (modulesToSync.isEmpty()) return@launch

            runExclusiveSync {
                modulesToSync.forEach { module ->
                    performSyncAction(module.key)
                }
            }
        }
    }
    private suspend fun runExclusiveSync(block: suspend () -> Unit) {
        syncMutex.withLock {
            isGlobalSyncing = true
            try {
                block()
            } finally {
                isGlobalSyncing = false
            }
        }
    }
    private suspend fun performSyncAction(module: String) {
        val profile = activeProfile ?: return
        if (!isAccountActiveNow()) return
        syncStates[module] = SyncState.SYNCING
        val contentModule = ContentModule.fromKey(module)
        val report = syncOrchestrator.syncModule(profile, contentModule)
        if (report.success) {
            refreshCountForModule(profile.id, module)
            lastSync[module] = formatTimestamp(System.currentTimeMillis())
            syncStates[module] = SyncState.SUCCESS
        } else {
            syncStates[module] = SyncState.ERROR
        }
    }
    fun syncModule(module: String) {
        if (!isAccountActiveNow()) return
        if (isGlobalSyncing || syncMutex.isLocked) return
        viewModelScope.launch {
            runExclusiveSync {
                performSyncAction(module)
            }
        }
    }
    fun syncAll() {
        if (!isAccountActiveNow()) return
        if (isGlobalSyncing || syncMutex.isLocked) return
        viewModelScope.launch {
            activeProfile?.let { profile ->
                profileRepository.fetchAccountInfo(profile).onSuccess { fresh ->
                    liveAccountStatus = fresh.accountStatus
                    liveExpirationDate = fresh.expirationDate
                }
                if (!isAccountActiveNow()) return@let
                runExclusiveSync {
                    performSyncAction("live")
                    performSyncAction("movies")
                    performSyncAction("series")
                }
            }
        }
    }
}
