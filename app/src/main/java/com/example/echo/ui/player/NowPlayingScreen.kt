package com.example.echo.ui.player

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.core.designsystem.theme.PremiumGold
import kotlin.math.roundToLong
import com.example.echo.R

/**
 * The player screen. Three ideas carry it:
 *   1. the cover is a spinning disc, and it stops the instant the music does;
 *   2. the background is a gradient sampled from that cover, so every song looks different;
 *   3. a Canvas waveform breathes underneath the controls.
 *
 * The disc is the shared element that grows out of the mini player.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.NowPlayingScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    onCollapse: () -> Unit,
    onShare: (Long) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()
    val song = playback.currentSong ?: return

    val snackbarHostState = remember { SnackbarHostState() }
    val premiumMessage = stringResource(R.string.premium_required)
    val downloadMessage = stringResource(R.string.download_started)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlayerEffect.PremiumRequired -> snackbarHostState.showSnackbar(premiumMessage)
                PlayerEffect.DownloadStarted -> snackbarHostState.showSnackbar(downloadMessage)
                is PlayerEffect.SleepTimerSet -> Unit
            }
        }
    }

    val accent = rememberDominantColor(song.coverImageUrl, MaterialTheme.colorScheme.primary)
    val animatedAccent by animateColorAsState(accent, tween(700), label = "accent")
    val surface = MaterialTheme.colorScheme.background

    var showSleepSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(
                    animatedAccent.copy(alpha = 0.55f),
                    animatedAccent.copy(alpha = 0.18f),
                    surface,
                )
            )
        ),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = EchoTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.cd_collapse))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSleepSheet = true }) {
                    Icon(
                        Icons.Filled.Bedtime,
                        stringResource(R.string.player_sleep_timer),
                        tint = if (playback.sleepTimerRemainingMs > 0) animatedAccent
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showSpeedSheet = true }) {
                    Icon(Icons.Filled.Speed, stringResource(R.string.player_speed))
                }
            }

            Spacer(Modifier.height(EchoTheme.spacing.lg))

            SpinningDisc(
                coverUrl = song.coverImageUrl,
                isPlaying = playback.isPlaying,
                accent = animatedAccent,
                modifier = Modifier
                    .sharedElement(
                        rememberSharedContentState(key = "cover-${song.id}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .size(280.dp),
            )

            Spacer(Modifier.height(EchoTheme.spacing.lg))

            Text(
                song.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(EchoTheme.spacing.md))

            AudioVisualizer(
                isPlaying = playback.isPlaying,
                color = animatedAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            )

            Spacer(Modifier.height(EchoTheme.spacing.sm))

            SeekBar(
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                accent = animatedAccent,
                onSeek = { viewModel.onEvent(PlayerEvent.SeekTo(it)) },
            )

            Spacer(Modifier.height(EchoTheme.spacing.sm))

            TransportControls(
                isPlaying = playback.isPlaying,
                shuffle = playback.shuffle,
                repeatMode = playback.repeatMode,
                accent = animatedAccent,
                onEvent = viewModel::onEvent,
            )

            Spacer(Modifier.height(EchoTheme.spacing.md))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                val liked = song.id in likedIds
                IconButton(onClick = { viewModel.onEvent(PlayerEvent.ToggleLike(song)) }) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        stringResource(R.string.cd_like),
                        tint = if (liked) animatedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { viewModel.onEvent(PlayerEvent.Download(song)) }) {
                    Icon(
                        if (song.isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        stringResource(R.string.cd_download),
                        tint = if (song.isDownloaded) PremiumGold
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onShare(song.id) }) {
                    Icon(Icons.Filled.Share, stringResource(R.string.cd_share))
                }
            }
            Spacer(Modifier.height(EchoTheme.spacing.lg))
        }
    }

    if (showSleepSheet) {
        SleepTimerSheet(
            remainingMs = playback.sleepTimerRemainingMs,
            onPick = { minutes ->
                viewModel.onEvent(PlayerEvent.SetSleepTimer(minutes))
                showSleepSheet = false
            },
            onDismiss = { showSleepSheet = false },
        )
    }
    if (showSpeedSheet) {
        SpeedSheet(
            current = playback.speed,
            onPick = { speed ->
                viewModel.onEvent(PlayerEvent.SetSpeed(speed))
                showSpeedSheet = false
            },
            onDismiss = { showSpeedSheet = false },
        )
    }
}

/** A vinyl record: cover art in a black ring, spinning at 33⅓-ish rpm while it plays. */
@Composable
private fun SpinningDisc(
    coverUrl: String,
    isPlaying: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    // Angle is kept in state so pausing freezes the disc where it is, and resuming picks it up.
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withFrameNanos { }
            angle = (angle + 0.25f) % 360f
        }
    }
    val smoothAngle by animateFloatAsState(angle, tween(60, easing = LinearEasing), label = "disc")

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )
        AsyncImage(
            model = coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize(0.88f)
                .rotate(smoothAngle)
                .clip(CircleShape),
        )
        // Spindle hole.
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
) {
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    val fraction = when {
        scrubbing != null -> scrubbing!!
        durationMs > 0 -> positionMs.toFloat() / durationMs
        else -> 0f
    }

    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = fraction.coerceIn(0f, 1f),
            onValueChange = { scrubbing = it },
            onValueChangeFinished = {
                scrubbing?.let { onSeek((it * durationMs).roundToLong()) }
                scrubbing = null
            },
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs), style = MaterialTheme.typography.labelSmall)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    accent: Color,
    onEvent: (PlayerEvent) -> Unit,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val transportDirection = if (isRtl) -1f else 1f

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onEvent(PlayerEvent.ToggleShuffle) }) {
            Icon(
                Icons.Filled.Shuffle,
                stringResource(R.string.cd_shuffle),
                tint = if (shuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onEvent(PlayerEvent.Previous) }) {
            Icon(Icons.Filled.SkipPrevious, stringResource(R.string.cd_previous),
                modifier = Modifier
                    .size(EchoTheme.sizes.iconLarge)
                    .scale(scaleX = transportDirection, scaleY = 1f))
        }
        Box(
            Modifier
                .size(EchoTheme.sizes.playButton)
                .clip(CircleShape)
                .background(accent)
                .pressScale { onEvent(PlayerEvent.TogglePlayPause) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(EchoTheme.sizes.iconLarge),
            )
        }
        IconButton(onClick = { onEvent(PlayerEvent.Next) }) {
            Icon(Icons.Filled.SkipNext, stringResource(R.string.cd_next),
                modifier = Modifier
                    .size(EchoTheme.sizes.iconLarge)
                    .scale(scaleX = transportDirection, scaleY = 1f))
        }
        IconButton(onClick = { onEvent(PlayerEvent.CycleRepeat) }) {
            Icon(
                if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE)
                    Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                stringResource(R.string.cd_repeat),
                tint = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) accent
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(remainingMs: Long, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(EchoTheme.spacing.md)) {
            Text(stringResource(R.string.player_sleep_timer), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(EchoTheme.spacing.sm))
            if (remainingMs > 0) {
                Text(
                    formatDuration(remainingMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(EchoTheme.spacing.sm))
            }
            listOf(0, 15, 30, 45, 60).forEach { minutes ->
                ListItem(
                    headlineContent = {
                        Text(
                            if (minutes == 0) stringResource(R.string.player_sleep_off)
                            else stringResource(R.string.player_sleep_minutes, minutes)
                        )
                    },
                    modifier = Modifier.pressScale { onPick(minutes) },
                )
            }
            Spacer(Modifier.height(EchoTheme.spacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(current: Float, onPick: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(EchoTheme.spacing.md)) {
            Text(stringResource(R.string.player_speed), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(EchoTheme.spacing.sm))
            listOf(0.5f, 1f, 1.5f, 2f).forEach { speed ->
                ListItem(
                    headlineContent = { Text("${speed}x") },
                    trailingContent = {
                        if (speed == current) Icon(Icons.Filled.Check, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.pressScale { onPick(speed) },
                )
            }
            Spacer(Modifier.height(EchoTheme.spacing.lg))
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
