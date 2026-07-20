package com.example.echo.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.echo.R
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.core.designsystem.theme.ThemeMode

/**
 * Language, theme and font size. Every one of these writes to DataStore, and the whole app
 * recomposes from the top — including the layout direction, which flips on the Persian tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: AppStateViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = EchoTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.lg),
        ) {
            SettingsSection(stringResource(R.string.settings_language)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("fa" to R.string.settings_language_fa, "en" to R.string.settings_language_en)
                        .forEachIndexed { index, (tag, label) ->
                            SegmentedButton(
                                selected = settings.language == tag,
                                onClick = { viewModel.setLanguage(tag) },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                            ) { Text(stringResource(label)) }
                        }
                }
            }

            SettingsSection(stringResource(R.string.settings_theme)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(
                        ThemeMode.LIGHT to R.string.settings_theme_light,
                        ThemeMode.DARK to R.string.settings_theme_dark,
                        ThemeMode.SYSTEM to R.string.settings_theme_system,
                    ).forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, 3),
                        ) { Text(stringResource(label)) }
                    }
                }
            }

            SettingsSection(stringResource(R.string.settings_font_size)) {
                Column {
                    Slider(
                        value = settings.fontScale,
                        onValueChange = { viewModel.setFontScale(it) },
                        valueRange = 0.85f..1.3f,
                        steps = 2,
                    )
                    Text(
                        "Aa",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(EchoTheme.spacing.sm))
                Text(stringResource(R.string.settings_logout))
            }
            Spacer(Modifier.height(EchoTheme.spacing.lg))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}
