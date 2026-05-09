package com.stream.nextftv.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stream.nextftv.data.local.entity.ParentalHiddenCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentalControlDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideCategory(item: ParentalHiddenCategoryEntity)

    @Query(
        """
        DELETE FROM parental_hidden_categories
        WHERE profileId = :profileId
        AND contentType = :contentType
        AND categoryId = :categoryId
        """
    )
    suspend fun showCategory(profileId: Int, contentType: String, categoryId: String)

    @Query(
        """
        SELECT categoryId
        FROM parental_hidden_categories
        WHERE profileId = :profileId
        AND contentType = :contentType
        """
    )
    fun observeHiddenCategoryIds(profileId: Int, contentType: String): Flow<List<String>>
}
