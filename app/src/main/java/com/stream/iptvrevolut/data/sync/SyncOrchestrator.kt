package com.stream.iptvrevolut.data.sync

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.stream.iptvrevolut.data.local.dao.DetailCacheDao
import com.stream.iptvrevolut.data.local.dao.FavoriteDao
import com.stream.iptvrevolut.data.local.dao.LiveTvDao
import com.stream.iptvrevolut.data.local.dao.RecentDao
import com.stream.iptvrevolut.data.local.dao.SeriesDao
import com.stream.iptvrevolut.data.local.dao.SyncStatusDao
import com.stream.iptvrevolut.data.local.dao.VodDao
import com.stream.iptvrevolut.data.local.entity.SyncStatusEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveCategoryEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveStreamEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesCategoryEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodCategoryEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodStreamEntity
import com.stream.iptvrevolut.data.mapper.toEntities
import com.stream.iptvrevolut.data.mapper.toEntity
import com.stream.iptvrevolut.data.remote.LiveStreamDto
import com.stream.iptvrevolut.data.remote.SeriesStreamDto
import com.stream.iptvrevolut.data.remote.VodStreamDto
import com.stream.iptvrevolut.data.remote.XtreamApiService
import com.stream.iptvrevolut.data.remote.utils.SafeBooleanAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeDoubleAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeIntAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeListStringAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeLongAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeStringAdapter
import com.stream.iptvrevolut.domain.model.ServerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SyncOrchestrator"
private const val BATCH_SIZE = 500
private const val MAX_RETRIES = 3
private const val RETRY_DELAY_MS = 2000L

@Singleton
class SyncOrchestrator @Inject constructor(
    private val api: XtreamApiService,
    private val liveTvDao: LiveTvDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao,
    private val detailCacheDao: DetailCacheDao,
    private val syncStatusDao: SyncStatusDao
) {
    private val tolerantGson: Gson = GsonBuilder()
        .registerTypeAdapter(Int::class.java, SafeIntAdapter())
        .registerTypeAdapter(java.lang.Integer::class.java, SafeIntAdapter())
        .registerTypeAdapter(Long::class.java, SafeLongAdapter())
        .registerTypeAdapter(java.lang.Long::class.java, SafeLongAdapter())
        .registerTypeAdapter(Double::class.java, SafeDoubleAdapter())
        .registerTypeAdapter(java.lang.Double::class.java, SafeDoubleAdapter())
        .registerTypeAdapter(Boolean::class.java, SafeBooleanAdapter())
        .registerTypeAdapter(java.lang.Boolean::class.java, SafeBooleanAdapter())
        .registerTypeAdapter(String::class.java, SafeStringAdapter())
        .registerTypeAdapter(
            object : com.google.gson.reflect.TypeToken<List<String>>() {}.type,
            SafeListStringAdapter()
        )
        .setLenient()
        .create()

    suspend fun syncModule(profile: ServerProfile, module: ContentModule): SyncReport {
        val startTime = System.currentTimeMillis()
        markInProgress(profile.id, module)

        return try {
            val report = when (module) {
                ContentModule.LIVE -> syncLive(profile)
                ContentModule.VOD -> syncVod(profile)
                ContentModule.SERIES -> syncSeries(profile)
            }

            val finalReport = report.copy(durationMs = System.currentTimeMillis() - startTime)
            if (finalReport.success) {
                markSuccess(profile.id, module, finalReport)
            } else {
                markFailed(profile.id, module)
            }

            Log.i(
                TAG,
                "Sync ${module.key}: success=${finalReport.success}, categories=${finalReport.categoryCount}, items=${finalReport.itemCount}, duration=${finalReport.durationMs}ms"
            )
            finalReport
        } catch (e: Exception) {
            markFailed(profile.id, module)
            Log.e(TAG, "Sync ${module.key} FAILED: ${e.message}", e)
            SyncReport(
                module = module,
                success = false,
                error = e.message ?: "Unknown error",
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    }

    suspend fun syncAll(profile: ServerProfile): List<SyncReport> {
        return ContentModule.entries.map { module -> syncModule(profile, module) }
    }

    suspend fun isSyncNeeded(profileId: Int, module: ContentModule, intervalMs: Long): Boolean {
        val status = syncStatusDao.getStatus(profileId, module.key) ?: return true
        return (System.currentTimeMillis() - status.lastSyncTimestamp) > intervalMs
    }

    suspend fun getLastSyncTimestamp(profileId: Int, module: ContentModule): Long {
        return syncStatusDao.getStatus(profileId, module.key)?.lastSyncTimestamp ?: 0L
    }

    private suspend fun syncLive(profile: ServerProfile): SyncReport {
        val profileId = profile.id
        liveTvDao.deleteStreamsByProfile(profileId)

        val apiCategories = retryApi("Live categories") {
            api.getLiveCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
        }.toEntities(profileId)

        val (itemCount, categoryIds) = syncLiveStreamsRaw(profile)
        val categories = mergeLiveCategories(profileId, apiCategories, categoryIds)
        liveTvDao.replaceCategories(profileId, categories)

        favoriteDao.clearOrphanLiveFavorites(profileId)
        recentDao.clearOrphanLiveRecents(profileId)

        return SyncReport(
            module = ContentModule.LIVE,
            categoryCount = categories.size,
            itemCount = itemCount,
            success = true
        )
    }

    private suspend fun syncVod(profile: ServerProfile): SyncReport {
        val profileId = profile.id
        detailCacheDao.clearVodCache()
        vodDao.deleteStreamsByProfile(profileId)

        val apiCategories = retryApi("VOD categories") {
            api.getVodCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
        }.toEntities(profileId)

        val (itemCount, categoryIds) = syncVodStreamsRaw(profile)
        val categories = mergeVodCategories(profileId, apiCategories, categoryIds)
        vodDao.replaceCategories(profileId, categories)

        favoriteDao.clearOrphanVodFavorites(profileId)
        recentDao.clearOrphanVodRecents(profileId)

        return SyncReport(
            module = ContentModule.VOD,
            categoryCount = categories.size,
            itemCount = itemCount,
            success = true
        )
    }

    private suspend fun syncSeries(profile: ServerProfile): SyncReport {
        val profileId = profile.id
        detailCacheDao.clearSeriesEpisodesCache()
        seriesDao.deleteStreamsByProfile(profileId)

        val apiCategories = retryApi("Series categories") {
            api.getSeriesCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
        }.toEntities(profileId)

        val (itemCount, categoryIds) = syncSeriesStreamsRaw(profile)
        val categories = mergeSeriesCategories(profileId, apiCategories, categoryIds)
        seriesDao.replaceCategories(profileId, categories)

        favoriteDao.clearOrphanSeriesFavorites(profileId)
        recentDao.clearOrphanSeriesRecents(profileId)

        return SyncReport(
            module = ContentModule.SERIES,
            categoryCount = categories.size,
            itemCount = itemCount,
            success = true
        )
    }

    private suspend fun syncLiveStreamsRaw(profile: ServerProfile): Pair<Int, Set<String>> {
        val baseUrl = "${profile.url}player_api.php"
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    val categoryIds = linkedSetOf<String>()
                    var total = 0
                    api.getLiveStreamsRaw(
                        url = baseUrl,
                        username = profile.username,
                        password = profile.password
                    ).use { body ->
                        JsonReader(body.charStream()).use { reader ->
                            reader.isLenient = true
                            total = parseLiveRoot(profile.id, reader, categoryIds)
                        }
                    }
                    total to categoryIds
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Live raw sync attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }

        throw lastException ?: Exception("Live raw sync failed after $MAX_RETRIES attempts")
    }

    private suspend fun syncVodStreamsRaw(profile: ServerProfile): Pair<Int, Set<String>> {
        val baseUrl = "${profile.url}player_api.php"
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    val categoryIds = linkedSetOf<String>()
                    var total = 0
                    api.getVodStreamsRaw(
                        url = baseUrl,
                        username = profile.username,
                        password = profile.password
                    ).use { body ->
                        JsonReader(body.charStream()).use { reader ->
                            reader.isLenient = true
                            total = parseVodRoot(profile.id, reader, categoryIds)
                        }
                    }
                    total to categoryIds
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "VOD raw sync attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }

        throw lastException ?: Exception("VOD raw sync failed after $MAX_RETRIES attempts")
    }

    private suspend fun syncSeriesStreamsRaw(profile: ServerProfile): Pair<Int, Set<String>> {
        val baseUrl = "${profile.url}player_api.php"
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    val categoryIds = linkedSetOf<String>()
                    var total = 0
                    api.getSeriesRaw(
                        url = baseUrl,
                        username = profile.username,
                        password = profile.password
                    ).use { body ->
                        JsonReader(body.charStream()).use { reader ->
                            reader.isLenient = true
                            total = parseSeriesRoot(profile.id, reader, categoryIds)
                        }
                    }
                    total to categoryIds
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Series raw sync attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }

        throw lastException ?: Exception("Series raw sync failed after $MAX_RETRIES attempts")
    }

    private suspend fun parseLiveRoot(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> parseLiveArray(profileId, reader, categoryIds)
            JsonToken.BEGIN_OBJECT -> parseLiveObject(profileId, reader, categoryIds)
            else -> {
                reader.skipValue()
                throw IllegalStateException("Unexpected live payload token: ${reader.peek()}")
            }
        }
    }

    private suspend fun parseLiveObject(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        var total = 0
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key == "data" || key == "results" || key == "streams" || key == "items") {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    total += parseLiveArray(profileId, reader, categoryIds)
                } else {
                    reader.skipValue()
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        if (total == 0) throw IllegalStateException("Live payload object did not contain a parseable list")
        return total
    }

    private suspend fun parseLiveArray(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        var count = 0
        val batch = ArrayList<LiveStreamEntity>(BATCH_SIZE)
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = readNextLiveDtoSafely(reader)
            if (dto?.streamId != null) {
                val entity = dto.toEntity(profileId)
                categoryIds += entity.categoryId
                batch += entity
                if (batch.size >= BATCH_SIZE) {
                    liveTvDao.insertStreams(batch.toList())
                    count += batch.size
                    batch.clear()
                }
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) {
            liveTvDao.insertStreams(batch.toList())
            count += batch.size
        }
        return count
    }

    private suspend fun parseVodRoot(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> parseVodArray(profileId, reader, categoryIds)
            JsonToken.BEGIN_OBJECT -> parseVodObject(profileId, reader, categoryIds)
            else -> {
                reader.skipValue()
                throw IllegalStateException("Unexpected vod payload token: ${reader.peek()}")
            }
        }
    }

    private suspend fun parseVodObject(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        var total = 0
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key == "data" || key == "results" || key == "streams" || key == "items") {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    total += parseVodArray(profileId, reader, categoryIds)
                } else {
                    reader.skipValue()
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        if (total == 0) throw IllegalStateException("VOD payload object did not contain a parseable list")
        return total
    }

    private suspend fun parseVodArray(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        var count = 0
        val batch = ArrayList<VodStreamEntity>(BATCH_SIZE)
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = readNextVodDtoSafely(reader)
            if (dto?.streamId != null) {
                val entity = dto.toEntity(profileId)
                categoryIds += entity.categoryId
                batch += entity
                if (batch.size >= BATCH_SIZE) {
                    vodDao.insertStreams(batch.toList())
                    count += batch.size
                    batch.clear()
                }
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) {
            vodDao.insertStreams(batch.toList())
            count += batch.size
        }
        return count
    }

    private suspend fun parseSeriesRoot(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> parseSeriesArray(profileId, reader, categoryIds)
            JsonToken.BEGIN_OBJECT -> parseSeriesObject(profileId, reader, categoryIds)
            else -> {
                reader.skipValue()
                throw IllegalStateException("Unexpected series payload token: ${reader.peek()}")
            }
        }
    }

    private suspend fun parseSeriesObject(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        var total = 0
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key == "data" || key == "results" || key == "series" || key == "items") {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    total += parseSeriesArray(profileId, reader, categoryIds)
                } else {
                    reader.skipValue()
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        if (total == 0) throw IllegalStateException("Series payload object did not contain a parseable list")
        return total
    }

    private suspend fun parseSeriesArray(profileId: Int, reader: JsonReader, categoryIds: MutableSet<String>): Int {
        var count = 0
        val batch = ArrayList<SeriesStreamEntity>(BATCH_SIZE)
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = readNextSeriesDtoSafely(reader)
            if (dto?.seriesId != null) {
                val entity = dto.toEntity(profileId)
                categoryIds += entity.categoryId
                batch += entity
                if (batch.size >= BATCH_SIZE) {
                    seriesDao.insertStreams(batch.toList())
                    count += batch.size
                    batch.clear()
                }
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) {
            seriesDao.insertStreams(batch.toList())
            count += batch.size
        }
        return count
    }

    private fun mergeLiveCategories(
        profileId: Int,
        apiCategories: List<LiveCategoryEntity>,
        streamCategoryIds: Set<String>
    ): List<LiveCategoryEntity> {
        val byId = apiCategories.associateBy { it.categoryId }.toMutableMap()
        var nextOrder = (apiCategories.maxOfOrNull { it.orderIndex } ?: -1) + 1
        streamCategoryIds.filter { it.isNotBlank() }.forEach { categoryId ->
            if (!byId.containsKey(categoryId)) {
                byId[categoryId] = LiveCategoryEntity(
                    profileId = profileId,
                    categoryId = categoryId,
                    categoryName = "Categoria $categoryId",
                    parentId = 0,
                    orderIndex = nextOrder++
                )
            }
        }
        return byId.values.sortedBy { it.orderIndex }
    }

    private fun mergeVodCategories(
        profileId: Int,
        apiCategories: List<VodCategoryEntity>,
        streamCategoryIds: Set<String>
    ): List<VodCategoryEntity> {
        val byId = apiCategories.associateBy { it.categoryId }.toMutableMap()
        var nextOrder = (apiCategories.maxOfOrNull { it.orderIndex } ?: -1) + 1
        streamCategoryIds.filter { it.isNotBlank() }.forEach { categoryId ->
            if (!byId.containsKey(categoryId)) {
                byId[categoryId] = VodCategoryEntity(
                    profileId = profileId,
                    categoryId = categoryId,
                    categoryName = "Categoria $categoryId",
                    parentId = 0,
                    orderIndex = nextOrder++
                )
            }
        }
        return byId.values.sortedBy { it.orderIndex }
    }

    private fun mergeSeriesCategories(
        profileId: Int,
        apiCategories: List<SeriesCategoryEntity>,
        streamCategoryIds: Set<String>
    ): List<SeriesCategoryEntity> {
        val byId = apiCategories.associateBy { it.categoryId }.toMutableMap()
        var nextOrder = (apiCategories.maxOfOrNull { it.orderIndex } ?: -1) + 1
        streamCategoryIds.filter { it.isNotBlank() }.forEach { categoryId ->
            if (!byId.containsKey(categoryId)) {
                byId[categoryId] = SeriesCategoryEntity(
                    profileId = profileId,
                    categoryId = categoryId,
                    categoryName = "Categoria $categoryId",
                    parentId = 0,
                    orderIndex = nextOrder++
                )
            }
        }
        return byId.values.sortedBy { it.orderIndex }
    }

    private suspend fun <T> retryApi(label: String, block: suspend () -> T): T {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "$label attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        throw lastException ?: Exception("$label failed after $MAX_RETRIES attempts")
    }

    private suspend fun markInProgress(profileId: Int, module: ContentModule) {
        val existing = syncStatusDao.getStatus(profileId, module.key)
        syncStatusDao.upsert(
            (existing ?: SyncStatusEntity(profileId = profileId, module = module.key)).copy(status = "IN_PROGRESS")
        )
    }

    private suspend fun markSuccess(profileId: Int, module: ContentModule, report: SyncReport) {
        syncStatusDao.upsert(
            SyncStatusEntity(
                profileId = profileId,
                module = module.key,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastItemCount = report.itemCount,
                lastCategoryCount = report.categoryCount,
                lastDurationMs = report.durationMs,
                status = "SUCCESS"
            )
        )
    }

    private suspend fun markFailed(profileId: Int, module: ContentModule) {
        val existing = syncStatusDao.getStatus(profileId, module.key)
        syncStatusDao.upsert(
            (existing ?: SyncStatusEntity(profileId = profileId, module = module.key)).copy(status = "FAILED")
        )
    }

    private fun readNextLiveDtoSafely(reader: JsonReader): LiveStreamDto? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        return try {
            tolerantGson.fromJson<LiveStreamDto>(reader, LiveStreamDto::class.java)
        } catch (e: Exception) {
            runCatching { reader.skipValue() }
            Log.w(TAG, "Skipping malformed LIVE item: ${e.message}")
            null
        }
    }

    private fun readNextVodDtoSafely(reader: JsonReader): VodStreamDto? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        return try {
            tolerantGson.fromJson<VodStreamDto>(reader, VodStreamDto::class.java)
        } catch (e: Exception) {
            runCatching { reader.skipValue() }
            Log.w(TAG, "Skipping malformed VOD item: ${e.message}")
            null
        }
    }

    private fun readNextSeriesDtoSafely(reader: JsonReader): SeriesStreamDto? {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        return try {
            tolerantGson.fromJson<SeriesStreamDto>(reader, SeriesStreamDto::class.java)
        } catch (e: Exception) {
            runCatching { reader.skipValue() }
            Log.w(TAG, "Skipping malformed SERIES item: ${e.message}")
            null
        }
    }
}
