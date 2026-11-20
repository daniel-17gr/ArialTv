package com.example.arialtv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val NothingYouTubeColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = InkOnWhite,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = PureWhite,
    secondary = SignalRed,
    onSecondary = PureWhite,
    secondaryContainer = SignalRedSoft,
    onSecondaryContainer = PureWhite,
    tertiary = RatingGold,
    onTertiary = InkOnWhite,
    background = CanvasBlack,
    onBackground = InkPrimary,
    surface = SurfaceTonal,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = InkSecondary,
    border = DividerHairline,
    borderVariant = PureWhite,
    error = SignalRed,
    onError = PureWhite,
)

@Composable
fun ArialTvTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NothingYouTubeColorScheme,
        typography = ArialTvTypography,
        content = content,
    )
}
