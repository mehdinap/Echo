package com.example.echo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.EchoTopBar
import com.example.echo.core.designsystem.component.EmptyState
import com.example.echo.core.designsystem.component.ShimmerBox
import com.example.echo.core.designsystem.component.ShimmerCardRow
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.domain.model.Playlist
import com.example.echo.domain.model.Song
import com.example.echo.ui.navigation.Destination
import kotlinx.coroutines.delay
import com.example.echo.R

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineMessage = stringResource(R.string.error_offline_title)

    LaunchedEffect(Unit) {
        viewModel.effects.collect {
            when (it) {
                HomeEffect.ShowOfflineSnackbar -> snackbarHostState.showSnackbar(offlineMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            EchoTopBar(
                avatarUrl = state.avatarUrl,
                onProfileClick = { navController.navigate(Destination.Profile.route) },
                onNotificationsClick = { navController.navigate(Destination.Conversations.route) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.hasError && state.carousel.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.error_offline_title),
                body = stringResource(R.string.error_offline_body),
                actionLabel = stringResource(R.string.retry),
                onAction = { viewModel.onEvent(HomeEvent.Refresh) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = EchoTheme.sizes.miniPlayerHeight + EchoTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.lg),
        ) {
            item { QuickActions(navController) }

            item {
                if (state.isLoading) {
                    ShimmerBox(
                        Modifier
                            .padding(horizontal = EchoTheme.spacing.screen)
                            .fillMaxWidth()
                            .height(EchoTheme.sizes.carouselHeight),
                        MaterialTheme.shapes.large,
                    )
                } else {
                    DailyPicksCarousel(
                        songs = state.carousel,
                        onSongClick = { viewModel.onEvent(HomeEvent.PlaySong(it, state.carousel)) },
                    )
                }
            }

            item {
                SectionHeader(stringResource(R.string.home_newest))
                if (state.isLoading) ShimmerCardRow()
                else SongRail(state.newest) { viewModel.onEvent(HomeEvent.PlaySong(it, state.newest)) }
            }

            item {
                SectionHeader(stringResource(R.string.home_most_popular))
                if (state.isLoading) ShimmerCardRow()
                else SongRail(state.popular) { viewModel.onEvent(HomeEvent.PlaySong(it, state.popular)) }
            }

            item {
                SectionHeader(stringResource(R.string.home_world_playlists))
                if (state.isLoading) ShimmerCardRow()
                else PlaylistRail(state.worldPlaylists) {
                    navController.navigate(Destination.PlaylistDetail.of(it.id))
                }
            }

            item {
                SectionHeader(stringResource(R.string.home_local_playlists))
                if (state.isLoading) ShimmerCardRow()
                else PlaylistRail(state.localPlaylists) {
                    navController.navigate(Destination.PlaylistDetail.of(it.id))
                }
            }
        }
    }
}

/** Auto-advancing hero carousel. Pauses nothing, loops forever, no dots — the art is the point. */
@Composable
private fun DailyPicksCarousel(songs: List<Song>, onSongClick: (Song) -> Unit) {
    if (songs.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { songs.size })

    LaunchedEffect(songs.size) {
        while (true) {
            delay(4_500)
            val next = (pagerState.currentPage + 1) % songs.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column {
        SectionHeader(stringResource(R.string.home_picks_today))
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = EchoTheme.spacing.screen),
            pageSpacing = EchoTheme.spacing.gutter,
        ) { page ->
            val song = songs[page]
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(EchoTheme.sizes.carouselHeight)
                    .clip(MaterialTheme.shapes.large)
                    .pressScale(pressedScale = 0.98f) { onSongClick(song) },
            ) {
                AsyncImage(
                    model = song.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.75f),
                            )
                        )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(EchoTheme.spacing.md)
                ) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(EchoTheme.spacing.md),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                ) {
                    Text(
                        stringResource(R.string.home_picks_today),
                        modifier = Modifier.padding(horizontal = EchoTheme.spacing.sm, vertical = EchoTheme.spacing.xs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.94f),
                    shadowElevation = 8.dp,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(EchoTheme.spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(navController: NavController) {
    val actions = listOf(
        Action(R.string.home_liked_songs, Icons.Filled.Favorite, Destination.LikedSongs.route),
        Action(R.string.home_recently_played, Icons.Filled.History, Destination.RecentlyPlayed.route),
        Action(R.string.home_my_playlists,
            Icons.AutoMirrored.Filled.QueueMusic, Destination.Playlists.route),
        Action(R.string.home_top_artists, Icons.Filled.Star, Destination.TopArtists.route),
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
    ) {
        items(actions) { action ->
            ActionItem(action, navController)
        }
    }
}

@Composable
private fun ActionItem(action: Action, navController: NavController) {
    Surface(
        modifier = Modifier
            .pressScale { navController.navigate(action.route) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = EchoTheme.spacing.md, vertical = EchoTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    action.icon,
                    null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(EchoTheme.spacing.sm),
                )
            }
            Text(
                stringResource(action.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(
            horizontal = EchoTheme.spacing.screen,
            vertical = EchoTheme.spacing.sm,
        ),
    )
}

@Composable
private fun SongRail(songs: List<Song>, onClick: (Song) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = EchoTheme.spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter),
    ) {
        items(songs, key = { it.id }) { song ->
            Surface(
                modifier = Modifier.width(EchoTheme.sizes.artCard).pressScale { onClick(song) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Column(Modifier.padding(bottom = EchoTheme.spacing.sm)) {
                AsyncImage(
                    model = song.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(EchoTheme.sizes.artCard)
                        .clip(MaterialTheme.shapes.medium),
                )
                Spacer(Modifier.height(EchoTheme.spacing.sm))
                Text(song.title, modifier = Modifier.padding(horizontal = EchoTheme.spacing.sm), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    song.artistName,
                    modifier = Modifier.padding(horizontal = EchoTheme.spacing.sm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRail(playlists: List<Playlist>, onClick: (Playlist) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = EchoTheme.spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Surface(
                modifier = Modifier.width(EchoTheme.sizes.artCard).pressScale { onClick(playlist) },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Column(Modifier.padding(bottom = EchoTheme.spacing.sm)) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(EchoTheme.sizes.artCard)
                        .clip(MaterialTheme.shapes.medium),
                )
                Spacer(Modifier.height(EchoTheme.spacing.sm))
                Text(playlist.title, modifier = Modifier.padding(horizontal = EchoTheme.spacing.sm), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.playlist_song_count, playlist.songCount),
                    modifier = Modifier.padding(horizontal = EchoTheme.spacing.sm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                }
            }
        }
    }
}
