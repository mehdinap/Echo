package com.example.echo.ui.songlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.echo.R

@Composable
fun LikedSongsScreen(
    navController: NavController,
    viewModel: LikedSongsViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    SongListScaffold(
        title = stringResource(R.string.home_liked_songs),
        songs = songs,
        navController = navController,
        emptyTitle = stringResource(R.string.empty_likes_title),
        emptyBody = stringResource(R.string.empty_likes_body),
        onPlay = viewModel::play,
        onPlayAll = viewModel::playAll,
        onShuffleAll = viewModel::shuffleAll,
        onRemove = { viewModel.unlike(it) },   // swiping a liked song away un-likes it
    )
}
