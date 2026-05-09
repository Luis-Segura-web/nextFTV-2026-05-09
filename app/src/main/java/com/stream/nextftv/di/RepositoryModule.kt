package com.stream.nextftv.di

import com.stream.nextftv.data.repository.DownloadRepositoryImpl
import com.stream.nextftv.data.repository.LiveTvRepositoryImpl
import com.stream.nextftv.data.repository.ProfileRepositoryImpl
import com.stream.nextftv.data.repository.SeriesRepositoryImpl
import com.stream.nextftv.data.repository.VodRepositoryImpl
import com.stream.nextftv.domain.repository.DownloadRepository
import com.stream.nextftv.domain.repository.LiveTvRepository
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.domain.repository.SeriesRepository
import com.stream.nextftv.domain.repository.VodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindLiveTvRepository(
        liveTvRepositoryImpl: LiveTvRepositoryImpl
    ): LiveTvRepository

    @Binds
    @Singleton
    abstract fun bindVodRepository(
        vodRepositoryImpl: VodRepositoryImpl
    ): VodRepository

    @Binds
    @Singleton
    abstract fun bindSeriesRepository(
        seriesRepositoryImpl: SeriesRepositoryImpl
    ): SeriesRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository
}