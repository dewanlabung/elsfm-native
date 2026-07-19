package com.elsfm.mobile.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val ElsfmDarkColorScheme = darkColorScheme(
    primary = ElsfmPrimary,
    onPrimary = ElsfmOnPrimary,
    secondary = ElsfmPrimary,
    onSecondary = ElsfmOnPrimary,
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
    secondary = ElsfmPrimary,
    onSecondary = ElsfmOnPrimary,
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
    customPrimaryColor: Color? = null,
    customAccentColor: Color? = null,
    customBackgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val base = if (useDarkTheme) ElsfmDarkColorScheme else ElsfmLightColorScheme
    val colorScheme = base.copy(
        primary = customPrimaryColor ?: base.primary,
        onPrimary = (customPrimaryColor ?: base.primary).contentColor(),
        secondary = customAccentColor ?: base.secondary,
        onSecondary = (customAccentColor ?: base.secondary).contentColor(),
        background = customBackgroundColor ?: base.background,
        onBackground = if (customBackgroundColor != null) customBackgroundColor.contentColor() else base.onBackground,
        surface = customBackgroundColor ?: base.surface,
        onSurface = if (customBackgroundColor != null) customBackgroundColor.contentColor() else base.onSurface,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ElsfmTypography,
        content = content,
    )
}

private fun Color.contentColor(): Color =
    if (luminance() > 0.179f) Color(0xFF000000) else Color(0xFFFFFFFF)
