package com.stream.nextftv.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.stream.nextftv.presentation.player.PlayerFullscreenState
import com.stream.nextftv.utils.findActivity

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,
    onPrimary = Black,
    background = DarkGrey900,
    onBackground = White,
    surface = GreyCard,
    onSurface = White,
    surfaceVariant = GreySurfaceVariant,
    error = ErrorRedLight
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = White,
    background = GreyLight,
    onBackground = DarkGrey800,
    surface = White,
    onSurface = DarkGrey800,
    surfaceVariant = GreyLight,
    error = ErrorRed
)

@Composable
@Suppress("DEPRECATION")
fun NextFTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, view)

            if (PlayerFullscreenState.isFullscreen) {
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
                return@SideEffect
            }

            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalDimensions provides Dimensions()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
