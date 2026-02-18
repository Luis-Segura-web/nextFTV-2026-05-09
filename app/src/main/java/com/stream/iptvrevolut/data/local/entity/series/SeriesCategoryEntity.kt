package com.stream.iptvrevolut.data.local.entity.series

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "series_categories",
    primaryKeys = ["profileId", "categoryId"],
    indices = [Index(value = ["profileId"])]
)
data class SeriesCategoryEntity(
    val profileId: Int,
    val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0
)