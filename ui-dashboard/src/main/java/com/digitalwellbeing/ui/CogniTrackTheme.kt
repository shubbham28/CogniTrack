package com.digitalwellbeing.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Midnight = Color(0xFF09111F)
private val Mist = Color(0xFFF5F7FB)
private val Ember = Color(0xFFFF7A59)
private val Volt = Color(0xFFB6F36A)
private val Ice = Color(0xFF8EC5FF)
private val Fog = Color(0xFF9FAFD1)
private val Glass = Color(0x26FFFFFF)
private val GlassDark = Color(0x1F0F1B2E)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Ember,
    onPrimary = Mist,
    secondary = Volt,
    tertiary = Ice,
    background = Color(0xFFEAF0F8),
    onBackground = Midnight,
    surface = Glass,
    onSurface = Midnight,
    surfaceVariant = Color(0x66FFFFFF),
    onSurfaceVariant = Midnight
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Ember,
    onPrimary = Mist,
    secondary = Volt,
    tertiary = Ice,
    background = Midnight,
    onBackground = Mist,
    surface = GlassDark,
    onSurface = Mist,
    surfaceVariant = Color(0x331A2940),
    onSurfaceVariant = Color(0xFFDDE7FF)
)

private val CogniTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 44.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
)

@Composable
fun CogniTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = CogniTypography,
        content = content
    )
}
