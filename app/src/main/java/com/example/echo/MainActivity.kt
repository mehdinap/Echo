package com.example.echo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.core.util.applyLocale
import com.example.echo.core.util.updateLocale
import com.example.echo.data.local.prefs.SettingsStore
import com.example.echo.data.local.prefs.dataStore
import com.example.echo.ui.EchoAppRoot
import com.example.echo.ui.settings.AppStateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appStateViewModel: AppStateViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val langCode = runCatching {
            runBlocking(Dispatchers.IO) {
                newBase.dataStore.data.first()[SettingsStore.Keys.LANGUAGE] ?: "fa"
            }
        }.getOrElse { "fa" }

        val updatedContext = newBase.updateLocale(langCode)
        super.attachBaseContext(updatedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var ready = false
        splash.setKeepOnScreenCondition { !ready }

        setContent {
            val settings by appStateViewModel.settings.collectAsStateWithLifecycle()
            val loggedIn by appStateViewModel.isLoggedIn.collectAsStateWithLifecycle()
            ready = true

            // Keep the Activity context intact for Hilt while refreshing its resources before
            // the child tree reads stringResource(). No key/recreation is used, so navigation
            // keeps its current back stack when the language changes.
            remember(settings.language) {
                this@MainActivity.applyLocale(settings.language)
            }
            val layoutDirection = if (settings.language == "fa") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
            ) {
                EchoTheme(themeMode = settings.themeMode, fontScale = settings.fontScale) {
                    EchoAppRoot(isLoggedIn = loggedIn)
                }
            }
        }
    }
}
