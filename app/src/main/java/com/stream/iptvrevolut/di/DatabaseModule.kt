package com.stream.iptvrevolut.di

import android.content.Context
import androidx.room.Room
import com.stream.iptvrevolut.data.local.IPTVDatabase
import com.stream.iptvrevolut.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IPTVDatabase {
        return Room.databaseBuilder(
            context,
            IPTVDatabase::class.java,
            "iptv_revolut_db"
        )
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .fallbackToDestructiveMigration(true)
        .build()
    }

    @Provides
    @Singleton
    fun provideProfileDao(db: IPTVDatabase): ProfileDao {
        return db.profileDao()
    }

    @Provides
    @Singleton
    fun provideLiveTvDao(db: IPTVDatabase): LiveTvDao {
        return db.liveTvDao()
    }

    @Provides
    @Singleton
    fun provideVodDao(db: IPTVDatabase): VodDao {
        return db.vodDao()
    }
    
    @Provides
    @Singleton
    fun provideSeriesDao(db: IPTVDatabase): SeriesDao {
        return db.seriesDao()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(db: IPTVDatabase): DownloadDao {
        return db.downloadDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(db: IPTVDatabase): FavoriteDao {
        return db.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideRecentDao(db: IPTVDatabase): RecentDao {
        return db.recentDao()
    }

    @Provides
    @Singleton
    fun provideDetailCacheDao(db: IPTVDatabase): DetailCacheDao {
        return db.detailCacheDao()
    }

    @Provides
    @Singleton
    fun provideSyncStatusDao(db: IPTVDatabase): SyncStatusDao {
        return db.syncStatusDao()
    }
}
