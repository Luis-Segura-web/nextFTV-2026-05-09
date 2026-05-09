package com.stream.nextftv.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun StreamingBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val grainPoints = remember {
        List(160) { index ->
            val normalizedX = ((index * 37) % 1000) / 1000f
            val normalizedY = ((index * 91) % 1000) / 1000f
            Offset(normalizedX, normalizedY)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF060B16),
                            Color(0xFF0B1120),
                            Color(0xFF111827)
                        )
                    } else {
                        listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFF1F5FB),
                            Color(0xFFE7EEF7)
                        )
                    }
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF3B82F6).copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFFA7C7FF).copy(alpha = 0.32f),
                                Color.Transparent
                            )
                        },
                        center = Offset(1100f, 180f),
                        radius = 900f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFFD9E6FF).copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        },
                        center = Offset(220f, 1250f),
                        radius = 760f
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF040814).copy(alpha = 0.84f),
                                Color.Transparent,
                                Color(0xFF040814).copy(alpha = 0.74f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.Transparent,
                                Color(0xFFE5ECF6).copy(alpha = 0.3f)
                            )
                        }
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = 0.018f)
                            } else {
                                Color.White.copy(alpha = 0.22f)
                            },
                            Color.Transparent
                        ),
                        center = Offset(960f, 540f),
                        radius = 520f
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            grainPoints.forEach { point ->
                drawCircle(
                    color = if (isDark) {
                        Color.White.copy(alpha = 0.016f)
                    } else {
                        Color(0xFF94A3B8).copy(alpha = 0.02f)
                    },
                    radius = if (isDark) 1.1f else 1.3f,
                    center = Offset(size.width * point.x, size.height * point.y),
                    blendMode = BlendMode.Softlight
                )
            }
        }
    }
}
