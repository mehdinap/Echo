package com.example.echo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.echo.domain.model.Song
import com.example.echo.domain.repository.DownloadRepository
import com.example.echo.domain.repository.MusicRepository
import com.example.echo.player.PlaybackState
import com.example.echo.player.PlayerController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot things the player screen needs to say. Never part of UiState. */
sealed interface PlayerEffect {
    data object PremiumRequired : PlayerEffect
    data object DownloadStarted : PlayerEffect
    data class SleepTimerSet(val minutes: Int) : PlayerEffect
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val musicRepository: MusicRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val playback: StateFlow<PlaybackState> = playerController.state

    val likedIds: StateFlow<Set<Long>> = musicRepository.likedSongs
        .map { songs -> songs.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _effects = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerEffect> = _effects.receiveAsFlow()

    fun onEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.TogglePlayPause -> playerController.togglePlayPause()
            PlayerEvent.Next -> playerController.next()
            PlayerEvent.Previous -> playerController.previous()
            is PlayerEvent.SeekTo -> playerController.seekTo(event.positionMs)
            is PlayerEvent.SetSpeed -> playerController.setSpeed(event.speed)
            PlayerEvent.ToggleShuffle -> playerController.setShuffle(!playback.value.shuffle)
            PlayerEvent.CycleRepeat -> playerController.cycleRepeat()
            is PlayerEvent.SetSleepTimer -> {
                if (event.minutes <= 0) playerController.cancelSleepTimer()
                else {
                    playerController.startSleepTimer(event.minutes)
                    viewModelScope.launch { _effects.send(PlayerEffect.SleepTimerSet(event.minutes)) }
                }
            }
            is PlayerEvent.ToggleLike -> viewModelScope.launch {
                val liked = event.song.id in likedIds.value
                musicRepository.toggleLike(event.song, !liked)
            }
            is PlayerEvent.Download -> viewModelScope.launch {
                val started = downloadRepository.enqueueDownload(event.song)
                _effects.send(if (started) PlayerEffect.DownloadStarted else PlayerEffect.PremiumRequired)
            }
        }
    }
}

/** Every user interaction with the player arrives as one of these. */
sealed interface PlayerEvent {
    data object TogglePlayPause : PlayerEvent
    data object Next : PlayerEvent
    data object Previous : PlayerEvent
    data object ToggleShuffle : PlayerEvent
    data object CycleRepeat : PlayerEvent
    data class SeekTo(val positionMs: Long) : PlayerEvent
    data class SetSpeed(val speed: Float) : PlayerEvent
    data class SetSleepTimer(val minutes: Int) : PlayerEvent
    data class ToggleLike(val song: Song) : PlayerEvent
    data class Download(val song: Song) : PlayerEvent
}
