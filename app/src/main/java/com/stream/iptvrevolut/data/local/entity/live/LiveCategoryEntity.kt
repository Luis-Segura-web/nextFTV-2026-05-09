package com.stream.iptvrevolut.data.local.entity.live

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "live_categories",
    primaryKeys = ["profileId", "categoryId"],
    indices = [Index(value = ["profileId"])]
)
data class LiveCategoryEntity(
    val profileId: Int,
    val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0,
    val orderIndex: Int = 0
)
