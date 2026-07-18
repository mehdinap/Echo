package com.example.echo.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.ShimmerBox
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.domain.model.Playlist
import com.example.echo.ui.home.SectionHeader
import com.example.echo.ui.navigation.Destination
import com.example.echo.R

/** Two columns of colour-washed cards, grouped world / local / yours. */
@Composable
fun PlaylistsScreen(
    navController: NavController,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val worldTitle = stringResource(R.string.playlists_world)
    val localTitle = stringResource(R.string.playlists_local)
    val mineTitle = stringResource(R.string.playlists_mine)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = EchoTheme.spacing.screen,
            end = EchoTheme.spacing.screen,
            bottom = EchoTheme.sizes.miniPlayerHeight + EchoTheme.spacing.xl,
        ),
        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter),
    ) {
        section(worldTitle, state.world, state.isLoading, navController)
        section(localTitle, state.local, state.isLoading, navController)
        section(mineTitle, state.mine, state.isLoading, navController)
    }
}
private fun LazyGridScope.section(
    title: String,
    playlists: List<Playlist>,
    isLoading: Boolean,
    navController: NavController,
) {
    item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(title, Modifier) }

    if (isLoading) {
        items(4) {
            ShimmerBox(Modifier.fillMaxWidth().aspectRatio(1f), MaterialTheme.shapes.medium)
        }
    } else {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(playlist) { navController.navigate(Destination.PlaylistDetail.of(playlist.id)) }
        }
    }
}

@Composable
private fun PlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .pressScale(onClick = onClick),
    ) {
        AsyncImage(
            model = playlist.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.8f),
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(EchoTheme.spacing.sm)
        ) {
            Text(
                playlist.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.playlist_song_count, playlist.songCount),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}
