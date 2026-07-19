package com.example.echo.ui.social

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.EmptyState
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.ui.navigation.Destination
import com.example.echo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    navController: NavController,
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val users by viewModel.users.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_following)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { padding ->
        if (users.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_playlist_title),
                body = stringResource(R.string.empty_playlist_body),
                actionLabel = stringResource(R.string.find_friends),
                onAction = { navController.navigate(Destination.FindFriends.route) },
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(Modifier.padding(padding)) {
            items(users, key = { it.id }) { user ->
                ListItem(
                    headlineContent = { Text(user.displayName) },
                    supportingContent = { Text("@${user.username}") },
                    leadingContent = {
                        AsyncImage(
                            model = user.avatarUrl,
                            placeholder = painterResource(R.drawable.ic_default_avatar),
                            error = painterResource(R.drawable.ic_default_avatar),
                            fallback = painterResource(R.drawable.ic_default_avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(EchoTheme.sizes.avatar)
                                .clip(CircleShape),
                        )
                    },
                    trailingContent = {
                        OutlinedButton(onClick = { viewModel.unfollow(user) }) {
                            Text(stringResource(R.string.unfollow))
                        }
                    },
                    modifier = Modifier.pressScale(pressedScale = 0.99f) {
                        navController.navigate(Destination.UserProfile.of(user.id))
                    },
                )
            }
        }
    }
}
