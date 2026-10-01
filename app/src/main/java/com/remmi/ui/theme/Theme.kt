package com.remmi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RemmiColors(
    val background: Color,
    val surface: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val accentSurface: Color,
    val accentOn: Color,
    val isDark: Boolean
)

val LocalRemmiColors = staticCompositionLocalOf {
    RemmiColors(
        background = LightBackground,
        surface = LightSurface,
        surfaceSubtle = LightSurfaceSubtle,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textMuted = LightTextMuted,
        divider = LightDivider,
        accent = Color(0xFF3978E8),
        accentStrong = Color(0xFF2563D5),
        accentSoft = Color(0xFFE5EDFF),
        accentSurface = Color(0xFFF0F4FF),
        accentOn = Color(0xFFFFFFFF),
        isDark = false
    )
}

val MaterialTheme.remmiColors: RemmiColors
    @Composable
    get() = LocalRemmiColors.current

@Composable
fun RemmiTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    appAccent: AppAccent = AppAccent.BLUE,
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val accentPalette = getAccentPalette(appAccent, isDark)

    val remmiColors = if (isDark) {
        RemmiColors(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceSubtle = DarkSurfaceElevated,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textMuted = DarkTextMuted,
            divider = DarkDivider,
            accent = accentPalette.accent,
            accentStrong = accentPalette.accentStrong,
            accentSoft = accentPalette.accentSoft,
            accentSurface = accentPalette.accentSurface,
            accentOn = accentPalette.accentOn,
            isDark = true
        )
    } else {
        RemmiColors(
            background = LightBackground,
            surface = LightSurface,
            surfaceSubtle = LightSurfaceSubtle,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textMuted = LightTextMuted,
            divider = LightDivider,
            accent = accentPalette.accent,
            accentStrong = accentPalette.accentStrong,
            accentSoft = accentPalette.accentSoft,
            accentSurface = accentPalette.accentSurface,
            accentOn = accentPalette.accentOn,
            isDark = false
        )
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = remmiColors.accent,
            onPrimary = remmiColors.accentOn,
            primaryContainer = remmiColors.accentSurface,
            onPrimaryContainer = remmiColors.accent,
            secondary = remmiColors.textSecondary,
            background = remmiColors.background,
            onBackground = remmiColors.textPrimary,
            surface = remmiColors.surface,
            onSurface = remmiColors.textPrimary,
            surfaceVariant = remmiColors.surfaceSubtle,
            onSurfaceVariant = remmiColors.textSecondary,
            outline = remmiColors.divider
        )
    } else {
        lightColorScheme(
            primary = remmiColors.accent,
            onPrimary = remmiColors.accentOn,
            primaryContainer = remmiColors.accentSurface,
            onPrimaryContainer = remmiColors.accent,
            secondary = remmiColors.textSecondary,
            background = remmiColors.background,
            onBackground = remmiColors.textPrimary,
            surface = remmiColors.surface,
            onSurface = remmiColors.textPrimary,
            surfaceVariant = remmiColors.surfaceSubtle,
            onSurfaceVariant = remmiColors.textSecondary,
            outline = remmiColors.divider
        )
    }

    CompositionLocalProvider(LocalRemmiColors provides remmiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
