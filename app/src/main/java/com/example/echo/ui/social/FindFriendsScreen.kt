package com.example.echo.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.ShimmerSongList
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.ui.navigation.Destination
import com.example.echo.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendsScreen(
    navController: NavController,
    viewModel: FindFriendsViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val followOverrides by viewModel.followOverrides.collectAsStateWithLifecycle()
    val users = viewModel.users.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.find_friends)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text(stringResource(R.string.find_friends)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EchoTheme.spacing.screen),
            )
            Spacer(Modifier.height(EchoTheme.spacing.sm))

            if (users.loadState.refresh is LoadState.Loading) {
                ShimmerSongList()
                return@Column
            }

            LazyColumn {
                items(users.itemCount, key = users.itemKey { it.id }) { index ->
                    val user = users[index] ?: return@items
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
                        //TODO
                        trailingContent = {
                            val following = followOverrides[user.id] ?: user.isFollowing
                            if (following) {
                                OutlinedButton(onClick = { viewModel.toggleFollow(user) }) {
                                    Text(stringResource(R.string.unfollow))
                                }
                            } else {
                                Button(onClick = { viewModel.toggleFollow(user) }) {
                                    Text(stringResource(R.string.follow))
                                }
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
}
