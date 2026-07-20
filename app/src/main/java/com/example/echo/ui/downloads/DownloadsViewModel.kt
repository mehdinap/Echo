package com.example.echo.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.domain.model.Song
import com.example.echo.domain.repository.DownloadRepository
import com.example.echo.domain.repository.DownloadSort
import com.example.echo.player.PlayerController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val songs: List<Song> = emptyList(),
    val sort: DownloadSort = DownloadSort.DATE,
    val isLoading: Boolean = true,
)

sealed interface DownloadsEvent {
    data class SortChanged(val sort: DownloadSort) : DownloadsEvent
    data class Play(val song: Song) : DownloadsEvent
    data class Delete(val songId: Long) : DownloadsEvent
    data object PlayAll : DownloadsEvent
    data object ShuffleAll : DownloadsEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val sort = MutableStateFlow(DownloadSort.DATE)

    val uiState: StateFlow<DownloadsUiState> = sort
        .flatMapLatest { s -> downloadRepository.downloads(s).map { songs -> s to songs } }
        .map { (s, songs) -> DownloadsUiState(songs, s, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun onEvent(event: DownloadsEvent) {
        when (event) {
            is DownloadsEvent.SortChanged -> sort.value = event.sort
            is DownloadsEvent.Play -> playerController.play(uiState.value.songs, uiState.value.songs.indexOf(event.song))
            is DownloadsEvent.Delete -> viewModelScope.launch { downloadRepository.deleteDownload(event.songId) }
            DownloadsEvent.PlayAll -> playerController.play(uiState.value.songs)
            DownloadsEvent.ShuffleAll -> playerController.shufflePlay(uiState.value.songs)
        }
    }
}
