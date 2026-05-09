package com.stream.nextftv.data.remote

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import com.stream.nextftv.data.remote.utils.SafeListStringAdapter
import com.stream.nextftv.data.remote.dto.LoginResponseDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface XtreamApiService {
    @GET
    suspend fun loginWithUrl(
        @Url url: String,
        @Query("action") action: String = "login",
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<LoginResponseDto>

    @GET
    suspend fun getLiveCategories(
        @Url url: String,
        @Query("action") action: String = "get_live_categories",
        @Query("username") username: String,
        @Query("password") password: String
    ): List<LiveCategoryDto>

    @GET
    suspend fun getLiveStreams(
        @Url url: String,
        @Query("action") action: String = "get_live_streams",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null
    ): List<LiveStreamDto>

    @Streaming
    @GET
    suspend fun getLiveStreamsRaw(
        @Url url: String,
        @Query("action") action: String = "get_live_streams",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null
    ): ResponseBody

    @GET
    suspend fun getVodCategories(
        @Url url: String,
        @Query("action") action: String = "get_vod_categories",
        @Query("username") username: String,
        @Query("password") password: String
    ): List<VodCategoryDto>

    @GET
    suspend fun getVodStreams(
        @Url url: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null
    ): List<VodStreamDto>

    @Streaming
    @GET
    suspend fun getVodStreamsRaw(
        @Url url: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null
    ): ResponseBody

    @GET
    suspend fun getVodInfo(
        @Url url: String,
        @Query("action") action: String = "get_vod_info",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("vod_id") vodId: Int
    ): VodInfoDto

    @GET
    suspend fun getSeriesCategories(
        @Url url: String,
        @Query("action") action: String = "get_series_categories",
        @Query("username") username: String,
        @Query("password") password: String
    ): List<SeriesCategoryDto>

    @GET
    suspend fun getSeries(
        @Url url: String,
        @Query("action") action: String = "get_series",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null
    ): List<SeriesStreamDto>

    @Streaming
    @GET
    suspend fun getSeriesRaw(
        @Url url: String,
        @Query("action") action: String = "get_series",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String? = null
    ): ResponseBody

    @GET
    suspend fun getSeriesInfo(
        @Url url: String,
        @Query("action") action: String = "get_series_info",
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("series_id") seriesId: Int
    ): SeriesInfoDto
}

@Serializable
data class SeriesCategoryDto(
    @SerializedName("category_id") @SerialName("category_id") val categoryId: String,
    @SerializedName("category_name") @SerialName("category_name") val categoryName: String,
    @SerializedName("parent_id") @SerialName("parent_id") val parentId: Int?
)

@Serializable
data class SeriesStreamDto(
    val num: Int?,
    val name: String?,
    @SerializedName("series_id") @SerialName("series_id") val seriesId: Int?,
    val cover: String?,
    val plot: String?,
    @SerializedName("cast") @SerialName("cast") val cast: String?,
    val director: String?,
    val genre: String?,
    @SerializedName("releaseDate") @SerialName("releaseDate") val releaseDate: String?,
    @SerializedName("last_modified") @SerialName("last_modified") val lastModified: String?,
    val rating: String?,
    @SerializedName("rating_5based") @SerialName("rating_5based") val rating5Based: Double?,
    @JsonAdapter(SafeListStringAdapter::class)
    @SerializedName("backdrop_path") @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerializedName("youtube_trailer") @SerialName("youtube_trailer") val youtubeTrailer: String?,
    @SerializedName("episode_run_time") @SerialName("episode_run_time") val episodeRunTime: String?,
    @SerializedName("category_id") @SerialName("category_id") val categoryId: String?,
    @SerializedName(value = "tmdb", alternate = ["tmdb_id", "tmdbid", "idtmdb"]) 
    @SerialName("tmdb") 
    val tmdbId: Int? = null
)

@Serializable
data class SeriesInfoDto(
    val info: SeriesInfoDetailDto? = null,
    val episodes: Map<String, List<SeriesEpisodeDto>>? = null
)

@Serializable
data class SeriesInfoDetailDto(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerializedName("releaseDate") @SerialName("releaseDate") val releaseDate: String? = null,
    val rating: String? = null,
    @JsonAdapter(SafeListStringAdapter::class)
    @SerializedName("backdrop_path") @SerialName("backdrop_path") val backdropPath: List<String>? = null,
    @SerializedName(value = "tmdb", alternate = ["tmdb_id", "tmdbid", "idtmdb"]) @SerialName("tmdb") val tmdbId: Int? = null
)

@Serializable
data class SeriesEpisodeDto(
    val id: String? = null,
    @SerializedName("episode_num") @SerialName("episode_num") val episodeNum: Int? = null,
    val title: String? = null,
    @SerializedName("container_extension") @SerialName("container_extension") val containerExtension: String? = null,
    val info: SeriesEpisodeInfoDto? = null,
    val custom_sid: String? = null,
    val added: String? = null,
    val season: Int? = null,
    @SerializedName("direct_source") @SerialName("direct_source") val direct_source: String? = null
)

@Serializable
data class SeriesEpisodeInfoDto(
    @SerializedName("movie_image") @SerialName("movie_image") val movieImage: String? = null,
    val plot: String? = null,
    val releasedate: String? = null,
    val rating: String? = null,
    val duration: String? = null
)

@Serializable
data class LiveCategoryDto(
    @SerializedName("category_id") @SerialName("category_id") val categoryId: String,
    @SerializedName("category_name") @SerialName("category_name") val categoryName: String,
    @SerializedName("parent_id") @SerialName("parent_id") val parentId: Int? = null
)

@Serializable
data class LiveStreamDto(
    val num: Int? = null,
    val name: String? = null,
    @SerializedName("stream_type") @SerialName("stream_type") val streamType: String? = null,
    @SerializedName("stream_id") @SerialName("stream_id") val streamId: Int? = null,
    @SerializedName("stream_icon") @SerialName("stream_icon") val streamIcon: String? = null,
    @SerializedName("epg_channel_id") @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerializedName("added") @SerialName("added") val added: String? = null,
    @SerializedName("category_id") @SerialName("category_id") val categoryId: String? = null,
    @SerializedName("custom_sid") @SerialName("custom_sid") val customSid: String? = null,
    @SerializedName("tv_archive") @SerialName("tv_archive") val tvArchive: Int? = null,
    @SerializedName("direct_source") @SerialName("direct_source") val directSource: String? = null,
    @SerializedName("tv_archive_duration") @SerialName("tv_archive_duration") val tvArchiveDuration: Int? = null
)

@Serializable
data class VodCategoryDto(
    @SerializedName("category_id") @SerialName("category_id") val categoryId: String,
    @SerializedName("category_name") @SerialName("category_name") val categoryName: String,
    @SerializedName("parent_id") @SerialName("parent_id") val parentId: Int? = null
)

@Serializable
data class VodStreamDto(
    val num: Int? = null,
    val name: String? = null,
    @SerializedName("stream_type") @SerialName("stream_type") val streamType: String? = null,
    @SerializedName("stream_id") @SerialName("stream_id") val streamId: Int? = null,
    @SerializedName("stream_icon") @SerialName("stream_icon") val streamIcon: String? = null,
    val rating: String? = null,
    @SerializedName("rating_5based") @SerialName("rating_5based") val rating5Based: Double? = null,
    val added: String? = null,
    @SerializedName("category_id") @SerialName("category_id") val categoryId: String? = null,
    @SerializedName("container_extension") @SerialName("container_extension") val containerExtension: String? = null,
    @SerializedName("custom_sid") @SerialName("custom_sid") val customSid: String? = null,
    @SerializedName("direct_source") @SerialName("direct_source") val directSource: String? = null,
    @SerializedName(value = "tmdb", alternate = ["tmdb_id", "tmdbid", "idtmdb"]) @SerialName("tmdb") val tmdbId: Int? = null,
    @JsonAdapter(SafeListStringAdapter::class)
    @SerializedName("backdrop_path") @SerialName("backdrop_path") val backdropPath: List<String>? = null
)

@Serializable
data class VodInfoDto(
    val info: VodInfoDetailDto? = null,
    @SerializedName("movie_data") @SerialName("movie_data") val movieData: VodMovieDataDto? = null
)

@Serializable
data class VodInfoDetailDto(
    @SerializedName("movie_image") @SerialName("movie_image") val movieImage: String? = null,
    val genre: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    @SerializedName("release_date") @SerialName("release_date") val releaseDate: String? = null,
    @SerializedName("rating_tmdb") @SerialName("rating_tmdb") val ratingTmdb: String? = null,
    val runtime: String? = null,
    val youtube_trailer: String? = null,
    @SerializedName(value = "tmdb", alternate = ["tmdb_id", "tmdbid", "idtmdb"]) @SerialName("tmdb") val tmdbId: Int? = null
)

@Serializable
data class VodMovieDataDto(
    @SerializedName("stream_id") @SerialName("stream_id") val streamId: Int? = null,
    val name: String? = null,
    @SerializedName("container_extension") @SerialName("container_extension") val containerExtension: String? = null
)
