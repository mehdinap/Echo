package com.example.echo.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TopAppBarDefaults.windowInsets
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.R

/**
 * Shared home bar: the brand stays centered while profile and notifications occupy both sides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoTopBar(
    avatarUrl: String?,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier.padding(horizontal = 8.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
            ) {
                Icon(
                    painterResource(R.drawable.ic_echo_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(EchoTheme.sizes.iconLarge),
                )
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
        navigationIcon = {
            IconButton(onClick = onProfileClick) {
                AsyncImage(
                    model = avatarUrl,
                    placeholder = painterResource(R.drawable.ic_default_avatar),
                    error = painterResource(R.drawable.ic_default_avatar),
                    fallback = painterResource(R.drawable.ic_default_avatar),
                    contentDescription = stringResource(R.string.cd_profile_picture),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(EchoTheme.sizes.icon + EchoTheme.spacing.sm)
                        .clip(CircleShape),
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                Icon(Icons.Filled.Notifications, stringResource(R.string.cd_notifications))
            }
            Spacer(Modifier.width(EchoTheme.spacing.xs))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}
