package com.stream.nextftv.data.sync

data class SyncProgressStep(
    val module: ContentModule,
    val message: String,
    val progress: Int
)
