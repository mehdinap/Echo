package com.example.echo.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.echo.R
import com.example.echo.core.designsystem.theme.EchoTheme

/**
 * Login and registration are the same form; the extra field slides in rather than
 * pushing the user to a second screen.
 */
@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val registrationMessage = stringResource(R.string.auth_registration_success)

    LaunchedEffect(state.registrationSucceeded) {
        if (state.registrationSucceeded) {
            snackbarHostState.showSnackbar(registrationMessage)
            viewModel.onEvent(AuthEvent.RegistrationMessageShown)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(EchoTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Icon(
                painterResource(R.drawable.ic_echo_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp),
            )
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)

            Spacer(Modifier.height(EchoTheme.spacing.xl))

            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.onEvent(AuthEvent.UsernameChanged(it)) },
                label = { Text(stringResource(R.string.auth_username)) },
                singleLine = true,
                isError = state.hasError,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(visible = state.isRegisterMode) {
                Column {
                    Spacer(Modifier.height(EchoTheme.spacing.sm))
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = { viewModel.onEvent(AuthEvent.DisplayNameChanged(it)) },
                        label = { Text(stringResource(R.string.auth_display_name)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(EchoTheme.spacing.sm))

            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(AuthEvent.PasswordChanged(it)) },
                label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true,
                isError = state.hasError,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.hasError) {
                Spacer(Modifier.height(EchoTheme.spacing.sm))
                Text(
                    stringResource(R.string.auth_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(EchoTheme.spacing.lg))

            Button(
                onClick = { viewModel.onEvent(AuthEvent.Submit) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(EchoTheme.sizes.touchTarget),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        stringResource(
                            if (state.isRegisterMode) R.string.auth_register else R.string.auth_login
                        )
                    )
                }
            }

            TextButton(onClick = { viewModel.onEvent(AuthEvent.ToggleMode) }) {
                Text(
                    stringResource(
                        if (state.isRegisterMode) R.string.auth_switch_to_login
                        else R.string.auth_switch_to_register
                    )
                )
            }
            }
        }
    }
}
