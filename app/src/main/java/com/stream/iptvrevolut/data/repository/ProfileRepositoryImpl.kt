package com.stream.iptvrevolut.data.repository

import com.stream.iptvrevolut.data.local.dao.ProfileDao
import com.stream.iptvrevolut.data.local.dao.RecentDao
import com.stream.iptvrevolut.data.local.dao.DetailCacheDao
import com.stream.iptvrevolut.data.local.entity.ProfileEntity
import com.stream.iptvrevolut.data.local.entity.toDomain
import com.stream.iptvrevolut.data.remote.XtreamApiService
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

class ProfileRepositoryImpl @Inject constructor(
    private val apiService: XtreamApiService,
    private val profileDao: ProfileDao,
    private val recentDao: RecentDao,
    private val detailCacheDao: DetailCacheDao,
    @ApplicationContext private val context: Context
) : ProfileRepository {

    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)


    override suspend fun getSyncInterval(): Int {
        return prefs.getInt("sync_interval", 12) 
    }

    override suspend fun setSyncInterval(hours: Int) {
        prefs.edit().putInt("sync_interval", hours).apply()
    }

    override suspend fun isTmdbEnabled(): Boolean {
        return prefs.getBoolean("tmdb_enabled", true)
    }

    override suspend fun setTmdbEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("tmdb_enabled", enabled).apply()
    }

    override suspend fun isPipEnabled(): Boolean {
        return prefs.getBoolean("pip_enabled", true)
    }

    override suspend fun setPipEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pip_enabled", enabled).apply()
    }

    override suspend fun isBackgroundPlaybackEnabled(): Boolean {
        return prefs.getBoolean("background_playback_enabled", true)
    }

    override suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("background_playback_enabled", enabled).apply()
    }

    override suspend fun getParentalPin(): String? {
        return prefs.getString("parental_pin", null)
    }

    override suspend fun setParentalPin(pin: String) {
        prefs.edit().putString("parental_pin", pin).apply()
    }

    override suspend fun clearParentalPin() {
        prefs.edit().remove("parental_pin").apply()
    }

    override suspend fun hasParentalPin(): Boolean {
        return !prefs.getString("parental_pin", null).isNullOrBlank()
    }

    override suspend fun verifyParentalPin(pin: String): Boolean {
        return prefs.getString("parental_pin", null) == pin
    }

    override suspend fun clearRecents(profileId: Int) {
        recentDao.clearRecents(profileId)
    }

    override suspend fun clearDetailCache() {
        detailCacheDao.clearAllCache()
    }

    override suspend fun loginAndSave(
        name: String,
        url: String,
        username: String,
        password: String
    ): Result<Unit> {
        return try {
            val baseUrl = if (url.endsWith("/")) url else "$url/"
            val fullUrl = "${baseUrl}player_api.php"
            
            android.util.Log.d("XtreamLogin", "Intentando login en: $fullUrl | User: $username")

            val response = apiService.loginWithUrl(
                url = fullUrl, 
                username = username, 
                password = password
            )

            if (response.userInfo != null) {
                val expDate = response.userInfo.expDate?.let { timestamp ->
                    if (timestamp == "null" || timestamp == "0") "Ilimitada"
                    else {
                        try {
                            val date = java.util.Date(timestamp.toLong() * 1000)
                            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd - HH:mm 'hrs'", java.util.Locale.getDefault())
                            sdf.format(date)
                        } catch (e: Exception) {
                            "Desconocida"
                        }
                    }
                } ?: "Ilimitada"

                profileDao.deactivateAllProfiles()
                val entity = ProfileEntity(
                    name = name,
                    url = baseUrl,
                    username = username,
                    password = password,
                    isActive = true,
                    expirationDate = expDate,
                    accountStatus = response.userInfo.status ?: "Unknown"
                )
                profileDao.insertProfile(entity)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Credenciales inválidas o cuenta inactiva"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getProfiles(): Flow<List<ServerProfile>> {
        return profileDao.getAllProfiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteProfile(profile: ServerProfile) {
        val entity = ProfileEntity(
            id = profile.id,
            name = profile.name,
            url = profile.url,
            username = profile.username,
            password = profile.password,
            isActive = profile.isActive,
            expirationDate = profile.expirationDate,
            accountStatus = profile.accountStatus
        )
        profileDao.deleteProfile(entity)
    }

    override suspend fun setActiveProfile(id: Int) {
        profileDao.deactivateAllProfiles()
        profileDao.activateProfile(id)
    }

    override suspend fun refreshAccountInfo(profile: ServerProfile): Result<Unit> {
        return try {
            val fullUrl = "${profile.url}player_api.php"
            val response = apiService.loginWithUrl(
                url = fullUrl, 
                username = profile.username, 
                password = profile.password
            )

            if (response.userInfo != null) {
                val expDate = response.userInfo.expDate?.let { timestamp ->
                    if (timestamp == "null" || timestamp == "0") "Ilimitada"
                    else {
                        try {
                            val date = java.util.Date(timestamp.toLong() * 1000)
                            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd - HH:mm 'hrs'", java.util.Locale.getDefault())
                            sdf.format(date)
                        } catch (e: Exception) {
                            "Desconocida"
                        }
                    }
                } ?: "Ilimitada"

                profileDao.updateAccountInfo(
                    profile.id,
                    expDate,
                    response.userInfo.status ?: "Unknown"
                )
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al refrescar cuenta"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
