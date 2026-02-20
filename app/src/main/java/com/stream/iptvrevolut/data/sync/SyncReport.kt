package com.stream.iptvrevolut.data.sync

/**
 * Result data from a single module sync operation.
 */
data class SyncReport(
    val module: ContentModule,
    val categoryCount: Int = 0,
    val itemCount: Int = 0,
    val durationMs: Long = 0L,
    val success: Boolean = true,
    val error: String? = null
)

