package com.example.echo.core.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Context.updateLocale(languageCode: String): Context {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)

    return createConfigurationContext(config)
}

/** Applies a language change to an existing Activity without replacing its Context identity. */
fun Context.applyLocale(languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    @Suppress("DEPRECATION")
    resources.updateConfiguration(config, resources.displayMetrics)
}
