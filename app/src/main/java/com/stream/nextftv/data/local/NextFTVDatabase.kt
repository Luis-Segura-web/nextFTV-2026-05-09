package com.stream.nextftv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stream.nextftv.data.local.dao.*
import com.stream.nextftv.data.local.entity.*
import com.stream.nextftv.data.local.entity.download.DownloadEntity
import com.stream.nextftv.data.local.entity.live.LiveCategoryEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamEntity
import com.stream.nextftv.data.local.entity.live.LiveStreamCategoryRefEntity
import com.stream.nextftv.data.local.entity.series.SeriesCategoryEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamEntity
import com.stream.nextftv.data.local.entity.series.SeriesStreamCategoryRefEntity
import com.stream.nextftv.data.local.entity.vod.VodCategoryEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamEntity
import com.stream.nextftv.data.local.entity.vod.VodStreamCategoryRefEntity
import com.stream.nextftv.data.utils.Converters

import com.stream.nextftv.data.local.entity.cache.DetailCacheEntity
import com.stream.nextftv.data.local.entity.SyncStatusEntity
import com.stream.nextftv.data.local.entity.ParentalHiddenCategoryEntity

@Database(
    entities = [
        ProfileEntity::class,
        LiveStreamEntity::class,
        LiveStreamCategoryRefEntity::class,
        LiveCategoryEntity::class,
        VodStreamEntity::class,
        VodStreamCategoryRefEntity::class,
        VodCategoryEntity::class,
        SeriesStreamEntity::class,
        SeriesStreamCategoryRefEntity::class,
        SeriesCategoryEntity::class,
        FavoriteEntity::class,
        RecentEntity::class,
        DownloadEntity::class,
        DetailCacheEntity::class,
        SyncStatusEntity::class,
        ParentalHiddenCategoryEntity::class
    ],
    version = 24,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NextFTVDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun liveTvDao(): LiveTvDao
    abstract fun vodDao(): VodDao
    abstract fun seriesDao(): SeriesDao
    abstract fun downloadDao(): DownloadDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun detailCacheDao(): DetailCacheDao
    abstract fun syncStatusDao(): SyncStatusDao
    abstract fun parentalControlDao(): ParentalControlDao
}
