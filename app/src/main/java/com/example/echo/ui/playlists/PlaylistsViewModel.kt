package com.example.echo.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.Playlist
import com.example.echo.domain.model.PlaylistKind
import com.example.echo.domain.repository.MusicRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val isLoading: Boolean = true,
    val world: List<Playlist> = emptyList(),
    val local: List<Playlist> = emptyList(),
    val mine: List<Playlist> = emptyList(),
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val world = async { musicRepository.playlists(PlaylistKind.WORLD) }
        val local = async { musicRepository.playlists(PlaylistKind.LOCAL) }
        val mine = async { musicRepository.myPlaylists() }
        _uiState.update {
            it.copy(
                isLoading = false,
                world = (world.await() as? Outcome.Success)?.data.orEmpty(),
                local = (local.await() as? Outcome.Success)?.data.orEmpty(),
                mine = (mine.await() as? Outcome.Success)?.data.orEmpty(),
            )
        }
    }
}
