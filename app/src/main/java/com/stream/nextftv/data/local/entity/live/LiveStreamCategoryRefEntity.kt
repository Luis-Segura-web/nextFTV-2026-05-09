package com.stream.nextftv.data.local.entity.live

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "live_stream_category_refs",
    primaryKeys = ["profileId", "streamId", "categoryId"],
    indices = [
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "streamId"])
    ]
)
data class LiveStreamCategoryRefEntity(
    val profileId: Int,
    val streamId: Int,
    val categoryId: String
)
