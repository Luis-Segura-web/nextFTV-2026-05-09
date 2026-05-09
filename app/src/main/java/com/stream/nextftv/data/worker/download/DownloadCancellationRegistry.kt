package com.stream.nextftv.data.worker.download

import java.util.concurrent.ConcurrentHashMap

object DownloadCancellationRegistry {
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()

    fun requestCancel(downloadId: String) {
        cancelledIds.add(downloadId)
    }

    fun isCancellationRequested(downloadId: String): Boolean {
        return cancelledIds.contains(downloadId)
    }

    fun clear(downloadId: String) {
        cancelledIds.remove(downloadId)
    }
}
