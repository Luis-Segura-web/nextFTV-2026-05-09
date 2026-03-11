package com.stream.iptvrevolut.data.repository

import com.stream.iptvrevolut.data.local.dao.ProfileDao
import com.stream.iptvrevolut.data.local.dao.RecentDao
import com.stream.iptvrevolut.data.local.dao.DetailCacheDao
import com.stream.iptvrevolut.data.local.entity.ProfileEntity
import com.stream.iptvrevolut.data.local.entity.toDomain
import com.stream.iptvrevolut.data.remote.XtreamApiService
import com.stream.iptvrevolut.data.sync.SyncOrchestrator
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.presentation.player.PlayerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

class ProfileRepositoryImpl @Inject constructor(
    private val apiService: XtreamApiService,
    private val profileDao: ProfileDao,
    private val recentDao: RecentDao,
    private val detailCacheDao: DetailCacheDao,
    private val syncOrchestrator: SyncOrchestrator,
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

    override suspend fun getPreferredPlayerEngine(): PlayerEngine {
        val stored = prefs.getString("preferred_player_engine", PlayerEngine.MEDIA3.name)
        return stored
            ?.let { value -> PlayerEngine.entries.find { it.name == value } }
            ?: PlayerEngine.MEDIA3
    }

    override suspend fun setPreferredPlayerEngine(engine: PlayerEngine) {
        prefs.edit().putString("preferred_player_engine", engine.name).apply()
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
                val expDate = formatExpirationDate(response.userInfo.expDate)
                val normalizedStatus = normalizeAccountStatus(
                    rawStatus = response.userInfo.status,
                    rawExpDate = response.userInfo.expDate
                )

                profileDao.deactivateAllProfiles()
                val entity = ProfileEntity(
                    name = name,
                    url = baseUrl,
                    username = username,
                    password = password,
                    isActive = true,
                    expirationDate = expDate,
                    accountStatus = normalizedStatus
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
        syncOrchestrator.clearStreamingContent(profile.id)
        recentDao.clearRecents(profile.id)
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
            val freshProfile = fetchAccountInfo(profile).getOrThrow()
            profileDao.updateAccountInfo(profile.id, freshProfile.expirationDate ?: "Ilimitada", freshProfile.accountStatus ?: "Unknown")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAccountInfo(profile: ServerProfile): Result<ServerProfile> {
        return try {
            val fullUrl = "${profile.url}player_api.php"
            val response = apiService.loginWithUrl(
                url = fullUrl,
                username = profile.username,
                password = profile.password
            )

            if (response.userInfo != null) {
                val expDate = formatExpirationDate(response.userInfo.expDate)
                val normalizedStatus = normalizeAccountStatus(
                    rawStatus = response.userInfo.status,
                    rawExpDate = response.userInfo.expDate
                )
                Result.success(
                    profile.copy(
                        expirationDate = expDate,
                        accountStatus = normalizedStatus
                    )
                )
            } else {
                Result.failure(Exception("Error al consultar estado de cuenta"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatExpirationDate(rawExpDate: String?): String {
        val expEpochSeconds = parseExpEpochSeconds(rawExpDate) ?: return "Ilimitada"
        return try {
            val date = Date(expEpochSeconds * 1000L)
            val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm 'hrs'", Locale.getDefault())
            sdf.format(date)
        } catch (_: Exception) {
            "Desconocida"
        }
    }

    private fun normalizeAccountStatus(rawStatus: String?, rawExpDate: String?): String {
        val nowEpochSeconds = System.currentTimeMillis() / 1000L
        val expEpochSeconds = parseExpEpochSeconds(rawExpDate)
        val isExpiredByDate = expEpochSeconds != null && expEpochSeconds <= nowEpochSeconds
        if (isExpiredByDate) return "Expired"

        return when (rawStatus?.trim()?.lowercase(Locale.ROOT)) {
            "active" -> "Active"
            "expired" -> "Expired"
            "disabled" -> "Disabled"
            "banned" -> "Banned"
            "trial" -> "Trial"
            null, "" -> if (expEpochSeconds == null || expEpochSeconds > nowEpochSeconds) "Active" else "Expired"
            else -> rawStatus.trim()
        }
    }

    private fun parseExpEpochSeconds(rawExpDate: String?): Long? {
        val value = rawExpDate?.trim()
        if (value.isNullOrEmpty() || value == "null" || value == "0") return null
        return value.toLongOrNull()?.takeIf { it > 0L }
    }
}
