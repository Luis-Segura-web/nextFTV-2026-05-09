package com.stream.iptvrevolut.data.mapper

import com.stream.iptvrevolut.data.remote.LiveStreamDto
import com.stream.iptvrevolut.data.remote.SeriesStreamDto
import com.stream.iptvrevolut.data.remote.VodStreamDto
import com.stream.iptvrevolut.domain.model.canonical.CatalogItem
import com.stream.iptvrevolut.domain.model.canonical.MediaType

fun LiveStreamDto.toCanonical(): CatalogItem {
    return CatalogItem(
        id = streamId.toString(),
        type = MediaType.LIVE,
        title = (name ?: "Unknown").trim(),
        categoryId = categoryId,
        posterUrl = streamIcon?.takeIf { it.startsWith("http") },
        epgChannelId = epgChannelId,
        directSource = directSource,
        addedAt = try { added?.toLong() } catch (e: Exception) { null }
    )
}

fun VodStreamDto.toCanonical(): CatalogItem {
    return CatalogItem(
        id = streamId.toString(),
        type = MediaType.MOVIE,
        title = (name ?: "Unknown").trim(),
        categoryId = categoryId,
        posterUrl = streamIcon?.takeIf { it.startsWith("http") },
        containerExtension = containerExtension,
        rating10 = rating5Based?.times(2) ?: try { rating?.toDouble() } catch (e: Exception) { null },
        addedAt = try { added?.toLong() } catch (e: Exception) { null }
    )
}

fun SeriesStreamDto.toCanonical(): CatalogItem {
    return CatalogItem(
        id = seriesId.toString(),
        type = MediaType.SERIES,
        title = (name ?: "Unknown").trim(),
        categoryId = categoryId,
        posterUrl = cover?.takeIf { it.startsWith("http") },
        rating10 = rating5Based?.times(2) ?: try { rating?.toDouble() } catch (e: Exception) { null },
        backdropUrl = backdropPath?.firstOrNull()
    )
}
