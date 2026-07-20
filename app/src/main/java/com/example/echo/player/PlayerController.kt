package com.example.echo.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.echo.di.ApplicationScope
import com.example.echo.domain.model.Song
import com.example.echo.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers

data class PlaybackState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1f,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val sleepTimerRemainingMs: Long = 0,
)

/**
 * The one object the UI talks to about playback. It owns a MediaController bound to
 * [MusicService] and exposes a single StateFlow — no ViewModel ever touches ExoPlayer.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val musicRepository: MusicRepository,
) {
    private var controller: MediaController? = null
    private val sleepTimer = SleepTimer(scope)

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var queue: List<Song> = emptyList()

    init {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        MediaController.Builder(context, token).buildAsync().apply {
            addListener({
                controller = get()
                controller?.addListener(listener)
                startTicker()
            }, MoreExecutors.directExecutor())
        }
        scope.launch {
            sleepTimer.remainingMs.collect { ms -> _state.value = _state.value.copy(sleepTimerRemainingMs = ms) }
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId?.toLongOrNull() ?: return
            val song = queue.firstOrNull { it.id == id } ?: return
            _state.value = _state.value.copy(currentSong = song)
            scope.launch { musicRepository.markPlayed(song.id) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(durationMs = controller?.duration?.coerceAtLeast(0) ?: 0)
        }
    }

    /** Emits position at 4 Hz — smooth enough for the seek bar, cheap enough to ignore. */
    private fun startTicker() = scope.launch(Dispatchers.Main) {
        while (true) {
            controller?.let { c ->
                _state.value = _state.value.copy(
                    positionMs = c.currentPosition.coerceAtLeast(0),
                    durationMs = c.duration.coerceAtLeast(0),
                    isPlaying = c.isPlaying,
                )
            }
            delay(250)
        }
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        // playbackUri resolves to the downloaded file when one exists — no data is spent twice.
        .setUri(playbackUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistName)
                .setArtworkUri(coverImageUrl.toUri())
                .build()
        )
        .build()

    fun play(songs: List<Song>, startIndex: Int = 0) {
        val c = controller ?: return
        queue = songs
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
        _state.value = _state.value.copy(currentSong = songs.getOrNull(startIndex), queue = songs)
    }

    fun playSingle(song: Song) = play(listOf(song))

    fun shufflePlay(songs: List<Song>) {
        setShuffle(true)
        play(songs.shuffled())
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(ms: Long) = controller?.seekTo(ms)

    fun setSpeed(speed: Float) {
        controller?.playbackParameters = PlaybackParameters(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun setShuffle(on: Boolean) {
        controller?.shuffleModeEnabled = on
        _state.value = _state.value.copy(shuffle = on)
    }

    fun cycleRepeat() {
        val c = controller ?: return
        val next = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        c.repeatMode = next
        _state.value = _state.value.copy(repeatMode = next)
    }

//    fun startSleepTimer(minutes: Int) =
//        sleepTimer.start(minutes * 60_000L) { controller?.pause() }
    fun startSleepTimer(minutes: Int) {
        sleepTimer.start(minutes * 60_000L) {
            scope.launch(Dispatchers.Main) { controller?.pause() }
        }
    }

    fun cancelSleepTimer() = sleepTimer.cancel()
}
