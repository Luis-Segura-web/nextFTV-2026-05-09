package com.stream.iptvrevolut.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object ProfileSelector : Screen("profile_selector")
    object Home : Screen("home")
    object LiveTv : Screen("live_tv")
    object Movies : Screen("movies")
    object Series : Screen("series")
    object Settings : Screen("settings")
    object ParentalControl : Screen("parental_control")
    object Downloads : Screen("downloads")
    
    // Rutas de detalles con parámetros opcionales para auto-reproducción
    object MovieDetail : Screen("movie_detail/{id}?autoPlay={autoPlay}") {
        fun createRoute(id: Int, autoPlay: Boolean = false) = 
            "movie_detail/$id?autoPlay=$autoPlay"
    }
    
    object SeriesDetail : Screen("series_detail/{id}?episodeId={episodeId}&autoPlay={autoPlay}") {
        fun createRoute(id: Int, episodeId: String? = null, autoPlay: Boolean = false) = 
            "series_detail/$id?episodeId=${episodeId ?: ""}&autoPlay=$autoPlay"
    }
}
