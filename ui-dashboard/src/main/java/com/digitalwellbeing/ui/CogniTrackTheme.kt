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

private val Ink = Color(0xFF121212)
private val Bone = Color(0xFFF6F0E8)
private val Vermilion = Color(0xFFD64C2F)
private val Lime = Color(0xFF9BC53D)
private val Slate = Color(0xFF2A3441)
private val Smoke = Color(0xFFEEE6DC)

private val LightColors: ColorScheme = lightColorScheme(
    primary = Vermilion,
    onPrimary = Bone,
    secondary = Lime,
    background = Bone,
    onBackground = Ink,
    surface = Smoke,
    onSurface = Ink,
    tertiary = Slate
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Vermilion,
    onPrimary = Bone,
    secondary = Lime,
    background = Ink,
    onBackground = Bone,
    surface = Slate,
    onSurface = Bone,
    tertiary = Smoke
)

private val CogniTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
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
