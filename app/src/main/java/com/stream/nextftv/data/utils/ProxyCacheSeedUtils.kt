package com.stream.nextftv.data.utils

import android.content.Context
import java.io.File
import java.security.MessageDigest

object ProxyCacheSeedUtils {
    fun seedDownloadFromProxyCache(
        context: Context,
        type: String,
        url: String,
        targetFile: File
    ): Long {
        val candidate = findBestProxyCacheFile(context, type, url) ?: return targetFile.lengthOrZero()
        val currentSize = targetFile.lengthOrZero()
        val candidateSize = candidate.lengthOrZero()

        if (candidateSize <= 0L || candidateSize <= currentSize) return currentSize

        targetFile.parentFile?.mkdirs()
        val tempTarget = File(targetFile.parentFile, "${targetFile.name}.seed")

        candidate.inputStream().use { input ->
            tempTarget.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (targetFile.exists()) {
            targetFile.delete()
        }

        if (!tempTarget.renameTo(targetFile)) {
            tempTarget.copyTo(targetFile, overwrite = true)
            tempTarget.delete()
        }

        return targetFile.lengthOrZero()
    }

    fun findBestProxyCacheFile(context: Context, type: String, url: String): File? {
        val playTag = PlaybackCacheLocator.playTagForDownloadType(type) ?: return null
        val cacheDirectory = PlaybackCacheLocator.cacheDirectory(context, playTag)
        if (!cacheDirectory.exists()) return null

        val baseName = md5(url)
        val completed = File(cacheDirectory, baseName)
        val inProgress = File(cacheDirectory, "$baseName.download")

        return sequenceOf(completed, inProgress)
            .filter { it.exists() && it.isFile }
            .maxByOrNull { it.lengthOrZero() }
    }

    private fun md5(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray())
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                append("%02x".format(byte))
            }
        }
    }

    private fun File.lengthOrZero(): Long = if (exists() && isFile) length() else 0L
}
