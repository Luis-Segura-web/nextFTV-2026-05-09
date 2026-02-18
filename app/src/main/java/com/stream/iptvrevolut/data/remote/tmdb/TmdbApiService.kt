package com.stream.iptvrevolut.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,
        @Query("language") language: String = "es-ES"
    ): TmdbSearchResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-ES",
        @Query("append_to_response") append: String = "credits,recommendations,similar"
    ): TmdbMovieDetailsDto

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("language") language: String = "es-ES"
    ): TmdbSearchResponseDto

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "es-ES",
        @Query("append_to_response") append: String = "credits,recommendations,similar"
    ): TmdbMovieDetailsDto

    @GET("collection/{collection_id}")
    suspend fun getCollection(
        @Path("collection_id") collectionId: Int,
        @Query("language") language: String = "es-ES"
    ): TmdbCollectionDto
}

data class TmdbSearchResponseDto(
    val results: List<TmdbSearchResultDto>
)

data class TmdbSearchResultDto(
    val id: Int,
    val title: String?,
    @com.google.gson.annotations.SerializedName("name") val tvName: String?
)

data class TmdbMovieDetailsDto(
    val id: Int,
    @com.google.gson.annotations.SerializedName("backdrop_path") val backdropPath: String?,
    @com.google.gson.annotations.SerializedName("poster_path") val posterPath: String?,
    val overview: String?,
    @com.google.gson.annotations.SerializedName("release_date") val releaseDate: String?,
    @com.google.gson.annotations.SerializedName("first_air_date") val firstAirDate: String?,
    @com.google.gson.annotations.SerializedName("vote_average") val voteAverage: Double?,
    val credits: TmdbCreditsDto?,
    val recommendations: TmdbMovieResponseDto?,
    val similar: TmdbMovieResponseDto?,
    @com.google.gson.annotations.SerializedName("belongs_to_collection") val belongsToCollection: TmdbCollectionRefDto?
)

data class TmdbCollectionRefDto(
    val id: Int,
    val name: String?
)

data class TmdbCollectionDto(
    val name: String?,
    val parts: List<TmdbMovieShortDto>?
)

data class TmdbMovieResponseDto(
    val results: List<TmdbMovieShortDto>?
)

data class TmdbMovieShortDto(
    val id: Int,
    val title: String?,
    @com.google.gson.annotations.SerializedName("name") val tvName: String?,
    @com.google.gson.annotations.SerializedName("poster_path") val posterPath: String?
)

data class TmdbCreditsDto(
    val cast: List<TmdbCastDto>
)

data class TmdbCastDto(
    val name: String,
    val character: String,
    @com.google.gson.annotations.SerializedName("profile_path") val profilePath: String?
)
