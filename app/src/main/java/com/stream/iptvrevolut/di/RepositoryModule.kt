package com.stream.iptvrevolut.di

import com.stream.iptvrevolut.data.repository.DownloadRepositoryImpl
import com.stream.iptvrevolut.data.repository.LiveTvRepositoryImpl
import com.stream.iptvrevolut.data.repository.ProfileRepositoryImpl
import com.stream.iptvrevolut.data.repository.SeriesRepositoryImpl
import com.stream.iptvrevolut.data.repository.VodRepositoryImpl
import com.stream.iptvrevolut.domain.repository.DownloadRepository
import com.stream.iptvrevolut.domain.repository.LiveTvRepository
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import com.stream.iptvrevolut.domain.repository.SeriesRepository
import com.stream.iptvrevolut.domain.repository.VodRepository
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