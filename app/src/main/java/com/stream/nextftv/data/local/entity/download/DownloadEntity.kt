package com.stream.nextftv.data.local.entity.download

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String, // streamId_type
    val streamId: Int,
    val type: String, // "movie", "episode"
    val title: String,
    val url: String,
    val filePath: String,
    val status: String, // "queued", "downloading", "completed", "paused", "cancelled", "error"
    val progress: Float,
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val downloadSpeed: Long = 0, // En bytes por segundo
    val retryCount: Int = 0,
    val profileId: Int,
    val parentId: Int? = null, // seriesId para episodios
    val parentName: String? = null, // Nombre de la serie
    val posterUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
