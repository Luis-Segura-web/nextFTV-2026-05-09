package com.stream.iptvrevolut.domain.repository

import com.stream.iptvrevolut.domain.model.ServerProfile
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
    suspend fun clearRecents(profileId: Int)
    suspend fun clearDetailCache()
    suspend fun refreshAccountInfo(profile: ServerProfile): Result<Unit>
}
