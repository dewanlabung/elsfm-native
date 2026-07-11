package com.elsfm.mobile.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Colors extracted directly from the real elsfm.com PWA's computed CSS custom properties
 * (`--be-*` tokens), inspected live in both its light and dark themes - not a generic
 * Material palette. See `--be-primary`, `--be-bg`, `--be-fg-base`, etc. on elsfm.com.
 */
val ElsfmPrimary = Color(0xFF689F38)
val ElsfmOnPrimary = Color(0xFFFFFFFF)
val ElsfmPrimaryLight = Color(0xFFB4CF9C)

// Light theme (`--be-bg: 255 255 255`, `--be-fg-base: 0 0 0`, `--be-bg-chip: 224 224 224`)
val ElsfmBackgroundLight = Color(0xFFFFFFFF)
val ElsfmSurfaceLight = Color(0xFFFFFFFF)
val ElsfmOnSurfaceLight = Color(0xFF000000)
val ElsfmSurfaceVariantLight = Color(0xFFE0E0E0)
val ElsfmOnSurfaceVariantLight = Color(0xFF666666)

// Dark theme (`--be-bg: 35 35 44`, `--be-fg-base: 255 255 255`, `--be-bg-chip: 53 53 67`)
val ElsfmBackgroundDark = Color(0xFF23232C)
val ElsfmSurfaceDark = Color(0xFF23232C)
val ElsfmOnSurfaceDark = Color(0xFFFFFFFF)
val ElsfmSurfaceVariantDark = Color(0xFF353543)
val ElsfmOnSurfaceVariantDark = Color(0xFFBDBDC0)
