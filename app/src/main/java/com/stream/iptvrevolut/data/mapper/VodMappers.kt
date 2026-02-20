package com.stream.iptvrevolut.data.mapper

import com.stream.iptvrevolut.data.local.entity.vod.VodCategoryEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodStreamEntity
import com.stream.iptvrevolut.data.remote.VodCategoryDto
import com.stream.iptvrevolut.data.remote.VodStreamDto
import com.stream.iptvrevolut.data.utils.StringUtils

fun VodCategoryDto.toEntity(profileId: Int, orderIndex: Int = 0) = VodCategoryEntity(
    profileId = profileId,
    categoryId = categoryId,
    categoryName = categoryName,
    parentId = parentId ?: 0,
    orderIndex = orderIndex
)

fun VodStreamDto.toEntity(profileId: Int): VodStreamEntity {
    val safeName = name ?: "Unknown"
    return VodStreamEntity(
        profileId = profileId,
        categoryId = categoryId ?: "0",
        streamId = streamId ?: 0,
        num = num ?: 0,
        name = safeName,
        normalizedName = StringUtils.normalize(safeName),
        naturalSortName = StringUtils.naturalSort(safeName),
        streamType = streamType ?: "movie",
        streamIcon = streamIcon,
        rating = rating,
        added = added,
        containerExtension = containerExtension,
        directSource = directSource,
        tmdbId = tmdbId,
        backdropUrl = backdropPath?.firstOrNull()
    )
}

fun List<VodCategoryDto>.toEntities(profileId: Int) = mapIndexed { index, item ->
    item.toEntity(profileId, index)
}
