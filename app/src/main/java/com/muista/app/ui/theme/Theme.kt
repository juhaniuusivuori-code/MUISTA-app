package com.muista.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MuistaYellow = Color(0xFFFFD600)
val MuistaOrange = Color(0xFFFFA000)
val MuistaBlue = Color(0xFF0D2B52)
val MuistaRed = Color(0xFFD50000)
val MuistaGreen = Color(0xFF1B8E3E)
val MuistaBackground = Color(0xFFFFFBEA)
val MuistaSurface = Color(0xFFFFFFFF)

private val MuistaColorScheme = lightColorScheme(
    primary = MuistaBlue,
    onPrimary = Color.White,
    secondary = MuistaRed,
    onSecondary = Color.White,
    tertiary = MuistaYellow,
    onTertiary = MuistaBlue,
    background = MuistaBackground,
    onBackground = MuistaBlue,
    surface = MuistaSurface,
    onSurface = MuistaBlue,
    surfaceVariant = Color(0xFFFFF3C4),
    onSurfaceVariant = MuistaBlue
)

@Composable
fun MuistaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuistaColorScheme,
        content = content
    )
}
