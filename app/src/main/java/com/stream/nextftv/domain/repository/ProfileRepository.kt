package com.stream.nextftv.domain.repository

import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.presentation.player.PlaybackCacheMode
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun loginAndSave(name: String, url: String, username: String, password: String): Result<Unit>
    suspend fun updateProfile(id: Int, name: String, url: String, username: String, password: String): Result<Unit>
    fun getProfiles(): Flow<List<ServerProfile>>
    suspend fun getProfileById(id: Int): ServerProfile?
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
    suspend fun isAutoPlayNextEnabled(): Boolean
    suspend fun setAutoPlayNextEnabled(enabled: Boolean)
    suspend fun getPlaybackCacheMode(contentType: String): PlaybackCacheMode
    suspend fun setPlaybackCacheMode(contentType: String, mode: PlaybackCacheMode)
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
