package com.stream.nextftv.data.repository

import android.content.Context
import android.util.Log
import androidx.work.*
import com.stream.nextftv.data.local.dao.DownloadDao
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.data.utils.ProxyCacheSeedUtils
import com.stream.nextftv.data.worker.download.DownloadCancellationRegistry
import com.stream.nextftv.data.worker.download.DownloadWorker
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) : DownloadRepository {
    private companion object {
        const val TAG = "DownloadRepository"
    }

    private val workManager = WorkManager.getInstance(context)
    private val QUEUE_WORK_NAME = "iptv_global_serial_download"

    override fun getDownloads(profileId: Int): Flow<List<DownloadEntity>> {
        return downloadDao.getDownloadsByProfile(profileId)
    }

    override suspend fun addDownload(
        profile: ServerProfile,
        streamId: Int,
        title: String,
        url: String,
        type: String,
        fileName: String,
        posterUrl: String?,
        parentId: Int?,
        parentName: String?
    ) {
        val downloadId = "${profile.id}_${streamId}_$type"
        val safeFileName = File(fileName).name
        val targetFile = File(context.filesDir, "downloads/${profile.id}/$downloadId-$safeFileName")
        val seededBytes = ProxyCacheSeedUtils.seedDownloadFromProxyCache(context, type, url, targetFile)
        val filePath = targetFile.absolutePath
        Log.d(TAG, "addDownload id=$downloadId type=$type url=$url seededBytes=$seededBytes path=$filePath")

        val entity = DownloadEntity(
            id = downloadId,
            streamId = streamId,
            type = type,
            title = title,
            url = url,
            filePath = filePath,
            status = "queued",
            progress = 0f,
            downloadedSize = seededBytes,
            profileId = profile.id,
            parentId = parentId,
            parentName = parentName,
            posterUrl = posterUrl
        )

        downloadDao.insertDownload(entity)
        triggerWorker()
    }

    private fun triggerWorker() {
        Log.d(TAG, "triggerWorker queueName=$QUEUE_WORK_NAME")
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag("download_queue")
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // KEEP asegura que si ya hay un Worker limpiando la cola, no se cree otro
        // Si no hay ninguno, lo inicia. Esto garantiza 1 sola conexión.
        workManager.enqueueUniqueWork(QUEUE_WORK_NAME, ExistingWorkPolicy.KEEP, downloadRequest)
    }

    override suspend fun removeDownload(download: DownloadEntity) {
        Log.d(TAG, "removeDownload id=${download.id} path=${download.filePath}")
        val wasActive = downloadDao.getDownloadById(download.id)?.status == "downloading"
        if (wasActive) {
            DownloadCancellationRegistry.requestCancel(download.id)
            downloadDao.updateStatus(download.id, "cancelled")
        }
        val file = File(download.filePath)
        if (file.exists()) file.delete()
        downloadDao.deleteDownload(download)
    }

    override suspend fun pauseDownload(downloadId: String) {
        Log.d(TAG, "pauseDownload id=$downloadId")
        downloadDao.updateStatus(downloadId, "paused")
    }

    override suspend fun resumeDownload(download: DownloadEntity) {
        Log.d(TAG, "resumeDownload id=${download.id} status=${download.status}")
        downloadDao.updateStatus(download.id, "queued")
        triggerWorker()
    }
}
