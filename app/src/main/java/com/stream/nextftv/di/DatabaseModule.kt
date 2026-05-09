package com.stream.nextftv.di

import android.content.Context
import androidx.room.Room
import com.stream.nextftv.data.local.NextFTVDatabase
import com.stream.nextftv.data.local.dao.*
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
    fun provideDatabase(@ApplicationContext context: Context): NextFTVDatabase {
        return Room.databaseBuilder(
            context,
            NextFTVDatabase::class.java,
            "iptv_revolut_db"
        )
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .fallbackToDestructiveMigration(true)
        .build()
    }

    @Provides
    @Singleton
    fun provideProfileDao(db: NextFTVDatabase): ProfileDao {
        return db.profileDao()
    }

    @Provides
    @Singleton
    fun provideLiveTvDao(db: NextFTVDatabase): LiveTvDao {
        return db.liveTvDao()
    }

    @Provides
    @Singleton
    fun provideVodDao(db: NextFTVDatabase): VodDao {
        return db.vodDao()
    }
    
    @Provides
    @Singleton
    fun provideSeriesDao(db: NextFTVDatabase): SeriesDao {
        return db.seriesDao()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(db: NextFTVDatabase): DownloadDao {
        return db.downloadDao()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(db: NextFTVDatabase): FavoriteDao {
        return db.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideRecentDao(db: NextFTVDatabase): RecentDao {
        return db.recentDao()
    }

    @Provides
    @Singleton
    fun provideDetailCacheDao(db: NextFTVDatabase): DetailCacheDao {
        return db.detailCacheDao()
    }

    @Provides
    @Singleton
    fun provideSyncStatusDao(db: NextFTVDatabase): SyncStatusDao {
        return db.syncStatusDao()
    }

    @Provides
    @Singleton
    fun provideParentalControlDao(db: NextFTVDatabase): ParentalControlDao {
        return db.parentalControlDao()
    }
}
