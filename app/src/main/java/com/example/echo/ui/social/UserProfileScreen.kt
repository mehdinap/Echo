package com.example.echo.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.ShimmerSongList
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.core.designsystem.theme.PremiumGold
import com.example.echo.ui.navigation.Destination
import com.example.echo.R

/** Someone else's profile: follow them, open a DM, browse their public playlists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    viewModel: UserProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.openChat.collect { conversationId ->
            user?.let { navController.navigate(Destination.Chat.of(conversationId, it.id)) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.displayName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        }
    ) { padding ->
        val current = user
        if (current == null) {
            ShimmerSongList(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(EchoTheme.spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = current.avatarUrl,
                placeholder = painterResource(R.drawable.ic_default_avatar),
                error = painterResource(R.drawable.ic_default_avatar),
                fallback = painterResource(R.drawable.ic_default_avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(EchoTheme.sizes.avatarLarge)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(EchoTheme.spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(current.displayName, style = MaterialTheme.typography.headlineSmall)
                if (current.isPremium) {
                    Spacer(Modifier.width(EchoTheme.spacing.xs))
                    Icon(
                        Icons.Filled.WorkspacePremium,
                        stringResource(R.string.profile_premium),
                        tint = PremiumGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.followers_count, current.followers),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(EchoTheme.spacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm)) {
                if (current.isFollowing) {
                    OutlinedButton(onClick = viewModel::toggleFollow) {
                        Text(stringResource(R.string.unfollow))
                    }
                } else {
                    Button(onClick = viewModel::toggleFollow) {
                        Text(stringResource(R.string.follow))
                    }
                }
                OutlinedButton(onClick = viewModel::message) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null)
                    Spacer(Modifier.width(EchoTheme.spacing.xs))
                    Text(stringResource(R.string.chat_title))
                }
            }

            Spacer(Modifier.height(EchoTheme.spacing.lg))

            if (current.publicPlaylists.isNotEmpty()) {
                Text(
                    stringResource(R.string.public_playlists),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(EchoTheme.spacing.sm))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.gutter)) {
                    items(current.publicPlaylists, key = { it.id }) { playlist ->
                        Column(
                            Modifier
                                .width(EchoTheme.sizes.artCard)
                                .pressScale {
                                    navController.navigate(Destination.PlaylistDetail.of(playlist.id))
                                }
                        ) {
                            AsyncImage(
                                model = playlist.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(EchoTheme.sizes.artCard)
                                    .clip(MaterialTheme.shapes.medium),
                            )
                            Spacer(Modifier.height(EchoTheme.spacing.xs))
                            Text(
                                playlist.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
