package com.example.echo.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.echo.core.designsystem.component.EmptyState
import com.example.echo.core.designsystem.component.ShimmerSongList
import com.example.echo.core.designsystem.component.SongRow
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.domain.model.SearchFilter
import com.example.echo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(Modifier.fillMaxSize().padding(top = EchoTheme.spacing.sm)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.onEvent(SearchEvent.QueryChanged(it)) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onEvent(SearchEvent.QueryChanged("")) }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.cd_remove))
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    viewModel.onEvent(SearchEvent.Submitted(state.query))
                    keyboard?.hide()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EchoTheme.spacing.screen),
        )

        Spacer(Modifier.height(EchoTheme.spacing.sm))

        LazyRow(
            contentPadding = PaddingValues(horizontal = EchoTheme.spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
        ) {
            items(SearchFilter.entries.toList()) { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { viewModel.onEvent(SearchEvent.FilterChanged(filter)) },
                    label = {
                        Text(
                            stringResource(
                                when (filter) {
                                    SearchFilter.ALL -> R.string.filter_all
                                    SearchFilter.SONG -> R.string.filter_songs
                                    SearchFilter.ARTIST -> R.string.filter_artists
                                }
                            )
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(EchoTheme.spacing.sm))

        when {
            state.query.length < 2 -> SearchHistory(
                history = state.history.map { it.query },
                onPick = { viewModel.onEvent(SearchEvent.HistoryClicked(it)) },
                onRemove = { viewModel.onEvent(SearchEvent.HistoryRemoved(it)) },
                onClear = { viewModel.onEvent(SearchEvent.HistoryCleared) },
            )

            results.loadState.refresh is LoadState.Loading -> ShimmerSongList()

            results.itemCount == 0 -> EmptyState(
                title = stringResource(R.string.empty_search_title),
                body = stringResource(R.string.empty_search_body),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = EchoTheme.sizes.miniPlayerHeight + EchoTheme.spacing.xl)
            ) {
                items(results.itemCount, key = results.itemKey { it.id }) { index ->
                    results[index]?.let { song ->
                        SongRow(song = song, onClick = { viewModel.onEvent(SearchEvent.PlaySong(song)) })
                    }
                }
                if (results.loadState.append is LoadState.Loading) {
                    item { ShimmerSongList(count = 2) }
                }
            }
        }
    }
}

@Composable
private fun SearchHistory(
    history: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (history.isEmpty()) return
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = EchoTheme.spacing.screen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) { Text(stringResource(R.string.search_clear_history)) }
        }
        LazyColumn {
            items(history) { query ->
                ListItem(
                    headlineContent = { Text(query) },
                    leadingContent = { Icon(Icons.Filled.History, null) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(query) }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.cd_remove))
                        }
                    },
                    modifier = Modifier.clickable { onPick(query) },
                )
            }
        }
    }
}
