package com.elsfm.mobile.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ElsfmDarkColorScheme = darkColorScheme(
    primary = ElsfmPrimary,
    onPrimary = ElsfmOnPrimary,
    background = ElsfmBackgroundDark,
    onBackground = ElsfmOnSurfaceDark,
    surface = ElsfmSurfaceDark,
    onSurface = ElsfmOnSurfaceDark,
    surfaceVariant = ElsfmSurfaceVariantDark,
    onSurfaceVariant = ElsfmOnSurfaceVariantDark,
)

private val ElsfmLightColorScheme = lightColorScheme(
    primary = ElsfmPrimary,
    onPrimary = ElsfmOnPrimary,
    background = ElsfmBackgroundLight,
    onBackground = ElsfmOnSurfaceLight,
    surface = ElsfmSurfaceLight,
    onSurface = ElsfmOnSurfaceLight,
    surfaceVariant = ElsfmSurfaceVariantLight,
    onSurfaceVariant = ElsfmOnSurfaceVariantLight,
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
