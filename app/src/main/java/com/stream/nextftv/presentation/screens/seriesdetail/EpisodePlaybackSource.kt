package com.stream.nextftv.presentation.screens.seriesdetail

import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.data.remote.SeriesEpisodeDto
import java.io.File

data class EpisodePlaybackSource(
    val url: String,
    val isLocal: Boolean
)

fun resolveEpisodePlaybackSource(
    episode: SeriesEpisodeDto?,
    profileUrl: String?,
    profileUsername: String?,
    profilePassword: String?,
    download: DownloadEntity?
): EpisodePlaybackSource? {
    val localPath = download
        ?.takeIf { it.status == "completed" }
        ?.filePath
        ?.takeIf { path ->
            val file = File(path)
            val fileLength = if (file.exists() && file.isFile) file.length() else 0L
            val expectedSize = download.totalSize.coerceAtLeast(0L)
            val hasExpectedBytes = expectedSize <= 0L || fileLength >= expectedSize
            file.exists() && file.isFile && fileLength > 0L && hasExpectedBytes
        }

    if (localPath != null) {
        return EpisodePlaybackSource(
            url = localPath,
            isLocal = true
        )
    }

    val episodeId = episode?.id?.takeIf { it.isNotBlank() } ?: return null
    val baseUrl = profileUrl?.takeIf { it.isNotBlank() } ?: return null
    val username = profileUsername?.takeIf { it.isNotBlank() } ?: return null
    val password = profilePassword?.takeIf { it.isNotBlank() } ?: return null
    val extension = episode.containerExtension?.takeIf { it.isNotBlank() } ?: "mp4"

    return EpisodePlaybackSource(
        url = "${baseUrl}series/${username}/${password}/${episodeId}.${extension}",
        isLocal = false
    )
}
