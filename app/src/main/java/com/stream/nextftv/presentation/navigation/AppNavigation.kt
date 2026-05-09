package com.stream.nextftv.presentation.navigation
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.stream.nextftv.presentation.screens.splash.SplashScreen
import com.stream.nextftv.presentation.screens.login.LoginScreen
import com.stream.nextftv.presentation.screens.profileselector.ProfileSelectorScreen
import com.stream.nextftv.presentation.screens.home.HomeScreen
import com.stream.nextftv.presentation.screens.livetv.LiveTvScreen
import com.stream.nextftv.presentation.screens.movies.MoviesScreen
import com.stream.nextftv.presentation.screens.moviedetail.MovieDetailScreen
import com.stream.nextftv.presentation.screens.series.SeriesScreen
import com.stream.nextftv.presentation.screens.seriesdetail.SeriesDetailScreen
import com.stream.nextftv.presentation.screens.artistsearch.ArtistSearchScreen
import com.stream.nextftv.presentation.screens.settings.ParentalControlScreen
import com.stream.nextftv.presentation.screens.settings.SettingsScreen
import com.stream.nextftv.presentation.screens.downloads.DownloadsScreen
import com.stream.nextftv.presentation.screens.actor.ActorDetailScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onNavigate = { route ->
                navController.navigate(route) { popUpTo(Screen.Splash.route) { inclusive = true } }
            })
        }
        
        composable(
            route = Screen.Login.route,
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getInt("profileId")?.takeIf { it >= 0 }
            LoginScreen(
                onLoginSuccess = {
                    if (profileId != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                    }
                },
                onBackToProfiles = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ProfileSelector.route) {
            ProfileSelectorScreen(
                onProfileSelected = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.ProfileSelector.route) { inclusive = true } }
                },
                onAddNewProfile = { navController.navigate(Screen.Login.createRoute()) },
                onEditProfile = { profileId ->
                    navController.navigate(Screen.Login.createRoute(profileId))
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSection = { section ->
                    when(section) {
                        "live" -> navController.navigate(Screen.LiveTv.route)
                        "movies" -> navController.navigate(Screen.Movies.route)
                        "series" -> navController.navigate(Screen.Series.route)
                        "artist_search" -> navController.navigate(Screen.ArtistSearch.route)
                        "downloads" -> navController.navigate(Screen.Downloads.route)
                    }
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.LiveTv.route) {
            LiveTvScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Movies.route) {
            MoviesScreen(
                onBack = { navController.popBackStack() },
                onHome = { }, // Not used anymore as per request to remove button
                onMovieClick = { movieId ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieId))
                }
            )
        }

        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("autoPlay") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("id") ?: 0
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            MovieDetailScreen(
                streamId = movieId,
                autoPlay = autoPlay,
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack(Screen.Home.route, false) },
                onMovieClick = { id ->
                    navController.navigate(Screen.MovieDetail.createRoute(id))
                },
                onActorClick = { actorId, actorName, actorPhoto ->
                    navController.navigate(Screen.ActorDetail.createRoute(actorId, actorName, actorPhoto))
                }
            )
        }

        composable(Screen.Series.route) {
            SeriesScreen(
                onBack = { navController.popBackStack() },
                onHome = { }, // Not used anymore
                onSeriesClick = { seriesId, sourceCategoryId ->
                    navController.navigate(Screen.SeriesDetail.createRoute(seriesId, sourceCategoryId = sourceCategoryId))
                }
            )
        }

        composable(Screen.ArtistSearch.route) {
            ArtistSearchScreen(
                onBack = { navController.popBackStack() },
                onActorClick = { actorId, actorName, actorPhoto ->
                    navController.navigate(Screen.ActorDetail.createRoute(actorId, actorName, actorPhoto))
                }
            )
        }

        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("episodeId") { type = NavType.StringType; nullable = true },
                navArgument("autoPlay") { type = NavType.BoolType; defaultValue = false },
                navArgument("sourceCategoryId") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getInt("id") ?: 0
            val episodeId = backStackEntry.arguments?.getString("episodeId")
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            val sourceCategoryId = backStackEntry.arguments?.getString("sourceCategoryId").orEmpty().ifBlank { null }
            SeriesDetailScreen(
                seriesId = seriesId,
                initialEpisodeId = episodeId,
                autoPlay = autoPlay,
                sourceCategoryId = sourceCategoryId,
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack(Screen.Home.route, false) },
                onSeriesClick = { id, nextSourceCategoryId ->
                    navController.navigate(Screen.SeriesDetail.createRoute(id, sourceCategoryId = nextSourceCategoryId))
                },
                onActorClick = { actorId, actorName, actorPhoto ->
                    navController.navigate(Screen.ActorDetail.createRoute(actorId, actorName, actorPhoto))
                }
            )
        }

        composable(
            route = Screen.ActorDetail.route,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = "" },
                navArgument("photo") { type = NavType.StringType; nullable = true; defaultValue = "" }
            )
        ) { backStackEntry ->
            ActorDetailScreen(
                actorId = backStackEntry.arguments?.getInt("id") ?: 0,
                fallbackName = backStackEntry.arguments?.getString("name").orEmpty().ifBlank { null },
                fallbackPhotoPath = backStackEntry.arguments?.getString("photo").orEmpty().ifBlank { null },
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieId))
                },
                onSeriesClick = { targetSeriesId ->
                    navController.navigate(Screen.SeriesDetail.createRoute(targetSeriesId))
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onBack = { navController.popBackStack() },
                onPlayMovie = { movieId -> 
                    navController.navigate(Screen.MovieDetail.createRoute(movieId, autoPlay = true))
                },
                onPlayEpisode = { seriesId, episodeId ->
                    navController.navigate(Screen.SeriesDetail.createRoute(seriesId, episodeId, autoPlay = true))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenParentalControl = { navController.navigate(Screen.ParentalControl.route) },
                onLogout = {
                    navController.navigate(Screen.ProfileSelector.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.ParentalControl.route) {
            ParentalControlScreen(onBack = { navController.popBackStack() })
        }
    }
}
