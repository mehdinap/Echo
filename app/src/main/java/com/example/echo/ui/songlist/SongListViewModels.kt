package com.example.echo.ui.songlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.core.util.Outcome
import com.example.echo.domain.model.Artist
import com.example.echo.domain.model.Song
import com.example.echo.domain.repository.MusicRepository
import com.example.echo.player.PlayerController
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikedSongsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    val songs: StateFlow<List<Song>> = musicRepository.likedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { viewModelScope.launch { musicRepository.syncLikes() } }

    fun play(index: Int) = playerController.play(songs.value, index)
    fun playAll() = playerController.play(songs.value)
    fun shuffleAll() = playerController.shufflePlay(songs.value)
    fun unlike(song: Song) = viewModelScope.launch { musicRepository.toggleLike(song, false) }
}

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    init {
        viewModelScope.launch {
            (musicRepository.recentlyPlayed() as? Outcome.Success)?.let { _songs.value = it.data }
        }
    }

    fun play(index: Int) = playerController.play(_songs.value, index)
    fun playAll() = playerController.play(_songs.value)
    fun shuffleAll() = playerController.shufflePlay(_songs.value)
}

@HiltViewModel
class TopArtistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    init {
        viewModelScope.launch {
            (musicRepository.topArtists() as? Outcome.Success)?.let { _artists.value = it.data }
        }
    }
}
