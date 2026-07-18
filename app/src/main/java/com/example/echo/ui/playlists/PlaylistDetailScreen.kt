package com.example.echo.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.designsystem.component.EmptyState
import com.example.echo.core.designsystem.component.ShimmerSongList
import com.example.echo.core.designsystem.component.SongRow
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.Playlist
import com.example.echo.domain.repository.MusicRepository
import com.example.echo.player.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.echo.R

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {
    private val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

    private val _state = MutableStateFlow<Playlist?>(null)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            (musicRepository.playlist(playlistId) as? Outcome.Success)?.let { _state.value = it.data }
        }
    }

    fun playAll() = _state.value?.songs?.let { playerController.play(it) }
    fun shuffleAll() = _state.value?.songs?.let { playerController.shufflePlay(it) }
    fun play(index: Int) = _state.value?.songs?.let { playerController.play(it, index) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val playlist by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { padding ->
        val current = playlist
        if (current == null) {
            ShimmerSongList(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        if (current.songs.isEmpty()) {
            EmptyState(
                stringResource(R.string.empty_playlist_title),
                stringResource(R.string.empty_playlist_body),
                Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = EchoTheme.sizes.miniPlayerHeight + EchoTheme.spacing.xl),
        ) {
            item {
                Box(Modifier.fillMaxWidth().height(220.dp)) {
                    AsyncImage(
                        model = current.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.3f to Color.Transparent,
                                    1f to MaterialTheme.colorScheme.background,
                                )
                            )
                    )
                    Row(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(EchoTheme.spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = viewModel::playAll) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(EchoTheme.spacing.xs))
                            Text(stringResource(R.string.play_all))
                        }
                        OutlinedButton(onClick = viewModel::shuffleAll) {
                            Icon(Icons.Filled.Shuffle, null)
                            Spacer(Modifier.width(EchoTheme.spacing.xs))
                            Text(stringResource(R.string.shuffle_all))
                        }
                    }
                }
            }

            itemsIndexed(current.songs, key = { _, song -> song.id }) { index, song ->
                SongRow(song = song, onClick = { viewModel.play(index) })
            }
        }
    }
}
