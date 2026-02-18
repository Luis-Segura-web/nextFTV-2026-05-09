package com.stream.iptvrevolut.data.repository

import android.content.Context
import androidx.work.*
import com.stream.iptvrevolut.data.local.dao.DownloadDao
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity
import com.stream.iptvrevolut.data.worker.download.DownloadWorker
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) : DownloadRepository {

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
        val downloadId = "${streamId}_$type"
        val filePath = File(context.filesDir, "downloads/$fileName").absolutePath

        val entity = DownloadEntity(
            id = downloadId,
            streamId = streamId,
            type = type,
            title = title,
            url = url,
            filePath = filePath,
            status = "queued",
            progress = 0f,
            profileId = profile.id,
            parentId = parentId,
            parentName = parentName,
            posterUrl = posterUrl
        )

        downloadDao.insertDownload(entity)
        triggerWorker()
    }

    private fun triggerWorker() {
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
        val file = File(download.filePath)
        if (file.exists()) file.delete()
        downloadDao.deleteDownload(download)
        // No cancelamos el worker porque podría estar descargando otra cosa
    }

    override suspend fun pauseDownload(downloadId: String) {
        downloadDao.updateStatus(downloadId, "paused")
        // Como el Worker está en un bucle, detectará el isStopped o el cambio de estado
        // Pero para ser inmediatos, cancelamos la cola única
        workManager.cancelUniqueWork(QUEUE_WORK_NAME)
    }

    override suspend fun resumeDownload(download: DownloadEntity) {
        downloadDao.updateStatus(download.id, "queued")
        triggerWorker()
    }
}
