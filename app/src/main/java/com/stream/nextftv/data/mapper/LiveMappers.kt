package com.stream.nextftv.data.mapper

import com.stream.nextftv.data.local.entity.live.LiveCategoryEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamCategoryRefEntity
import com.stream.nextftv.data.remote.LiveCategoryDto
import com.stream.nextftv.data.remote.LiveStreamDto
import com.stream.nextftv.data.utils.StringUtils

fun LiveCategoryDto.toEntity(profileId: Int, orderIndex: Int = 0) = LiveCategoryEntity(
    profileId = profileId,
    categoryId = categoryId,
    categoryName = categoryName,
    parentId = parentId ?: 0,
    orderIndex = orderIndex
)

fun LiveStreamDto.toEntity(profileId: Int): LiveStreamEntity {
    val safeName = name ?: "Unknown"
    return LiveStreamEntity(
        profileId = profileId,
        streamId = streamId ?: 0,
        num = num ?: 0,
        name = safeName,
        normalizedName = StringUtils.normalize(safeName),
        naturalSortName = StringUtils.naturalSort(safeName),
        streamType = streamType ?: "live",
        streamIcon = streamIcon,
        epgChannelId = epgChannelId,
        added = added,
        customSid = customSid,
        tvArchive = tvArchive ?: 0,
        directSource = directSource,
        tvArchiveDuration = tvArchiveDuration ?: 0
    )
}

fun LiveStreamDto.toCategoryRef(profileId: Int): LiveStreamCategoryRefEntity? {
    val safeStreamId = streamId ?: return null
    return LiveStreamCategoryRefEntity(
        profileId = profileId,
        streamId = safeStreamId,
        categoryId = categoryId ?: "0"
    )
}

fun List<LiveCategoryDto>.toEntities(profileId: Int) = mapIndexed { index, item ->
    item.toEntity(profileId, index)
}

fun List<LiveStreamDto>.toStreamEntities(profileId: Int) = map { it.toEntity(profileId) }
