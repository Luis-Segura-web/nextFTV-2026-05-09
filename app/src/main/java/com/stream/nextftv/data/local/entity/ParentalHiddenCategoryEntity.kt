package com.stream.nextftv.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "parental_hidden_categories",
    primaryKeys = ["profileId", "contentType", "categoryId"]
)
data class ParentalHiddenCategoryEntity(
    val profileId: Int,
    val contentType: String,
    val categoryId: String,
    val updatedAt: Long = System.currentTimeMillis()
)
