package com.example.echo.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.echo.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "ava_settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "fa",
    val fontScale: Float = 1f,
    val isPremium: Boolean = false,
)

/** Theme, language, font size and the premium flag. Everything user-tunable lives here. */
@Singleton
class SettingsStore @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val PREMIUM = booleanPreferencesKey("is_premium")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            language = p[Keys.LANGUAGE] ?: "fa",
            fontScale = p[Keys.FONT_SCALE] ?: 1f,
            isPremium = p[Keys.PREMIUM] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setLanguage(tag: String) = context.dataStore.edit { it[Keys.LANGUAGE] = tag }
    suspend fun setFontScale(scale: Float) = context.dataStore.edit { it[Keys.FONT_SCALE] = scale }
    suspend fun setPremium(premium: Boolean) = context.dataStore.edit { it[Keys.PREMIUM] = premium }
}
