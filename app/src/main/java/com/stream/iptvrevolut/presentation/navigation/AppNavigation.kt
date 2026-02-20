package com.stream.iptvrevolut.presentation.navigation
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.stream.iptvrevolut.presentation.screens.splash.SplashScreen
import com.stream.iptvrevolut.presentation.screens.login.LoginScreen
import com.stream.iptvrevolut.presentation.screens.profileselector.ProfileSelectorScreen
import com.stream.iptvrevolut.presentation.screens.home.HomeScreen
import com.stream.iptvrevolut.presentation.screens.livetv.LiveTvScreen
import com.stream.iptvrevolut.presentation.screens.movies.MoviesScreen
import com.stream.iptvrevolut.presentation.screens.moviedetail.MovieDetailScreen
import com.stream.iptvrevolut.presentation.screens.series.SeriesScreen
import com.stream.iptvrevolut.presentation.screens.seriesdetail.SeriesDetailScreen
import com.stream.iptvrevolut.presentation.screens.settings.SettingsScreen
import com.stream.iptvrevolut.presentation.screens.downloads.DownloadsScreen

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
        
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
            })
        }
        
        composable(Screen.ProfileSelector.route) {
            ProfileSelectorScreen(
                onProfileSelected = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.ProfileSelector.route) { inclusive = true } }
                },
                onAddNewProfile = { navController.navigate(Screen.Login.route) }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSection = { section ->
                    when(section) {
                        "live" -> navController.navigate(Screen.LiveTv.route)
                        "movies" -> navController.navigate(Screen.Movies.route)
                        "series" -> navController.navigate(Screen.Series.route)
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
                onMovieClick = { movieId -> navController.navigate(Screen.MovieDetail.createRoute(movieId)) }
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
                }
            )
        }

        composable(Screen.Series.route) {
            SeriesScreen(
                onBack = { navController.popBackStack() },
                onHome = { }, // Not used anymore
                onSeriesClick = { seriesId -> navController.navigate(Screen.SeriesDetail.createRoute(seriesId)) }
            )
        }

        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("episodeId") { type = NavType.StringType; nullable = true },
                navArgument("autoPlay") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getInt("id") ?: 0
            val episodeId = backStackEntry.arguments?.getString("episodeId")
            val autoPlay = backStackEntry.arguments?.getBoolean("autoPlay") ?: false
            SeriesDetailScreen(
                seriesId = seriesId,
                initialEpisodeId = episodeId,
                autoPlay = autoPlay,
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack(Screen.Home.route, false) },
                onSeriesClick = { id ->
                    navController.navigate(Screen.SeriesDetail.createRoute(id))
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
                onLogout = {
                    navController.navigate(Screen.ProfileSelector.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                }
            )
        }
    }
}
