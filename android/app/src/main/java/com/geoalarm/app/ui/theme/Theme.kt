package com.geoalarm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.geoalarm.app.data.repository.ThemeMode

// Primary: modern blue/indigo, matching the website and app icon.
private val Indigo = Color(0xFF3A45D6)
private val IndigoDark = Color(0xFF8089FF)
private val Warn = Color(0xFFC77700)
private val WarnDark = Color(0xFFFFB020)
private val Ok = Color(0xFF0F8A5F)
private val OkDark = Color(0xFF4FD6A0)

val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Ok,
    tertiary = Warn,
    background = Color(0xFFF5F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3E7F8),
    onBackground = Color(0xFF191D45),
    onSurface = Color(0xFF191D45),
    error = Color(0xFFC0362C)
)

val DarkColors = darkColorScheme(
    primary = IndigoDark,
    onPrimary = Color(0xFF0C1030),
    secondary = OkDark,
    tertiary = WarnDark,
    background = Color(0xFF0C1030),
    surface = Color(0xFF151A45),
    surfaceVariant = Color(0xFF232A6B),
    onBackground = Color(0xFFE9EBFC),
    onSurface = Color(0xFFE9EBFC),
    error = Color(0xFFFF6F63)
)

val GeoAlarmTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
)

@Composable
fun GeoAlarmTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GeoAlarmTypography,
        content = content
    )
}
