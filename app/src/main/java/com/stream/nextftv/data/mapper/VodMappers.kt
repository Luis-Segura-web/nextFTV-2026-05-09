package com.stream.nextftv.data.mapper

import com.stream.nextftv.data.local.entity.vod.VodCategoryEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamCategoryRefEntity
import com.stream.nextftv.data.remote.VodCategoryDto
import com.stream.nextftv.data.remote.VodStreamDto
import com.stream.nextftv.data.utils.StringUtils

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
        rating5Based = rating5Based,
        added = added,
        customSid = customSid,
        containerExtension = containerExtension,
        directSource = directSource,
        backdropPath = backdropPath,
        tmdbId = tmdbId,
        backdropUrl = backdropPath?.firstOrNull()
    )
}

fun VodStreamDto.toCategoryRef(profileId: Int): VodStreamCategoryRefEntity? {
    val safeStreamId = streamId ?: return null
    return VodStreamCategoryRefEntity(
        profileId = profileId,
        streamId = safeStreamId,
        categoryId = categoryId ?: "0"
    )
}

fun List<VodCategoryDto>.toEntities(profileId: Int) = mapIndexed { index, item ->
    item.toEntity(profileId, index)
}
