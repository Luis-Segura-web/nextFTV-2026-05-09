package com.stream.nextftv.data.sync

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.stream.nextftv.data.local.dao.DetailCacheDao
import com.stream.nextftv.data.local.dao.FavoriteDao
import com.stream.nextftv.data.local.dao.LiveTvDao
import com.stream.nextftv.data.local.dao.RecentDao
import com.stream.nextftv.data.local.dao.SeriesDao
import com.stream.nextftv.data.local.dao.SyncStatusDao
import com.stream.nextftv.data.local.dao.VodDao
import com.stream.nextftv.data.local.entity.SyncStatusEntity
import com.stream.nextftv.data.local.entity.live.LiveCategoryEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamCategoryRefEntity
import com.stream.nextftv.data.local.entity.series.SeriesCategoryEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamCategoryRefEntity
import com.stream.nextftv.data.local.entity.vod.VodCategoryEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamCategoryRefEntity
import com.stream.nextftv.data.mapper.toEntities
import com.stream.nextftv.data.mapper.toCategoryRef
import com.stream.nextftv.data.mapper.toEntity
import com.stream.nextftv.data.remote.LiveStreamDto
import com.stream.nextftv.data.remote.SeriesStreamDto
import com.stream.nextftv.data.remote.VodStreamDto
import com.stream.nextftv.data.remote.XtreamApiService
import com.stream.nextftv.data.remote.utils.SafeBooleanAdapter
import com.stream.nextftv.data.remote.utils.SafeDoubleAdapter
import com.stream.nextftv.data.remote.utils.SafeIntAdapter
import com.stream.nextftv.data.remote.utils.SafeListStringAdapter
import com.stream.nextftv.data.remote.utils.SafeLongAdapter
import com.stream.nextftv.data.remote.utils.SafeStringAdapter
import com.stream.nextftv.domain.model.ServerProfile
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
        .registerTypeAdapter(Int::class.javaPrimitiveType, SafeIntAdapter())
        .registerTypeAdapter(Int::class.javaObjectType, SafeIntAdapter())
        .registerTypeAdapter(Long::class.javaPrimitiveType, SafeLongAdapter())
        .registerTypeAdapter(Long::class.javaObjectType, SafeLongAdapter())
        .registerTypeAdapter(Double::class.javaPrimitiveType, SafeDoubleAdapter())
        .registerTypeAdapter(Double::class.javaObjectType, SafeDoubleAdapter())
        .registerTypeAdapter(Boolean::class.javaPrimitiveType, SafeBooleanAdapter())
        .registerTypeAdapter(Boolean::class.javaObjectType, SafeBooleanAdapter())
        .registerTypeAdapter(String::class.java, SafeStringAdapter())
        .registerTypeAdapter(
            object : com.google.gson.reflect.TypeToken<List<String>>() {}.type,
            SafeListStringAdapter()
        )
        .setStrictness(Strictness.LENIENT)
        .create()

    suspend fun syncModule(
        profile: ServerProfile,
        module: ContentModule,
        onProgress: ((SyncProgressStep) -> Unit)? = null
    ): SyncReport {
        val startTime = System.currentTimeMillis()
        markInProgress(profile.id, module)

        return try {
            val report = when (module) {
                ContentModule.LIVE -> syncLive(profile, onProgress)
                ContentModule.VOD -> syncVod(profile, onProgress)
                ContentModule.SERIES -> syncSeries(profile, onProgress)
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

    suspend fun syncAll(
        profile: ServerProfile,
        onProgress: ((SyncProgressStep) -> Unit)? = null
    ): List<SyncReport> {
        return ContentModule.entries.map { module -> syncModule(profile, module, onProgress) }
    }

    suspend fun isSyncNeeded(profileId: Int, module: ContentModule, intervalMs: Long): Boolean {
        val status = syncStatusDao.getStatus(profileId, module.key) ?: return true
        return (System.currentTimeMillis() - status.lastSyncTimestamp) > intervalMs
    }

    suspend fun getLastSyncTimestamp(profileId: Int, module: ContentModule): Long {
        return syncStatusDao.getStatus(profileId, module.key)?.lastSyncTimestamp ?: 0L
    }

    suspend fun clearStreamingContent(profileId: Int) {
        withContext(Dispatchers.IO) {
            detailCacheDao.clearVodCache()
            detailCacheDao.clearSeriesEpisodesCache()

            liveTvDao.deleteCategoriesByProfile(profileId)
            liveTvDao.deleteStreamsByProfile(profileId)
            vodDao.deleteCategoriesByProfile(profileId)
            vodDao.deleteStreamsByProfile(profileId)
            seriesDao.deleteCategoriesByProfile(profileId)
            seriesDao.deleteStreamsByProfile(profileId)

            favoriteDao.clearOrphanLiveFavorites(profileId)
            favoriteDao.clearOrphanVodFavorites(profileId)
            favoriteDao.clearOrphanSeriesFavorites(profileId)
            recentDao.clearOrphanLiveRecents(profileId)
            recentDao.clearOrphanVodRecents(profileId)
            recentDao.clearOrphanSeriesRecents(profileId)

            syncStatusDao.deleteByProfile(profileId)
        }
    }

    private suspend fun syncLive(
        profile: ServerProfile,
        onProgress: ((SyncProgressStep) -> Unit)?
    ): SyncReport {
        val profileId = profile.id
        onProgress?.invoke(SyncProgressStep(ContentModule.LIVE, "Descargando categorias...", 25))

        val apiCategories = retryApi("Live categories") {
            api.getLiveCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
        }.toEntities(profileId)

        onProgress?.invoke(SyncProgressStep(ContentModule.LIVE, "Descargando canales...", 85))
        val existingRefs = liveTvDao.getAllStreamCategoryRefsSnapshot(profileId)
        val (itemCount, categoryIds, remoteRefs) = syncLiveStreamsRaw(profile)
        val categories = mergeLiveCategories(profileId, apiCategories, categoryIds)
        liveTvDao.insertCategories(categories)
        val staleRefs = existingRefs.filterNot(remoteRefs::contains)
        if (staleRefs.isNotEmpty()) {
            liveTvDao.deleteStreamCategoryRefs(staleRefs)
        }
        val existingCategories = liveTvDao.getAllCategoriesSnapshot(profileId)
        val validCategoryIds = categories.mapTo(linkedSetOf()) { it.categoryId }
        val staleCategories = existingCategories.filterNot { it.categoryId in validCategoryIds }
        if (staleCategories.isNotEmpty()) {
            liveTvDao.deleteCategories(staleCategories)
        }
        liveTvDao.deleteOrphanStreams(profileId)

        favoriteDao.clearOrphanLiveFavorites(profileId)
        recentDao.clearOrphanLiveRecents(profileId)

        return SyncReport(
            module = ContentModule.LIVE,
            categoryCount = categories.size,
            itemCount = itemCount,
            success = true
        )
    }

    private suspend fun syncVod(
        profile: ServerProfile,
        onProgress: ((SyncProgressStep) -> Unit)?
    ): SyncReport {
        val profileId = profile.id
        detailCacheDao.clearVodCache()
        onProgress?.invoke(SyncProgressStep(ContentModule.VOD, "Descargando categorias...", 25))

        val apiCategories = retryApi("VOD categories") {
            api.getVodCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
        }.toEntities(profileId)

        onProgress?.invoke(SyncProgressStep(ContentModule.VOD, "Descargando peliculas...", 85))
        val existingRefs = vodDao.getAllStreamCategoryRefsSnapshot(profileId)
        val (itemCount, categoryIds, remoteRefs) = syncVodStreamsRaw(profile)
        val categories = mergeVodCategories(profileId, apiCategories, categoryIds)
        vodDao.insertCategories(categories)
        val staleRefs = existingRefs.filterNot(remoteRefs::contains)
        if (staleRefs.isNotEmpty()) {
            vodDao.deleteStreamCategoryRefs(staleRefs)
        }
        val existingCategories = vodDao.getAllCategoriesSnapshot(profileId)
        val validCategoryIds = categories.mapTo(linkedSetOf()) { it.categoryId }
        val staleCategories = existingCategories.filterNot { it.categoryId in validCategoryIds }
        if (staleCategories.isNotEmpty()) {
            vodDao.deleteCategories(staleCategories)
        }
        vodDao.deleteOrphanStreams(profileId)

        favoriteDao.clearOrphanVodFavorites(profileId)
        recentDao.clearOrphanVodRecents(profileId)

        return SyncReport(
            module = ContentModule.VOD,
            categoryCount = categories.size,
            itemCount = itemCount,
            success = true
        )
    }

    private suspend fun syncSeries(
        profile: ServerProfile,
        onProgress: ((SyncProgressStep) -> Unit)?
    ): SyncReport {
        val profileId = profile.id
        detailCacheDao.clearSeriesEpisodesCache()
        onProgress?.invoke(SyncProgressStep(ContentModule.SERIES, "Descargando categorias...", 25))

        val apiCategories = retryApi("Series categories") {
            api.getSeriesCategories(
                url = "${profile.url}player_api.php",
                username = profile.username,
                password = profile.password
            )
        }.toEntities(profileId)

        onProgress?.invoke(SyncProgressStep(ContentModule.SERIES, "Descargando series...", 85))
        val existingRefs = seriesDao.getAllStreamCategoryRefsSnapshot(profileId)
        val (itemCount, categoryIds, remoteRefs) = syncSeriesStreamsRaw(profile)
        val categories = mergeSeriesCategories(profileId, apiCategories, categoryIds)
        seriesDao.insertCategories(categories)
        val staleRefs = existingRefs.filterNot(remoteRefs::contains)
        if (staleRefs.isNotEmpty()) {
            seriesDao.deleteStreamCategoryRefs(staleRefs)
        }
        val existingCategories = seriesDao.getAllCategoriesSnapshot(profileId)
        val validCategoryIds = categories.mapTo(linkedSetOf()) { it.categoryId }
        val staleCategories = existingCategories.filterNot { it.categoryId in validCategoryIds }
        if (staleCategories.isNotEmpty()) {
            seriesDao.deleteCategories(staleCategories)
        }
        seriesDao.deleteOrphanStreams(profileId)

        favoriteDao.clearOrphanSeriesFavorites(profileId)
        recentDao.clearOrphanSeriesRecents(profileId)

        return SyncReport(
            module = ContentModule.SERIES,
            categoryCount = categories.size,
            itemCount = itemCount,
            success = true
        )
    }

    private suspend fun syncLiveStreamsRaw(profile: ServerProfile): Triple<Int, Set<String>, Set<LiveStreamCategoryRefEntity>> {
        val baseUrl = "${profile.url}player_api.php"
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    val categoryIds = linkedSetOf<String>()
                    val refs = linkedSetOf<LiveStreamCategoryRefEntity>()
                    var total = 0
                    api.getLiveStreamsRaw(
                        url = baseUrl,
                        username = profile.username,
                        password = profile.password
                    ).use { body ->
                        JsonReader(body.charStream()).use { reader ->
                            reader.setStrictness(Strictness.LENIENT)
                            total = parseLiveRoot(profile.id, reader, categoryIds, refs)
                        }
                    }
                    Triple(total, categoryIds, refs)
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Live raw sync attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }

        throw lastException ?: Exception("Live raw sync failed after $MAX_RETRIES attempts")
    }

    private suspend fun syncVodStreamsRaw(profile: ServerProfile): Triple<Int, Set<String>, Set<VodStreamCategoryRefEntity>> {
        val baseUrl = "${profile.url}player_api.php"
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    val categoryIds = linkedSetOf<String>()
                    val refs = linkedSetOf<VodStreamCategoryRefEntity>()
                    var total = 0
                    api.getVodStreamsRaw(
                        url = baseUrl,
                        username = profile.username,
                        password = profile.password
                    ).use { body ->
                        JsonReader(body.charStream()).use { reader ->
                            reader.setStrictness(Strictness.LENIENT)
                            total = parseVodRoot(profile.id, reader, categoryIds, refs)
                        }
                    }
                    Triple(total, categoryIds, refs)
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "VOD raw sync attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }

        throw lastException ?: Exception("VOD raw sync failed after $MAX_RETRIES attempts")
    }

    private suspend fun parseLiveRoot(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<LiveStreamCategoryRefEntity>
    ): Int {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> parseLiveArray(profileId, reader, categoryIds, refs)
            JsonToken.BEGIN_OBJECT -> parseLiveObject(profileId, reader, categoryIds, refs)
            else -> {
                reader.skipValue()
                throw IllegalStateException("Unexpected live payload token: ${reader.peek()}")
            }
        }
    }

    private suspend fun parseLiveObject(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<LiveStreamCategoryRefEntity>
    ): Int {
        var total = 0
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key == "data" || key == "results" || key == "streams" || key == "items") {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    total += parseLiveArray(profileId, reader, categoryIds, refs)
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

    private suspend fun parseLiveArray(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<LiveStreamCategoryRefEntity>
    ): Int {
        var count = 0
        val batch = ArrayList<LiveStreamEntity>(BATCH_SIZE)
        val relationBatch = ArrayList<LiveStreamCategoryRefEntity>(BATCH_SIZE)
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = readNextLiveDtoSafely(reader)
            if (dto?.streamId != null) {
                val relation = dto.toCategoryRef(profileId)
                val entity = dto.toEntity(profileId)
                if (relation != null) {
                    categoryIds += relation.categoryId
                    refs += relation
                    relationBatch.add(relation)
                }
                batch += entity
                if (batch.size >= BATCH_SIZE) {
                    liveTvDao.insertStreams(batch.toList())
                    if (relationBatch.isNotEmpty()) {
                        liveTvDao.insertStreamCategoryRefs(relationBatch.toList())
                        relationBatch.clear()
                    }
                    count += batch.size
                    batch.clear()
                }
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) {
            liveTvDao.insertStreams(batch.toList())
            if (relationBatch.isNotEmpty()) {
                liveTvDao.insertStreamCategoryRefs(relationBatch.toList())
            }
            count += batch.size
        }
        return count
    }

    private suspend fun parseVodRoot(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<VodStreamCategoryRefEntity>
    ): Int {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> parseVodArray(profileId, reader, categoryIds, refs)
            JsonToken.BEGIN_OBJECT -> parseVodObject(profileId, reader, categoryIds, refs)
            else -> {
                reader.skipValue()
                throw IllegalStateException("Unexpected vod payload token: ${reader.peek()}")
            }
        }
    }

    private suspend fun parseVodObject(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<VodStreamCategoryRefEntity>
    ): Int {
        var total = 0
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key == "data" || key == "results" || key == "streams" || key == "items") {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    total += parseVodArray(profileId, reader, categoryIds, refs)
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

    private suspend fun parseVodArray(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<VodStreamCategoryRefEntity>
    ): Int {
        var count = 0
        val batch = ArrayList<VodStreamEntity>(BATCH_SIZE)
        val relationBatch = ArrayList<VodStreamCategoryRefEntity>(BATCH_SIZE)
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = readNextVodDtoSafely(reader)
            if (dto?.streamId != null) {
                val relation = dto.toCategoryRef(profileId)
                val entity = dto.toEntity(profileId)
                if (relation != null) {
                    categoryIds += relation.categoryId
                    refs += relation
                    relationBatch.add(relation)
                }
                batch += entity
                if (batch.size >= BATCH_SIZE) {
                    vodDao.insertStreams(batch.toList())
                    if (relationBatch.isNotEmpty()) {
                        vodDao.insertStreamCategoryRefs(relationBatch.toList())
                        relationBatch.clear()
                    }
                    count += batch.size
                    batch.clear()
                }
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) {
            vodDao.insertStreams(batch.toList())
            if (relationBatch.isNotEmpty()) {
                vodDao.insertStreamCategoryRefs(relationBatch.toList())
            }
            count += batch.size
        }
        return count
    }

    private suspend fun parseSeriesRoot(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<SeriesStreamCategoryRefEntity>
    ): Int {
        return when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> parseSeriesArray(profileId, reader, categoryIds, refs)
            JsonToken.BEGIN_OBJECT -> parseSeriesObject(profileId, reader, categoryIds, refs)
            else -> {
                reader.skipValue()
                throw IllegalStateException("Unexpected series payload token: ${reader.peek()}")
            }
        }
    }

    private suspend fun parseSeriesObject(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<SeriesStreamCategoryRefEntity>
    ): Int {
        var total = 0
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key == "data" || key == "results" || key == "series" || key == "items") {
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    total += parseSeriesArray(profileId, reader, categoryIds, refs)
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

    private suspend fun parseSeriesArray(
        profileId: Int,
        reader: JsonReader,
        categoryIds: MutableSet<String>,
        refs: MutableSet<SeriesStreamCategoryRefEntity>
    ): Int {
        var count = 0
        val batch = ArrayList<SeriesStreamEntity>(BATCH_SIZE)
        val relationBatch = ArrayList<SeriesStreamCategoryRefEntity>(BATCH_SIZE)
        reader.beginArray()
        while (reader.hasNext()) {
            val dto = readNextSeriesDtoSafely(reader)
            if (dto?.seriesId != null) {
                val relation = dto.toCategoryRef(profileId)
                val entity = dto.toEntity(profileId)
                if (relation != null) {
                    categoryIds += relation.categoryId
                    refs += relation
                    relationBatch.add(relation)
                }
                batch += entity
                if (batch.size >= BATCH_SIZE) {
                    seriesDao.insertStreams(batch.toList())
                    if (relationBatch.isNotEmpty()) {
                        seriesDao.insertStreamCategoryRefs(relationBatch.toList())
                        relationBatch.clear()
                    }
                    count += batch.size
                    batch.clear()
                }
            }
        }
        reader.endArray()
        if (batch.isNotEmpty()) {
            seriesDao.insertStreams(batch.toList())
            if (relationBatch.isNotEmpty()) {
                seriesDao.insertStreamCategoryRefs(relationBatch.toList())
            }
            count += batch.size
        }
        return count
    }

    private suspend fun syncSeriesStreamsRaw(profile: ServerProfile): Triple<Int, Set<String>, Set<SeriesStreamCategoryRefEntity>> {
        val baseUrl = "${profile.url}player_api.php"
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return withContext(Dispatchers.IO) {
                    val categoryIds = linkedSetOf<String>()
                    val refs = linkedSetOf<SeriesStreamCategoryRefEntity>()
                    var total = 0
                    api.getSeriesRaw(
                        url = baseUrl,
                        username = profile.username,
                        password = profile.password
                    ).use { body ->
                        JsonReader(body.charStream()).use { reader ->
                            reader.setStrictness(Strictness.LENIENT)
                            total = parseSeriesRoot(profile.id, reader, categoryIds, refs)
                        }
                    }
                    Triple(total, categoryIds, refs)
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Series raw sync attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }

        throw lastException ?: Exception("Series raw sync failed after $MAX_RETRIES attempts")
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
