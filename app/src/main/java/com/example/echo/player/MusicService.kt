package com.example.echo.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import com.example.echo.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Playback lives in a foreground MediaSessionService: leaving the app, locking the screen,
 * or swiping the task away does not stop the music. Media3 renders the notification and the
 * lock-screen controls from the session, so Play/Pause/Next come for free.
 *
 * Audio focus is delegated to ExoPlayer (`setAudioAttributes(..., handleAudioFocus = true)`):
 * an incoming call pauses us, a Telegram voice note ducks us, and playback resumes after.
 */
@AndroidEntryPoint
@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    @Inject lateinit var okHttpClient: OkHttpClient

    private val serviceScope = CoroutineScope(SupervisorJob())
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private lateinit var crossfade: CrossfadeController

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(
                    PlaybackCache.factory(this, okHttpClient)
                )
            )
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)   // unplugging headphones pauses, never blasts
            .build()

        crossfade = CrossfadeController(serviceScope).also { it.attach(player) }

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Keep playing when the task is swiped away, unless playback is already paused. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        crossfade.detach()
        serviceScope.cancel()
        session?.run {
            player.release()
            release()
            session = null
        }
        super.onDestroy()
    }
}
