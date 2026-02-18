package com.stream.iptvrevolut.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val iconSmall: Dp = 16.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val iconExtraLarge: Dp = 48.dp,
    
    val buttonHeight: Dp = 56.dp,
    val inputHeight: Dp = 64.dp,
    
    val splashLogoSize: Dp = 120.dp,
    val splashLoaderSize: Dp = 32.dp,
    
    val profileCardIconSize: Dp = 32.dp,
    val profileCardPadding: Dp = 16.dp,
    
    val moduleCardHeight: Dp = 120.dp,
    val moduleCardIconSize: Dp = 40.dp,
    val bottomBarHeight: Dp = 56.dp,
    
    val channelLogoSize: Dp = 50.dp,
    val channelItemPadding: Dp = 12.dp,
    val categoryChipHeight: Dp = 40.dp,
    
    val moviePosterCorner: Dp = 12.dp,
    val movieGridSpacing: Dp = 12.dp,
    val movieFavoriteIconSize: Dp = 18.dp,
    val movieFavoriteBadgeSize: Dp = 32.dp,
    
    val castItemWidth: Dp = 80.dp,
    val castImageSize: Dp = 80.dp,
    val detailsHeroHeight: Dp = 250.dp, // Estimado por aspectRatio 16:9
    
    val episodeThumbnailWidth: Dp = 80.dp,
    
    val downloadProgressBarHeight: Dp = 8.dp,
    val downloadActionIconSize: Dp = 32.dp,
    
    val cardElevation: Dp = 4.dp,
    val modalElevation: Dp = 8.dp
)

val LocalDimensions = compositionLocalOf { Dimensions() }

val androidx.compose.material3.MaterialTheme.dimens: Dimensions
    @Composable
    @ReadOnlyComposable
    get() = LocalDimensions.current
