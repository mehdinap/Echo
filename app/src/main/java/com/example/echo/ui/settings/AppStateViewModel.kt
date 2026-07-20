package com.example.echo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.designsystem.theme.ThemeMode
import com.example.echo.data.local.prefs.AppSettings
import com.example.echo.data.local.prefs.SettingsStore
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Process-wide state: theme, language, font size, and whether anyone is logged in. */
@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    authRepository.refreshMe()
                    chatRepository.connect()   // socket opens once, for the whole session
                } else {
                    chatRepository.disconnect()
                }
            }
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsStore.setThemeMode(mode) }
    fun setLanguage(tag: String) = viewModelScope.launch { settingsStore.setLanguage(tag) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsStore.setFontScale(scale) }
    fun logout() = viewModelScope.launch { authRepository.logout() }
}
