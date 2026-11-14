package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentColorOption
import com.example.data.model.AppThemeStyle

val SonicFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SonicFlowTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.AMOLED_BLACK,
    accentOption: AccentColorOption = AccentColorOption.ELECTRIC_CYAN,
    content: @Composable () -> Unit
) {
    val primaryColor = when (accentOption) {
        AccentColorOption.ELECTRIC_CYAN -> ElectricCyan
        AccentColorOption.COBALT_BLUE -> ElectricBlue
        AccentColorOption.EMERALD_STUDIO -> VerifiedEmerald
        AccentColorOption.WARM_AMBER -> AmberPeak
    }
    val primaryContainer = when (accentOption) {
        AccentColorOption.ELECTRIC_CYAN -> CyanMuted
        AccentColorOption.COBALT_BLUE -> Color(0xFF102A5C)
        AccentColorOption.EMERALD_STUDIO -> Color(0xFF0A3B24)
        AccentColorOption.WARM_AMBER -> Color(0xFF3E2B07)
    }
    val backgroundColor = when (themeStyle) {
        AppThemeStyle.AMOLED_BLACK -> ObsidianBlack
        AppThemeStyle.GRAPHITE_DARK -> DeepAbyss
    }
    val surfaceColor = when (themeStyle) {
        AppThemeStyle.AMOLED_BLACK -> GraphiteSurface
        AppThemeStyle.GRAPHITE_DARK -> GraphiteElevated
    }

    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = ObsidianBlack,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryColor,
        secondary = ElectricBlue,
        onSecondary = IceWhite,
        secondaryContainer = GraphiteHighlight,
        onSecondaryContainer = CyanGlow,
        tertiary = VerifiedEmerald,
        onTertiary = ObsidianBlack,
        background = backgroundColor,
        onBackground = IceWhite,
        surface = surfaceColor,
        onSurface = IceWhite,
        surfaceVariant = GraphiteElevated,
        onSurfaceVariant = SilverMist,
        surfaceContainerLowest = backgroundColor,
        surfaceContainerLow = DeepAbyss,
        surfaceContainer = surfaceColor,
        surfaceContainerHigh = GraphiteElevated,
        surfaceContainerHighest = GraphiteHighlight,
        outline = SlateBorder,
        outlineVariant = GraphiteHighlight,
        error = ClipCrimson,
        onError = IceWhite
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SonicFlowTypography,
        shapes = SonicFlowShapes,
        content = content
    )
}
