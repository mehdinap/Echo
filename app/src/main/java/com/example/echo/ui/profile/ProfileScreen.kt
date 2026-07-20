package com.example.echo.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.core.designsystem.theme.PremiumGold
import com.example.echo.ui.navigation.Destination
import com.example.echo.R

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val upgraded = stringResource(R.string.profile_upgraded)
    val failed = stringResource(R.string.error_generic)
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let(viewModel::changeAvatar) },
    )

    LaunchedEffect(Unit) {
        viewModel.effects.collect {
            snackbarHostState.showSnackbar(if (it is ProfileEffect.Upgraded) upgraded else failed)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(EchoTheme.spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                AsyncImage(
                    model = state.user?.avatarUrl,
                    placeholder = painterResource(R.drawable.ic_default_avatar),
                    error = painterResource(R.drawable.ic_default_avatar),
                    fallback = painterResource(R.drawable.ic_default_avatar),
                    contentDescription = stringResource(R.string.cd_profile_picture),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(EchoTheme.sizes.avatarLarge)
                        .clip(CircleShape),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .pressScale {
                            avatarPicker.launch("image/*")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        stringResource(R.string.profile_change_avatar),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(EchoTheme.spacing.md))
            Text(state.user?.displayName.orEmpty(), style = MaterialTheme.typography.headlineSmall)
            Text(
                "@${state.user?.username.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(EchoTheme.spacing.md))
            PremiumBadge(isPremium = state.user?.isPremium == true)

            Spacer(Modifier.height(EchoTheme.spacing.md))
            Button(
                onClick = viewModel::buyPremium,
                enabled = !state.isPurchasing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isPurchasing) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(EchoTheme.spacing.sm))
                    Text(stringResource(R.string.profile_processing))
                } else {
                    Text(
                        stringResource(
                            if (state.user?.isPremium == true) R.string.profile_renew
                            else R.string.profile_get_premium
                        )
                    )
                }
            }

            Spacer(Modifier.height(EchoTheme.spacing.lg))

            val shortcuts = listOf(
                Triple(Icons.Filled.Favorite, R.string.home_liked_songs, Destination.LikedSongs.route),
                Triple(Icons.Filled.History, R.string.home_recently_played, Destination.RecentlyPlayed.route),
                Triple(Icons.Filled.People, R.string.profile_following, Destination.Following.route),
                Triple(Icons.Filled.PersonSearch, R.string.find_friends, Destination.FindFriends.route),
                Triple(Icons.Filled.Chat, R.string.chat_title, Destination.Conversations.route),
                Triple(Icons.Filled.Settings, R.string.settings_title, Destination.Settings.route),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = EchoTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
            ) {
                itemsIndexed(shortcuts) { index, (icon, label, route) ->
                    ProfileShortcutCard(
                        icon = icon,
                        label = stringResource(label),
                        accent = index % 2 == 0,
                        onClick = { navController.navigate(route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileShortcutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .pressScale(pressedScale = 0.96f, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (accent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        tonalElevation = if (accent) 0.dp else 2.dp,
    ) {
        Column(
            Modifier.padding(EchoTheme.spacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (accent) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(EchoTheme.spacing.sm),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (accent) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}


/** Gold, and gold only here. It has to mean something. */
@Composable
private fun PremiumBadge(isPremium: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isPremium) PremiumGold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.padding(horizontal = EchoTheme.spacing.md, vertical = EchoTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.xs),
        ) {
            Icon(
                if (isPremium) Icons.Filled.WorkspacePremium else Icons.Filled.Person,
                null,
                tint = if (isPremium) PremiumGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(if (isPremium) R.string.profile_premium else R.string.profile_free),
                style = MaterialTheme.typography.labelLarge,
                color = if (isPremium) PremiumGold else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
