package com.mathapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary       = Blue40,
    onPrimary     = Color.White,
    secondary     = Orange40,
    onSecondary   = Color.White,
    background    = Surface,
    onBackground  = OnSurface,
    surface       = Surface,
    onSurface     = OnSurface
)

@Composable
fun MathAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
