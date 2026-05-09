package com.stream.nextftv.data.local.entity.live

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "live_streams",
    primaryKeys = ["profileId", "streamId"],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["normalizedName"]),
        Index(value = ["naturalSortName"])
    ]
)
data class LiveStreamEntity(
    val profileId: Int,
    val streamId: Int,
    val num: Int, // Orden original del proveedor
    val name: String,
    val normalizedName: String, // Sin acentos, minúsculas
    val naturalSortName: String, // Para orden natural (sin espacios extra, ceros leading manejados)
    val streamType: String,
    val streamIcon: String?,
    val epgChannelId: String?,
    val added: String?,
    val customSid: String?,
    val tvArchive: Int,
    val directSource: String?,
    val tvArchiveDuration: Int
)
