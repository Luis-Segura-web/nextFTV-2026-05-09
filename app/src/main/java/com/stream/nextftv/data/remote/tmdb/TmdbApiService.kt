package com.stream.nextftv.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponseDto

    @GET("search/person")
    suspend fun searchPerson(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX"
    ): TmdbPersonSearchResponseDto

    @GET("discover/movie")
    suspend fun discoverMoviesByActor(
        @Query("with_cast") personId: Int,
        @Query("language") language: String = "es-MX",
        @Query("sort_by") sortBy: String = "popularity.desc"
    ): TmdbMovieResponseDto

    @GET("person/{person_id}")
    suspend fun getPersonDetails(
        @Path("person_id") personId: Int,
        @Query("language") language: String = "es-MX",
        @Query("append_to_response") append: String = "combined_credits"
    ): TmdbPersonDetailsDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "es-MX",
        @Query("append_to_response") append: String = "credits,recommendations,similar"
    ): TmdbMovieDetailsDto

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponseDto

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "es-MX",
        @Query("append_to_response") append: String = "credits,recommendations,similar"
    ): TmdbMovieDetailsDto

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getTvSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "es-MX"
    ): TmdbSeasonDetailsDto

    @GET("collection/{collection_id}")
    suspend fun getCollection(
        @Path("collection_id") collectionId: Int,
        @Query("language") language: String = "es-MX"
    ): TmdbCollectionDto
}

data class TmdbSearchResponseDto(
    val results: List<TmdbSearchResultDto>
)

data class TmdbPersonSearchResponseDto(
    val results: List<TmdbPersonSearchResultDto>
)

data class TmdbSearchResultDto(
    val id: Int,
    val title: String?,
    @com.google.gson.annotations.SerializedName("name") val tvName: String?
)

data class TmdbPersonSearchResultDto(
    val id: Int,
    val name: String?,
    @com.google.gson.annotations.SerializedName("profile_path") val profilePath: String? = null,
    val popularity: Double? = null
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

data class TmdbPersonDetailsDto(
    val id: Int,
    val name: String,
    @com.google.gson.annotations.SerializedName("profile_path") val profilePath: String?,
    val biography: String? = null,
    @com.google.gson.annotations.SerializedName("also_known_as") val alsoKnownAs: List<String>? = null,
    @com.google.gson.annotations.SerializedName("combined_credits") val combinedCredits: TmdbCombinedCreditsDto? = null
)

data class TmdbCombinedCreditsDto(
    val cast: List<TmdbCombinedCreditDto>? = null
)

data class TmdbCombinedCreditDto(
    val id: Int,
    @com.google.gson.annotations.SerializedName("media_type") val mediaType: String?,
    val title: String? = null,
    @com.google.gson.annotations.SerializedName("name") val tvName: String? = null,
    @com.google.gson.annotations.SerializedName("poster_path") val posterPath: String? = null,
    @com.google.gson.annotations.SerializedName("release_date") val releaseDate: String? = null,
    @com.google.gson.annotations.SerializedName("first_air_date") val firstAirDate: String? = null,
    val character: String? = null,
    val overview: String? = null
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
    @com.google.gson.annotations.SerializedName("poster_path") val posterPath: String?,
    @com.google.gson.annotations.SerializedName("release_date") val releaseDate: String? = null
)

data class TmdbCreditsDto(
    val cast: List<TmdbCastDto>
)

data class TmdbCastDto(
    val id: Int,
    val name: String,
    val character: String,
    @com.google.gson.annotations.SerializedName("profile_path") val profilePath: String?
)

data class TmdbSeasonDetailsDto(
    val id: Int? = null,
    @com.google.gson.annotations.SerializedName("season_number") val seasonNumber: Int? = null,
    val episodes: List<TmdbEpisodeDto>? = null
)

data class TmdbEpisodeDto(
    val id: Int? = null,
    val name: String? = null,
    @com.google.gson.annotations.SerializedName("episode_number") val episodeNumber: Int? = null,
    @com.google.gson.annotations.SerializedName("season_number") val seasonNumber: Int? = null
)
