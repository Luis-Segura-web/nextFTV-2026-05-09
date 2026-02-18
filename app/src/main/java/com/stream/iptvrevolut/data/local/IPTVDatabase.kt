package com.stream.iptvrevolut.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stream.iptvrevolut.data.local.dao.*
import com.stream.iptvrevolut.data.local.entity.*
import com.stream.iptvrevolut.data.local.entity.download.DownloadEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveCategoryEntity
import com.stream.iptvrevolut.data.local.entity.live.LiveStreamEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesCategoryEntity
import com.stream.iptvrevolut.data.local.entity.series.SeriesStreamEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodCategoryEntity
import com.stream.iptvrevolut.data.local.entity.vod.VodStreamEntity
import com.stream.iptvrevolut.data.utils.Converters

import com.stream.iptvrevolut.data.local.entity.cache.DetailCacheEntity

@Database(
    entities = [
        ProfileEntity::class,
        LiveStreamEntity::class,
        LiveCategoryEntity::class,
        VodStreamEntity::class,
        VodCategoryEntity::class,
        SeriesStreamEntity::class,
        SeriesCategoryEntity::class,
        FavoriteEntity::class,
        RecentEntity::class,
        DownloadEntity::class,
        DetailCacheEntity::class
    ],
    version = 16, // Incremento por retryCount en Downloads
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class IPTVDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun liveTvDao(): LiveTvDao
    abstract fun vodDao(): VodDao
    abstract fun seriesDao(): SeriesDao
    abstract fun downloadDao(): DownloadDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun detailCacheDao(): DetailCacheDao
}