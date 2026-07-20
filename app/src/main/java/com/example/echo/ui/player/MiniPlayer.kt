package com.example.echo.ui.player

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.domain.model.Song
import com.example.echo.R

/**
 * Floats above the bottom bar whenever something is loaded. Tapping it expands into the
 * full player; the cover is the shared element that grows from 44dp to 280dp.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.sm)
            .height(EchoTheme.sizes.miniPlayerHeight)
            .pressScale(pressedScale = 0.98f, onClick = onExpand),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 5.dp,
        shadowElevation = 10.dp,
    ) {
        Column {
            Row(
                Modifier
                    .weight(1f)
                    .padding(horizontal = EchoTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter),
            ) {
                AsyncImage(
                    model = song.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sharedElement(
                            rememberSharedContentState(key = "cover-${song.id}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        stringResource(R.string.cd_previous),
                        modifier = Modifier.scale(
                            scaleX = if (isRtl) -1f else 1f,
                            scaleY = 1f,
                        ),
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        stringResource(R.string.cd_next),
                        modifier = Modifier.scale(
                            scaleX = if (isRtl) -1f else 1f,
                            scaleY = 1f,
                        ),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    // LinearProgressIndicator is a visual track, so mirror its fill manually
                    // in RTL while keeping the progress value unchanged.
                    .scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
