package com.stream.iptvrevolut.data.worker.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.stream.iptvrevolut.data.local.dao.DownloadDao
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Named
import java.util.LinkedList

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao,
    @Named("XtreamClient") private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "download_channel"
    private val notificationId = 1001
    private val speedSamples = LinkedList<Long>()

    override suspend fun doWork(): Result {
        createNotificationChannel()

        while (true) {
            val download = downloadDao.getNextQueuedDownload() ?: break
            
            // Límite de 3 reintentos persistentes en base de datos
            if (download.retryCount >= 3) {
                downloadDao.updateStatus(download.id, "error")
                continue
            }

            val result = downloadSingleFile(download)
            
            when (result) {
                is DownloadResult.Success -> continue // Siguiente en la cola
                is DownloadResult.Paused -> return Result.success() // Detener worker
                is DownloadResult.Retry -> {
                    downloadDao.incrementRetryCount(download.id)
                    downloadDao.updateStatus(download.id, "retrying")
                    return Result.retry() // Reintentar con backoff de WorkManager
                }
                is DownloadResult.Error -> {
                    downloadDao.updateStatus(download.id, "error")
                    continue // Pasar al siguiente
                }
            }
        }
        return Result.success()
    }

    private suspend fun downloadSingleFile(download: DownloadEntity): DownloadResult {
        try {
            val attemptStr = if (download.retryCount > 0) " (Reintento ${download.retryCount}/3)" else ""
            setForeground(createForegroundInfo(download.title + attemptStr, 0))
            downloadDao.updateStatus(download.id, "downloading")
            speedSamples.clear()
            
            val file = File(context.filesDir, "downloads/${File(download.filePath).name}")
            file.parentFile?.mkdirs()
            val downloadedSoFar = if (file.exists()) file.length() else 0L

            val request = Request.Builder()
                .url(download.url)
                .apply { if (downloadedSoFar > 0) addHeader("Range", "bytes=$downloadedSoFar-") }
                .build()

            val response = okHttpClient.newCall(request).execute()
            val isResuming = response.code == 206
            
            if (!response.isSuccessful) {
                if (response.code == 404 || response.code == 403) return DownloadResult.Error("Fatal ${response.code}")
                return DownloadResult.Retry
            }

            val body = response.body ?: return DownloadResult.Error("Empty")
            val totalSize = if (isResuming) body.contentLength() + downloadedSoFar else body.contentLength()
            
            if (totalSize > 0 && totalSize > (context.filesDir.usableSpace - 100 * 1024 * 1024L)) {
                return DownloadResult.Error("No space")
            }

            var bytesDownloaded = if (isResuming) downloadedSoFar else 0L
            var lastUpdateBytes = bytesDownloaded
            var lastUpdateTime = System.currentTimeMillis()
            var hasProgressedInThisSession = false
            val buffer = ByteArray(64 * 1024)
            
            FileOutputStream(file, isResuming).use { outputStream ->
                body.byteStream().use { inputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            return DownloadResult.Paused
                        }
                        
                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        
                        // Si logramos escribir datos, reiniciamos el contador de reintentos
                        if (!hasProgressedInThisSession && bytesRead > 0) {
                            hasProgressedInThisSession = true
                            downloadDao.resetRetryCount(download.id)
                        }
                        
                        val now = System.currentTimeMillis()
                        val timeDiff = now - lastUpdateTime
                        
                        if (timeDiff >= 1000 || bytesDownloaded - lastUpdateBytes >= 2 * 1024 * 1024 || bytesDownloaded == totalSize) {
                            val speed = if (timeDiff > 0) ((bytesDownloaded - lastUpdateBytes) * 1000 / timeDiff) else 0L
                            speedSamples.addLast(speed)
                            if (speedSamples.size > 10) speedSamples.removeFirst()
                            val avgSpeed = speedSamples.average().toLong()

                            lastUpdateBytes = bytesDownloaded
                            lastUpdateTime = now
                            
                            val progress = if (totalSize > 0) (bytesDownloaded.toFloat() / totalSize) else 0f
                            downloadDao.updateProgress(download.id, progress, bytesDownloaded, avgSpeed)
                            notificationManager.notify(notificationId, createNotification("${download.title} (${formatSpeed(avgSpeed)})$attemptStr", (progress * 100).toInt()))
                        }
                    }
                }
            }

            downloadDao.updateStatus(download.id, "completed")
            return DownloadResult.Success

        } catch (e: IOException) {
            return DownloadResult.Retry
        } catch (e: Exception) {
            return DownloadResult.Error(e.message ?: "Unknown")
        }
    }

    private fun formatSpeed(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB/s", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> "${bytes / 1024} KB/s"
        else -> "$bytes B/s"
    }

    private sealed class DownloadResult {
        object Success : DownloadResult()
        object Paused : DownloadResult()
        object Retry : DownloadResult()
        data class Error(val message: String) : DownloadResult()
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = createNotification(title, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun createNotification(title: String, progress: Int) = NotificationCompat.Builder(context, channelId)
        .setContentTitle(title)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setOngoing(true)
        .setProgress(100, progress, progress <= 0)
        .setSilent(true)
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Descargas", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
