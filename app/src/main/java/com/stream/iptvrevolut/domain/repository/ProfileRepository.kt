package com.stream.iptvrevolut.domain.repository

import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.presentation.player.PlayerEngine
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun loginAndSave(name: String, url: String, username: String, password: String): Result<Unit>
    fun getProfiles(): Flow<List<ServerProfile>>
    suspend fun deleteProfile(profile: ServerProfile)
    suspend fun setActiveProfile(id: Int)
    suspend fun getSyncInterval(): Int
    suspend fun setSyncInterval(hours: Int)
    suspend fun isTmdbEnabled(): Boolean
    suspend fun setTmdbEnabled(enabled: Boolean)
    suspend fun isPipEnabled(): Boolean
    suspend fun setPipEnabled(enabled: Boolean)
    suspend fun isBackgroundPlaybackEnabled(): Boolean
    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean)
    suspend fun getPreferredPlayerEngine(): PlayerEngine
    suspend fun setPreferredPlayerEngine(engine: PlayerEngine)
    suspend fun getParentalPin(): String?
    suspend fun setParentalPin(pin: String)
    suspend fun clearParentalPin()
    suspend fun hasParentalPin(): Boolean
    suspend fun verifyParentalPin(pin: String): Boolean
    suspend fun clearRecents(profileId: Int)
    suspend fun clearDetailCache()
    suspend fun refreshAccountInfo(profile: ServerProfile): Result<Unit>
    suspend fun fetchAccountInfo(profile: ServerProfile): Result<ServerProfile>
}
