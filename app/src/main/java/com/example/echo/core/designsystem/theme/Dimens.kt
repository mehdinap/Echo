package com.example.echo.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale (4dp base). Padding and margin values are pulled from here, never typed inline.
 * Usage: `Modifier.padding(EchoTheme.spacing.md)`
 */
data class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val screen: Dp = 20.dp,
    val gutter: Dp = 14.dp,
)

/** Fixed component sizes that recur across screens. */
data class Sizes(
    val icon: Dp = 20.dp,
    val iconLarge: Dp = 32.dp,
    val avatar: Dp = 40.dp,
    val avatarLarge: Dp = 96.dp,
    val artThumb: Dp = 56.dp,
    val artCard: Dp = 148.dp,
    val carouselHeight: Dp = 238.dp,
    val miniPlayerHeight: Dp = 64.dp,
    val bottomBarHeight: Dp = 76.dp,
    val touchTarget: Dp = 48.dp,
    val playButton: Dp = 68.dp,
    val strokeThin: Dp = 1.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalSizes = staticCompositionLocalOf { Sizes() }
