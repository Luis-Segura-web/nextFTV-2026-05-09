package com.stream.nextftv.data.local.entity.series

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "series_stream_category_refs",
    primaryKeys = ["profileId", "seriesId", "categoryId"],
    indices = [
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "seriesId"])
    ]
)
data class SeriesStreamCategoryRefEntity(
    val profileId: Int,
    val seriesId: Int,
    val categoryId: String
)
