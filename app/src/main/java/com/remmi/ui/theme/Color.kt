package com.remmi.ui.theme

import androidx.compose.ui.graphics.Color

// Light Matte Canvas Palette
val LightBackground = Color(0xFFF5F5F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceSubtle = Color(0xFFEEEEEC)
val LightTextPrimary = Color(0xFF151515)
val LightTextSecondary = Color(0xFF666666)
val LightTextMuted = Color(0xFF999999)
val LightDivider = Color(0xFFE2E2DF)

// Dark Matte Canvas Palette
val DarkBackground = Color(0xFF111111)
val DarkSurface = Color(0xFF191919)
val DarkSurfaceElevated = Color(0xFF222222)
val DarkTextPrimary = Color(0xFFF2F2F2)
val DarkTextSecondary = Color(0xFFA6A6A6)
val DarkTextMuted = Color(0xFF707070)
val DarkDivider = Color(0xFF292929)

data class AccentPalette(
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val accentSurface: Color,
    val accentOn: Color
)

fun getAccentPalette(accent: AppAccent, isDark: Boolean): AccentPalette {
    return if (isDark) {
        when (accent) {
            AppAccent.BLUE -> AccentPalette(
                accent = Color(0xFF78A5FF),
                accentStrong = Color(0xFF91B4FF),
                accentSoft = Color(0xFF1B2942),
                accentSurface = Color(0xFF172238),
                accentOn = Color(0xFF111111)
            )
            AppAccent.GREEN -> AccentPalette(
                accent = Color(0xFF81C784),
                accentStrong = Color(0xFFA5D6A7),
                accentSoft = Color(0xFF1B3320),
                accentSurface = Color(0xFF142618),
                accentOn = Color(0xFF111111)
            )
            AppAccent.PURPLE -> AccentPalette(
                accent = Color(0xFFBA68C8),
                accentStrong = Color(0xFFCE93D8),
                accentSoft = Color(0xFF2D1A34),
                accentSurface = Color(0xFF231429),
                accentOn = Color(0xFF111111)
            )
            AppAccent.ORANGE -> AccentPalette(
                accent = Color(0xFFFFB74D),
                accentStrong = Color(0xFFFFCC80),
                accentSoft = Color(0xFF3E2723),
                accentSurface = Color(0xFF33201C),
                accentOn = Color(0xFF111111)
            )
            AppAccent.RED -> AccentPalette(
                accent = Color(0xFFE57373),
                accentStrong = Color(0xFFEF9A9A),
                accentSoft = Color(0xFF3B1C1C),
                accentSurface = Color(0xFF301717),
                accentOn = Color(0xFF111111)
            )
            AppAccent.TEAL -> AccentPalette(
                accent = Color(0xFF4DB6AC),
                accentStrong = Color(0xFF80CBC4),
                accentSoft = Color(0xFF10302C),
                accentSurface = Color(0xFF0D2724),
                accentOn = Color(0xFF111111)
            )
            AppAccent.YELLOW -> AccentPalette(
                accent = Color(0xFFFBC02D),
                accentStrong = Color(0xFFFFF59D),
                accentSoft = Color(0xFF3E3210),
                accentSurface = Color(0xFF33290D),
                accentOn = Color(0xFF111111)
            )
            AppAccent.PINK -> AccentPalette(
                accent = Color(0xFFF06292),
                accentStrong = Color(0xFFF48FB1),
                accentSoft = Color(0xFF3B1724),
                accentSurface = Color(0xFF30121D),
                accentOn = Color(0xFF111111)
            )
        }
    } else {
        when (accent) {
            AppAccent.BLUE -> AccentPalette(
                accent = Color(0xFF3978E8),
                accentStrong = Color(0xFF2563D5),
                accentSoft = Color(0xFFE5EDFF),
                accentSurface = Color(0xFFF0F4FF),
                accentOn = Color(0xFFFFFFFF)
            )
            AppAccent.GREEN -> AccentPalette(
                accent = Color(0xFF2E7D32),
                accentStrong = Color(0xFF1B5E20),
                accentSoft = Color(0xFFE8F5E9),
                accentSurface = Color(0xFFF1F8E9),
                accentOn = Color(0xFFFFFFFF)
            )
            AppAccent.PURPLE -> AccentPalette(
                accent = Color(0xFF7B1FA2),
                accentStrong = Color(0xFF512DA8),
                accentSoft = Color(0xFFF3E5F5),
                accentSurface = Color(0xFFF8F0FB),
                accentOn = Color(0xFFFFFFFF)
            )
            AppAccent.ORANGE -> AccentPalette(
                accent = Color(0xFFE65100),
                accentStrong = Color(0xFFBF360C),
                accentSoft = Color(0xFFFFF3E0),
                accentSurface = Color(0xFFFFF8E1),
                accentOn = Color(0xFFFFFFFF)
            )
            AppAccent.RED -> AccentPalette(
                accent = Color(0xFFC62828),
                accentStrong = Color(0xFFB71C1C),
                accentSoft = Color(0xFFFFEBEE),
                accentSurface = Color(0xFFFFF0F2),
                accentOn = Color(0xFFFFFFFF)
            )
            AppAccent.TEAL -> AccentPalette(
                accent = Color(0xFF00695C),
                accentStrong = Color(0xFF004D40),
                accentSoft = Color(0xFFE0F2F1),
                accentSurface = Color(0xFFE6F7F5),
                accentOn = Color(0xFFFFFFFF)
            )
            AppAccent.YELLOW -> AccentPalette(
                accent = Color(0xFFF57F17),
                accentStrong = Color(0xFFE65100),
                accentSoft = Color(0xFFFFFDE7),
                accentSurface = Color(0xFFFFFDE7),
                accentOn = Color(0xFFFFFFFF)
            )
            AppAccent.PINK -> AccentPalette(
                accent = Color(0xFFC2185B),
                accentStrong = Color(0xFF880E4F),
                accentSoft = Color(0xFFFCE4EC),
                accentSurface = Color(0xFFFDEDF2),
                accentOn = Color(0xFFFFFFFF)
            )
        }
    }
}
