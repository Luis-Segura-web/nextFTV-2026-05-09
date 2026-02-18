package com.stream.iptvrevolut.domain.repository

import com.stream.iptvrevolut.domain.model.ServerProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun loginAndSave(name: String, url: String, username: String, password: String): Result<Unit>
    fun getProfiles(): Flow<List<ServerProfile>>
    suspend fun deleteProfile(profile: ServerProfile)
    suspend fun setActiveProfile(id: Int)
    suspend fun updateSyncTime(profileId: Int, module: String, timestamp: Long)
    suspend fun getSyncInterval(): Int
    suspend fun setSyncInterval(hours: Int)
    suspend fun isTmdbEnabled(): Boolean
    suspend fun setTmdbEnabled(enabled: Boolean)
    suspend fun clearRecents(profileId: Int)
    suspend fun clearDetailCache()
    suspend fun refreshAccountInfo(profile: ServerProfile): Result<Unit>
}
