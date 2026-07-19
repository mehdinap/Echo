package com.example.echo.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.echo.core.designsystem.component.pressScale
import com.example.echo.core.designsystem.theme.EchoTheme
import com.example.echo.domain.model.Message
import com.example.echo.domain.model.MessageStatus
import com.example.echo.R

/**
 * Direct messages over WebSocket. Nothing here polls.
 *
 * The list is `reverseLayout` so index 0 is the newest message and a new arrival lands at the
 * bottom without any scroll gymnastics — which matches the Paging source's DESC ordering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.chat_title))
                        AnimatedVisibility(visible = state.peerIsTyping) {
                            Text(
                                stringResource(R.string.chat_typing),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        bottomBar = {
            MessageComposer(
                draft = state.draft,
                onDraftChanged = { viewModel.onEvent(ChatEvent.DraftChanged(it)) },
                onSend = { viewModel.onEvent(ChatEvent.Send) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = EchoTheme.spacing.sm),
            state = listState,
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.xs),
        ) {
            items(messages.itemCount, key = messages.itemKey { it.senderId to it.id }) { index ->
                messages[index]?.let { message ->
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == state.currentUserId,
                        onPlaySong = { viewModel.onEvent(ChatEvent.PlaySong(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    onPlaySong: (Long) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isMine) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(Modifier.padding(EchoTheme.spacing.sm)) {
                message.song?.let { song ->
                    // Shared track: a compact card that plays in place.
                    Row(
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surface)
                            .pressScale { onPlaySong(song.id) }
                            .padding(EchoTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                    ) {
                        AsyncImage(
                            model = song.coverImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                        )
                        Column(Modifier.weight(1f, fill = false)) {
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
                            )
                        }
                        Icon(
                            Icons.Filled.PlayCircle,
                            stringResource(R.string.cd_play),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                message.body?.takeIf { it.isNotBlank() }?.let { body ->
                    if (message.song != null) Spacer(Modifier.height(EchoTheme.spacing.xs))
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.xxs),
                ) {
                    Text(
                        timeOf(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isMine) ReadReceipt(message.status)
                }
            }
        }
    }
}

/** Clock while unsent, one tick on delivery to the server, two green ticks once read. */
@Composable
private fun ReadReceipt(status: MessageStatus) {
    when (status) {
        MessageStatus.SENDING -> Icon(
            Icons.Filled.Schedule, null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MessageStatus.SENT -> Icon(
            Icons.Filled.Check, null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MessageStatus.DELIVERED -> Icon(
            Icons.Filled.DoneAll, null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MessageStatus.READ -> Icon(
            Icons.Filled.DoneAll, null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MessageComposer(
    draft: String,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(EchoTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                placeholder = { Text(stringResource(R.string.chat_hint)) },
                shape = MaterialTheme.shapes.extraLarge,
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.chat_send))
            }
        }
    }
}

private fun timeOf(epochMs: Long): String {
    val date = java.util.Date(epochMs)
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
}
