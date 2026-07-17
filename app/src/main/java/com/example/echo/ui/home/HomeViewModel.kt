package com.example.echo.ui.home

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.*
import com.example.echo.domain.repository.AuthRepository
import com.example.echo.domain.repository.MusicRepository
import com.example.echo.player.PlayerController
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Every piece of Home's state, in one class, exposed as one StateFlow. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val avatarUrl: String? = null,
    val carousel: List<Song> = emptyList(),
    val popular: List<Song> = emptyList(),
    val newest: List<Song> = emptyList(),
    val worldPlaylists: List<Playlist> = emptyList(),
    val localPlaylists: List<Playlist> = emptyList(),
    val hasError: Boolean = false,
)

data class Action(val labelRes: Int, val icon: ImageVector, val route: String)

sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data class PlaySong(val song: Song, val queue: List<Song>) : HomeEvent
}

sealed interface HomeEffect {
    data object ShowOfflineSnackbar : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** One-off events. A Channel, not state — a snackbar must not replay on rotation. */
    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(avatarUrl = user?.avatarUrl) }
            }
        }
        load()
    }

    fun onEvent(event: HomeEvent) = when (event) {
        HomeEvent.Refresh -> load()
        is HomeEvent.PlaySong -> {
            val index = event.queue.indexOf(event.song).coerceAtLeast(0)
            playerController.play(event.queue, index)
            Unit
        }
    }

    private fun load() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, hasError = false) }

        // Six calls, one round trip's worth of latency.
        val carousel = async { musicRepository.carousel() }
        val popularPage = async { musicRepository.recentlyPlayed() }
        val world = async { musicRepository.playlists(PlaylistKind.WORLD) }
        val local = async { musicRepository.playlists(PlaylistKind.LOCAL) }
        musicRepository.syncLikes()

        val carouselResult = carousel.await()
        val worldResult = world.await()
        val localResult = local.await()
        popularPage.await()

        if (carouselResult is Outcome.Failure) {
            _uiState.update { it.copy(isLoading = false, hasError = true) }
            _effects.send(HomeEffect.ShowOfflineSnackbar)
            return@launch
        }

        val songs = (carouselResult as Outcome.Success).data
        _uiState.update {
            it.copy(
                isLoading = false,
                carousel = songs.take(6),
                popular = songs.sortedByDescending { s -> s.durationMs },
                newest = songs,
                worldPlaylists = (worldResult as? Outcome.Success)?.data.orEmpty(),
                localPlaylists = (localResult as? Outcome.Success)?.data.orEmpty(),
            )
        }
    }
}
