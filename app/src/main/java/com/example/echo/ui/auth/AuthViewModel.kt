package com.example.echo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.util.Outcome
import com.example.echo.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isRegisterMode: Boolean = false,
    val username: String = "",
    val password: String = "",
    val displayName: String = "",
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val registrationSucceeded: Boolean = false,
) {
    val canSubmit: Boolean
        get() = username.length >= 3 && password.length >= 4 && !isSubmitting
}

sealed interface AuthEvent {
    data class UsernameChanged(val value: String) : AuthEvent
    data class PasswordChanged(val value: String) : AuthEvent
    data class DisplayNameChanged(val value: String) : AuthEvent
    data object ToggleMode : AuthEvent
    data object Submit : AuthEvent
    data object RegistrationMessageShown : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.UsernameChanged -> _uiState.update { it.copy(username = event.value, hasError = false) }
            is AuthEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value, hasError = false) }
            is AuthEvent.DisplayNameChanged -> _uiState.update { it.copy(displayName = event.value) }
            AuthEvent.ToggleMode -> _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, hasError = false) }
            AuthEvent.Submit -> submit()
            AuthEvent.RegistrationMessageShown -> _uiState.update { it.copy(registrationSucceeded = false) }
        }
    }

    private fun submit() = viewModelScope.launch {
        val state = _uiState.value
        if (!state.canSubmit) return@launch
        _uiState.update { it.copy(isSubmitting = true, hasError = false) }

        val result = if (state.isRegisterMode) {
            authRepository.register(
                state.username.trim(),
                state.password,
                state.displayName.ifBlank { state.username }.trim(),
            )
        } else {
            authRepository.login(state.username.trim(), state.password)
        }

        if (result is Outcome.Failure) {
            _uiState.update { it.copy(isSubmitting = false, hasError = true) }
        } else if (state.isRegisterMode) {
            // Registration must not create a session. The user should explicitly log in.
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    isRegisterMode = false,
                    password = "",
                    hasError = false,
                    registrationSucceeded = true,
                )
            }
        } else {
            _uiState.update { it.copy(isSubmitting = false, hasError = false) }
        }
    }
}
