package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SonicFlowColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = CyanMuted,
    onPrimaryContainer = ElectricCyan,
    secondary = ElectricBlue,
    onSecondary = IceWhite,
    secondaryContainer = GraphiteHighlight,
    onSecondaryContainer = CyanGlow,
    tertiary = VerifiedEmerald,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = IceWhite,
    surface = GraphiteSurface,
    onSurface = IceWhite,
    surfaceVariant = GraphiteElevated,
    onSurfaceVariant = SilverMist,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = DeepAbyss,
    surfaceContainer = GraphiteSurface,
    surfaceContainerHigh = GraphiteElevated,
    surfaceContainerHighest = GraphiteHighlight,
    outline = SlateBorder,
    outlineVariant = GraphiteHighlight,
    error = ClipCrimson,
    onError = IceWhite
)

val SonicFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SonicFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SonicFlowColorScheme,
        typography = SonicFlowTypography,
        shapes = SonicFlowShapes,
        content = content
    )
}
