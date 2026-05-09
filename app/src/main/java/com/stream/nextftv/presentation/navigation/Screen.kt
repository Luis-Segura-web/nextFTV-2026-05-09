package com.stream.nextftv.presentation.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login?profileId={profileId}") {
        fun createRoute(profileId: Int? = null) =
            if (profileId != null) "login?profileId=$profileId" else "login?profileId=-1"
    }
    object ProfileSelector : Screen("profile_selector")
    object Home : Screen("home")
    object LiveTv : Screen("live_tv")
    object Movies : Screen("movies")
    object Series : Screen("series")
    object ArtistSearch : Screen("artist_search")
    object Settings : Screen("settings")
    object ParentalControl : Screen("parental_control")
    object Downloads : Screen("downloads")
    
    // Rutas de detalles con parámetros opcionales para auto-reproducción
    object MovieDetail : Screen("movie_detail/{id}?autoPlay={autoPlay}") {
        fun createRoute(id: Int, autoPlay: Boolean = false) =
            "movie_detail/$id?autoPlay=$autoPlay"
    }
    
    object SeriesDetail : Screen("series_detail/{id}?episodeId={episodeId}&autoPlay={autoPlay}&sourceCategoryId={sourceCategoryId}") {
        fun createRoute(id: Int, episodeId: String? = null, autoPlay: Boolean = false, sourceCategoryId: String? = null) =
            "series_detail/$id?episodeId=${Uri.encode(episodeId ?: "")}&autoPlay=$autoPlay&sourceCategoryId=${Uri.encode(sourceCategoryId.orEmpty())}"
    }

    object ActorDetail : Screen("actor_detail/{id}?name={name}&photo={photo}") {
        fun createRoute(id: Int, name: String? = null, photo: String? = null) =
            "actor_detail/$id?name=${Uri.encode(name.orEmpty())}&photo=${Uri.encode(photo.orEmpty())}"
    }
}
