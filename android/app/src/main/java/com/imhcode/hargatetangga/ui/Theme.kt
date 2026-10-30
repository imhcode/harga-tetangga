package com.imhcode.hargatetangga.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Leaf green ink with a shelf-label yellow.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0E6B53),
    onPrimary = Color.White,
    secondary = Color(0xFFFFD84A),
    onSecondary = Color(0xFF2A2200),
    background = Color(0xFFF2F5F1),
    surface = Color(0xFFF2F5F1),
    surfaceVariant = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5FD1A3),
    onPrimary = Color(0xFF06241A),
    secondary = Color(0xFFE8C23A),
    onSecondary = Color(0xFF241C00),
    background = Color(0xFF0D1512),
    surface = Color(0xFF0D1512),
    surfaceVariant = Color(0xFF15201B),
)

@Composable
fun HargaTetanggaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
