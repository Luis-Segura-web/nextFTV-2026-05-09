package com.stream.nextftv.data.local.entity.vod

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vod_categories",
    primaryKeys = ["profileId", "categoryId"],
    indices = [Index(value = ["profileId"])]
)
data class VodCategoryEntity(
    val profileId: Int,
    val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0,
    val orderIndex: Int = 0
)
