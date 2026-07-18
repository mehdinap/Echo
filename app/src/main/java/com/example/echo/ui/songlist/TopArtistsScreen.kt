package com.example.echo.ui.songlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.ShimmerSongList
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsScreen(
    navController: NavController,
    viewModel: TopArtistsViewModel = hiltViewModel(),
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_top_artists)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        }
    ) { padding ->
        if (artists.isEmpty()) {
            ShimmerSongList(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding)) {
            items(artists, key = { it.id }) { artist ->
                ListItem(
                    headlineContent = { Text(artist.name) },
                    supportingContent = {
                        Text(stringResource(R.string.followers_count, artist.followers))
                    },
                    leadingContent = {
                        AsyncImage(
                            model = artist.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(EchoTheme.sizes.avatar)
                                .clip(CircleShape),
                        )
                    },
                )
            }
        }
    }
}
