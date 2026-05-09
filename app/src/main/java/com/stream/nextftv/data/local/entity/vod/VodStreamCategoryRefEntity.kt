package com.stream.nextftv.data.local.entity.vod

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vod_stream_category_refs",
    primaryKeys = ["profileId", "streamId", "categoryId"],
    indices = [
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "streamId"])
    ]
)
data class VodStreamCategoryRefEntity(
    val profileId: Int,
    val streamId: Int,
    val categoryId: String
)
