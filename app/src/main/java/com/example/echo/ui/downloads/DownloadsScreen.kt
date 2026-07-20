package com.example.echo.ui.downloads

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.echo.core.designsystem.component.EmptyState
import com.example.echo.core.designsystem.component.SongRow
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.domain.repository.DownloadSort
import com.example.echo.R

/** Swipe a row to the end of the line to remove the file. The red panel appears as you drag. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.downloads_title)) },
                actions = {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.Filled.Sort, stringResource(R.string.downloads_sort_date))
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        listOf(
                            DownloadSort.DATE to R.string.downloads_sort_date,
                            DownloadSort.TITLE to R.string.downloads_sort_title,
                            DownloadSort.ARTIST to R.string.downloads_sort_artist,
                        ).forEach { (sort, label) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                onClick = {
                                    viewModel.onEvent(DownloadsEvent.SortChanged(sort))
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = state.songs.isEmpty() && !state.isLoading,
            modifier = Modifier.padding(padding),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "downloadsContent",
        ) { empty ->
            if (empty) {
                EmptyState(
                    title = stringResource(R.string.empty_downloads_title),
                    body = stringResource(R.string.empty_downloads_body),
                    modifier = Modifier.fillMaxSize(),
                )
                return@AnimatedContent
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = EchoTheme.sizes.miniPlayerHeight + EchoTheme.spacing.xl),
            ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(EchoTheme.spacing.screen),
                    horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                ) {
                    Button(
                        onClick = { viewModel.onEvent(DownloadsEvent.PlayAll) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(EchoTheme.spacing.xs))
                        Text(stringResource(R.string.play_all))
                    }
                    OutlinedButton(
                        onClick = { viewModel.onEvent(DownloadsEvent.ShuffleAll) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Shuffle, null)
                        Spacer(Modifier.width(EchoTheme.spacing.xs))
                        Text(stringResource(R.string.shuffle_all))
                    }
                }
            }

            items(state.songs, key = { it.id }) { song ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.onEvent(DownloadsEvent.Delete(song.id))
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = EchoTheme.spacing.lg),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                stringResource(R.string.cd_remove),
                                tint = MaterialTheme.colorScheme.onError,
                            )
                        }
                    },
                ) {
                    Surface(
                        modifier = Modifier.animateItem(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        SongRow(song = song, onClick = { viewModel.onEvent(DownloadsEvent.Play(song)) })
                    }
                }
            }
            }
        }
    }
}
