package com.stream.iptvrevolut.di

import com.stream.iptvrevolut.data.remote.XtreamApiService
import com.stream.iptvrevolut.data.remote.tmdb.TmdbApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

import com.google.gson.GsonBuilder
import com.stream.iptvrevolut.data.remote.utils.SafeBooleanAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeDoubleAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeIntAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeLongAdapter
import com.stream.iptvrevolut.data.remote.utils.SafeStringAdapter
import com.stream.iptvrevolut.data.remote.utils.SeriesEpisodesDeserializer
import com.stream.iptvrevolut.data.remote.SeriesEpisodeDto
import com.stream.iptvrevolut.data.remote.utils.SafeListStringAdapter
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TMDB_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIwYTgyYzZmZjJiNGIxMzBmODNmYWNmNTZhZTlhODliMSIsIm5iZiI6MTc0MDk4Nzg3MS4wOTEsInN1YiI6IjY3YzU1ZGRmNGIxNjM5OTA0YWU3NWQ5ZiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.JPiDvlbC85p9Zjwp-MkEbq33G_Mny2MpGQrxwOlU1hE"

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    @Named("XtreamClient")
    fun provideXtreamOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    @Named("TmdbClient")
    fun provideTmdbOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $TMDB_TOKEN")
                .addHeader("accept", "application/json")
                .build()
            chain.proceed(newRequest)
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson {
        return com.google.gson.Gson()
    }

    @Provides
    @Singleton
    fun provideXtreamApiService(@Named("XtreamClient") okHttpClient: OkHttpClient): XtreamApiService {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
        }
        val contentType = "application/json".toMediaType()

        val gson = GsonBuilder()
            // Registrar tanto primitivos como envoltorios (Integer, Long, etc.)
            .registerTypeAdapter(Int::class.java, SafeIntAdapter())
            .registerTypeAdapter(java.lang.Integer::class.java, SafeIntAdapter())
            .registerTypeAdapter(Long::class.java, SafeLongAdapter())
            .registerTypeAdapter(java.lang.Long::class.java, SafeLongAdapter())
            .registerTypeAdapter(Double::class.java, SafeDoubleAdapter())
            .registerTypeAdapter(java.lang.Double::class.java, SafeDoubleAdapter())
            .registerTypeAdapter(Boolean::class.java, SafeBooleanAdapter())
            .registerTypeAdapter(java.lang.Boolean::class.java, SafeBooleanAdapter())
            .registerTypeAdapter(String::class.java, SafeStringAdapter())
            .registerTypeAdapter(
                object : com.google.gson.reflect.TypeToken<List<String>>() {}.type,
                SafeListStringAdapter()
            )
            .registerTypeAdapter(
                object : com.google.gson.reflect.TypeToken<Map<String, List<SeriesEpisodeDto>>>() {}.type,
                SeriesEpisodesDeserializer()
            )
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl("http://localhost/") 
            .addConverterFactory(json.asConverterFactory(contentType))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
            .create(XtreamApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTmdbApiService(@Named("TmdbClient") okHttpClient: OkHttpClient): TmdbApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(TmdbApiService::class.java)
    }
}
