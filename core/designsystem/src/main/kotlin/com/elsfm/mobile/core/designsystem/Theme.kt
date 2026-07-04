package com.elsfm.mobile.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ElsfmDarkColorScheme = darkColorScheme(
    primary = ElsfmAccent,
    background = ElsfmBackgroundDark,
    surface = ElsfmSurfaceDark,
    onSurface = ElsfmOnSurfaceDark,
)

private val ElsfmLightColorScheme = lightColorScheme(
    primary = ElsfmAccent,
)

@Composable
fun ElsfmTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) ElsfmDarkColorScheme else ElsfmLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ElsfmTypography,
        content = content,
    )
}
