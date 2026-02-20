package com.stream.iptvrevolut.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Tracks sync status per profile per content module.
 * Replaces the lastLiveSync/lastMoviesSync/lastSeriesSync columns on ProfileEntity.
 */
@Entity(
    tableName = "sync_status",
    primaryKeys = ["profileId", "module"],
    indices = [Index(value = ["profileId"])]
)
data class SyncStatusEntity(
    val profileId: Int,
    val module: String, // "live", "movies", "series"
    val lastSyncTimestamp: Long = 0L,
    val lastItemCount: Int = 0,
    val lastCategoryCount: Int = 0,
    val lastDurationMs: Long = 0L,
    val status: String = "IDLE" // IDLE, IN_PROGRESS, SUCCESS, FAILED
)

