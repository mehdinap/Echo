package com.example.echo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** What the user picked in Settings. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColors = lightColorScheme(
    primary = Moss600,
    onPrimary = Paper,
    primaryContainer = Sprout100,
    onPrimaryContainer = Moss900,
    secondary = Moss500,
    onSecondary = Paper,
    secondaryContainer = Sprout050,
    onSecondaryContainer = Moss800,
    tertiary = Moss700,
    onTertiary = Paper,
    background = Bone,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Fog,
    onSurfaceVariant = Slate,
    surfaceContainerHighest = Fog,
    outline = Slate.copy(alpha = 0.45f),
    outlineVariant = Fog,
    error = Ruby,
    onError = Paper,
    scrim = Ink.copy(alpha = 0.6f),
)

private val DarkColors = darkColorScheme(
    primary = Sprout300,
    onPrimary = Moss900,
    primaryContainer = Moss700,
    onPrimaryContainer = Sprout100,
    secondary = Sprout400,
    onSecondary = Moss900,
    secondaryContainer = Moss800,
    onSecondaryContainer = Sprout200,
    tertiary = Sprout200,
    onTertiary = Moss900,
    background = NightBg,
    onBackground = Sprout050,
    surface = NightSurface,
    onSurface = Sprout050,
    surfaceVariant = NightRaised,
    onSurfaceVariant = Mist,
    surfaceContainerHighest = NightRaised,
    outline = NightOutline,
    outlineVariant = NightRaised,
    error = RubyDark,
    onError = Moss900,
    scrim = androidx.compose.ui.graphics.Color(0xCC000000),
)

@Composable
fun EchoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalSizes provides Sizes(),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = EchoTypography.scaled(fontScale),
            shapes = EchoShapes,
            content = content,
        )
    }
}

/** `EchoTheme.spacing.md` reads better at the call site than `LocalSpacing.current.md`. */
object EchoTheme {
    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val sizes: Sizes
        @Composable @ReadOnlyComposable get() = LocalSizes.current
}
