package com.stream.nextftv.data.repository

import com.stream.nextftv.data.local.dao.ProfileDao
import com.stream.nextftv.data.local.dao.RecentDao
import com.stream.nextftv.data.local.dao.DetailCacheDao
import com.stream.nextftv.data.local.entity.ProfileEntity
import com.stream.nextftv.data.local.entity.toDomain
import com.stream.nextftv.data.remote.XtreamApiService
import com.stream.nextftv.data.remote.dto.LoginResponseDto
import com.stream.nextftv.data.sync.SyncOrchestrator
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.presentation.player.PlaybackCacheMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl
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
    @param:ApplicationContext private val context: Context
) : ProfileRepository {

    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val defaultPlaybackCacheModes = mapOf(
        "live_tv" to PlaybackCacheMode.DISABLED,
        "movies" to PlaybackCacheMode.PROXY,
        "series" to PlaybackCacheMode.PROXY
    )


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

    override suspend fun isAutoPlayNextEnabled(): Boolean {
        return prefs.getBoolean("autoplay_next_enabled", true)
    }

    override suspend fun setAutoPlayNextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay_next_enabled", enabled).apply()
    }

    override suspend fun getPlaybackCacheMode(contentType: String): PlaybackCacheMode {
        val normalizedContentType = contentType.lowercase(Locale.ROOT)
        if (normalizedContentType == "live_tv") {
            val key = "${normalizedContentType}_playback_cache"
            if (prefs.getString(key, PlaybackCacheMode.DISABLED.name) != PlaybackCacheMode.DISABLED.name) {
                prefs.edit().putString(key, PlaybackCacheMode.DISABLED.name).apply()
            }
            return PlaybackCacheMode.DISABLED
        }
        val key = "${normalizedContentType}_playback_cache"
        val defaultMode = defaultPlaybackCacheModes[normalizedContentType] ?: PlaybackCacheMode.DISABLED
        return PlaybackCacheMode.fromStoredValue(prefs.getString(key, defaultMode.name))
    }

    override suspend fun setPlaybackCacheMode(contentType: String, mode: PlaybackCacheMode) {
        val normalizedContentType = contentType.lowercase(Locale.ROOT)
        val key = "${normalizedContentType}_playback_cache"
        val storedMode = if (normalizedContentType == "live_tv") PlaybackCacheMode.DISABLED else mode
        prefs.edit().putString(key, storedMode.name).apply()
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
            val normalizedUrl = normalizeBaseUrl(url)
            val loginResult = executeLogin(normalizedUrl, username, password)
            val loginBody = loginResult.body

            if (loginBody.userInfo != null) {
                val expDate = formatExpirationDate(loginBody.userInfo.expDate)
                val normalizedStatus = normalizeAccountStatus(
                    rawStatus = loginBody.userInfo.status,
                    rawExpDate = loginBody.userInfo.expDate
                )

                profileDao.deactivateAllProfiles()
                val entity = ProfileEntity(
                    name = name,
                    url = loginResult.baseUrl,
                    username = username,
                    password = password,
                    isActive = true,
                    expirationDate = expDate,
                    accountStatus = normalizedStatus,
                    serverTimezone = loginBody.serverInfo?.timezone?.trim()?.ifEmpty { null },
                    activeConnections = loginBody.userInfo.activeCons?.toIntOrNull(),
                    maxConnections = loginBody.userInfo.maxConnections?.toIntOrNull(),
                    allowedOutputFormats = loginBody.userInfo.allowedOutputFormats?.takeIf { it.isNotEmpty() }
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

    override suspend fun updateProfile(
        id: Int,
        name: String,
        url: String,
        username: String,
        password: String
    ): Result<Unit> {
        return try {
            val currentProfile = profileDao.getProfileById(id)
                ?: return Result.failure(Exception("Perfil no encontrado"))

            val normalizedUrl = normalizeBaseUrl(url)
            val loginResult = executeLogin(normalizedUrl, username, password)
            val loginBody = loginResult.body

            if (loginBody.userInfo != null) {
                val expDate = formatExpirationDate(loginBody.userInfo.expDate)
                val normalizedStatus = normalizeAccountStatus(
                    rawStatus = loginBody.userInfo.status,
                    rawExpDate = loginBody.userInfo.expDate
                )

                val updatedEntity = currentProfile.copy(
                    name = name,
                    url = loginResult.baseUrl,
                    username = username,
                    password = password,
                    expirationDate = expDate,
                    accountStatus = normalizedStatus,
                    serverTimezone = loginBody.serverInfo?.timezone?.trim()?.ifEmpty { null },
                    activeConnections = loginBody.userInfo.activeCons?.toIntOrNull(),
                    maxConnections = loginBody.userInfo.maxConnections?.toIntOrNull(),
                    allowedOutputFormats = loginBody.userInfo.allowedOutputFormats?.takeIf { it.isNotEmpty() }
                )
                profileDao.insertProfile(updatedEntity)
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

    override suspend fun getProfileById(id: Int): ServerProfile? {
        return profileDao.getProfileById(id)?.toDomain()
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
            sourceType = profile.sourceType,
            isActive = profile.isActive,
            lastLiveSync = profile.lastLiveSync,
            lastMoviesSync = profile.lastMoviesSync,
            lastSeriesSync = profile.lastSeriesSync,
            expirationDate = profile.expirationDate,
            accountStatus = profile.accountStatus,
            serverTimezone = profile.serverTimezone,
            activeConnections = profile.activeConnections,
            maxConnections = profile.maxConnections,
            allowedOutputFormats = profile.allowedOutputFormats,
            epgUrl = profile.epgUrl
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
            profileDao.updateAccountInfo(
                id = profile.id,
                url = freshProfile.url,
                expDate = freshProfile.expirationDate ?: "Ilimitada",
                status = freshProfile.accountStatus ?: "Unknown",
                serverTimezone = freshProfile.serverTimezone,
                activeConnections = freshProfile.activeConnections,
                maxConnections = freshProfile.maxConnections,
                allowedOutputFormats = freshProfile.allowedOutputFormats
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAccountInfo(profile: ServerProfile): Result<ServerProfile> {
        return try {
            val loginResult = executeLogin(profile.url, profile.username, profile.password)
            val loginBody = loginResult.body

            if (loginBody.userInfo != null) {
                val expDate = formatExpirationDate(loginBody.userInfo.expDate)
                val normalizedStatus = normalizeAccountStatus(
                    rawStatus = loginBody.userInfo.status,
                    rawExpDate = loginBody.userInfo.expDate
                )
                Result.success(
                    profile.copy(
                        url = loginResult.baseUrl,
                        expirationDate = expDate,
                        accountStatus = normalizedStatus,
                        serverTimezone = loginBody.serverInfo?.timezone?.trim()?.ifEmpty { null },
                        activeConnections = loginBody.userInfo.activeCons?.toIntOrNull(),
                        maxConnections = loginBody.userInfo.maxConnections?.toIntOrNull(),
                        allowedOutputFormats = loginBody.userInfo.allowedOutputFormats?.takeIf { it.isNotEmpty() }
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

    private suspend fun executeLogin(
        baseUrl: String,
        username: String,
        password: String
    ): XtreamLoginResult {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        val response = apiService.loginWithUrl(
            url = "${normalizedBaseUrl}player_api.php",
            username = username,
            password = password
        )
        if (!response.isSuccessful) {
            throw Exception("Error de red al iniciar sesion: HTTP ${response.code()}")
        }

        val body = response.body() ?: throw Exception("Respuesta vacia del servidor Xtream")
        val resolvedBaseUrl = resolveBaseUrl(response.raw().request.url)
        return XtreamLoginResult(
            body = body,
            baseUrl = resolvedBaseUrl
        )
    }

    private fun normalizeBaseUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun resolveBaseUrl(requestUrl: HttpUrl): String {
        return requestUrl.newBuilder()
            .encodedPath("/")
            .query(null)
            .fragment(null)
            .build()
            .toString()
    }

    private data class XtreamLoginResult(
        val body: LoginResponseDto,
        val baseUrl: String
    )
}
