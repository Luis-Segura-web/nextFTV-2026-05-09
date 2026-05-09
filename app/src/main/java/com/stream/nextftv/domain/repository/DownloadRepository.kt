package com.stream.nextftv.domain.repository

import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.domain.model.ServerProfile
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloads(profileId: Int): Flow<List<DownloadEntity>>
    
    suspend fun addDownload(
        profile: ServerProfile,
        streamId: Int,
        title: String,
        url: String,
        type: String,
        fileName: String,
        posterUrl: String? = null,
        parentId: Int? = null,
        parentName: String? = null
    )
    
    suspend fun removeDownload(download: DownloadEntity)
    suspend fun pauseDownload(downloadId: String)
    suspend fun resumeDownload(download: DownloadEntity)
}
